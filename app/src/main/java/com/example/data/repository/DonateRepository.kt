package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.BankConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class DonateRepository(private val context: Context) {

    companion object {
        private const val TAG = "DonateRepository"
        const val RTDB_URL = "https://baqiyyah-byruleno1-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val BANKS_PATH = "app_config/donate_us/banks"
        private const val PREFS_NAME = "donate_us_cache_prefs"
        private const val KEY_CACHED_BANKS = "cached_banks_json"
        private const val KEY_BANKS_ETAG = "cached_banks_etag"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // A new transport for each visit. Never cancel the app-wide HTTP client.
    class Session internal constructor() {
        @Volatile internal var active = true
        val httpClient = OkHttpClient.Builder()
            .dispatcher(Dispatcher())
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                if (!active) throw IOException("Donation screen is closed")
                chain.proceed(chain.request())
            }
            .build()
    }

    private val sessionLock = Any()
    private var currentSession: Session? = null

    fun openSession(): Session = synchronized(sessionLock) {
        currentSession?.let { closeSession(it) }
        Session().also { currentSession = it }
    }

    fun closeSession(session: Session) = synchronized(sessionLock) {
        session.active = false
        try {
            session.httpClient.dispatcher.cancelAll()
            session.httpClient.connectionPool.evictAll()
            session.httpClient.dispatcher.executorService.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Could not close donation transport", e)
        }
        if (currentSession === session) currentSession = null
    }

    /**
     * Reads cached banks from local SharedPreferences.
     * Returns empty list if no cache exists yet.
     */
    fun getCachedBanks(): List<BankConfig> {
        val jsonStr = prefs.getString(KEY_CACHED_BANKS, null) ?: return emptyList()
        return try {
            val list = mutableListOf<BankConfig>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BankConfig(
                        id = obj.optString("id", ""),
                        displayName = obj.optString("displayName", ""),
                        accountHolder = obj.optString("accountHolder", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        iban = obj.optString("iban", ""),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        qrImageUrl = obj.optString("qrImageUrl", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached banks", e)
            emptyList()
        }
    }

    /**
     * Saves the bank list to local SharedPreferences cache.
     */
    fun saveCachedBanks(banks: List<BankConfig>) {
        if (banks.isEmpty()) return
        try {
            val jsonArray = JSONArray()
            for (bank in banks) {
                val obj = JSONObject().apply {
                    put("id", bank.id)
                    put("displayName", bank.displayName)
                    put("accountHolder", bank.accountHolder)
                    put("accountNumber", bank.accountNumber)
                    put("iban", bank.iban)
                    put("isEnabled", bank.isEnabled)
                    put("qrImageUrl", bank.qrImageUrl)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_CACHED_BANKS, jsonArray.toString()).apply()
            Log.d(TAG, "Successfully saved ${banks.size} banks to persistent cache.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving banks to cache", e)
        }
    }

    /**
     * Compares fresh banks with cached banks field-by-field.
     * Matches: account_holder, account_number, iban, is_enabled, and qr_image_url.
     * If all values and URLs match, returns (cached, false).
     * If any value or image URL changed, returns (mergedList, true).
     */
    fun mergeAndCheckChanges(cached: List<BankConfig>, fresh: List<BankConfig>): Pair<List<BankConfig>, Boolean> {
        if (cached.isEmpty()) {
            return Pair(fresh, fresh.isNotEmpty())
        }
        if (fresh.isEmpty()) {
            return Pair(cached, false)
        }
        if (cached.size != fresh.size) {
            return Pair(fresh, true)
        }

        val cachedMap = cached.associateBy { it.id }
        var hasAnyChange = false
        val mergedList = mutableListOf<BankConfig>()

        for (freshBank in fresh) {
            val oldBank = cachedMap[freshBank.id]
            if (oldBank == null) {
                mergedList.add(freshBank)
                hasAnyChange = true
            } else {
                val isValuesIdentical =
                    oldBank.accountHolder.trim() == freshBank.accountHolder.trim() &&
                    oldBank.accountNumber.trim() == freshBank.accountNumber.trim() &&
                    oldBank.iban.trim() == freshBank.iban.trim() &&
                    oldBank.isEnabled == freshBank.isEnabled &&
                    oldBank.qrImageUrl.trim() == freshBank.qrImageUrl.trim()

                if (isValuesIdentical) {
                    mergedList.add(oldBank)
                } else {
                    Log.d(TAG, "Bank ${freshBank.id} updated: values or QR image URL modified.")
                    mergedList.add(freshBank)
                    hasAnyChange = true
                }
            }
        }

        return Pair(mergedList, hasAnyChange)
    }

    /** ETag of the last response, saved only together with the cache (see [commitEtag]). */
    @Volatile private var pendingEtag: String? = null

    /** Writes the ETag of the last 200 response, but only when a bank cache exists to go with it. */
    fun commitEtag() {
        val etag = pendingEtag ?: return
        if (prefs.contains(KEY_CACHED_BANKS)) prefs.edit().putString(KEY_BANKS_ETAG, etag).apply()
    }

    /**
     * One plain REST request (no Firebase SDK, no listener, no socket kept open).
     * Returns null when the server says 304 "unchanged": the cache is already current and
     * nothing was downloaded. Cancellation closes the actual socket, even mid-response.
     */
    suspend fun fetchBanksViaRest(session: Session): List<BankConfig>? =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url("$RTDB_URL/$BANKS_PATH.json")
                .header("X-Firebase-ETag", "true")
            val etag = prefs.getString(KEY_BANKS_ETAG, null)
            // Conditional GET only when there is a cache that the 304 would refer to.
            if (etag != null && prefs.contains(KEY_CACHED_BANKS)) request.header("If-None-Match", etag)
            val call = session.httpClient.newCall(request.build())
            continuation.invokeOnCancellation { call.cancel() }
            synchronized(sessionLock) {
                if (!session.active || currentSession !== session || !continuation.isActive) {
                    call.cancel()
                    continuation.cancel()
                    return@synchronized
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val banks = response.use {
                                if (it.code == 304) return@use null
                                if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
                                val body = it.body?.string() ?: throw IOException("Empty response")
                                pendingEtag = it.header("ETag")
                                val result = mutableListOf<BankConfig>()
                                if (body.trim() != "null") {
                                    val json = JSONObject(body)
                                    val keys = json.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val bank = json.optJSONObject(key) ?: continue
                                        val holder = bank.optString("account_holder", "").trim()
                                        result.add(BankConfig(
                                            id = key,
                                            displayName = formatBankDisplayName(key, holder),
                                            accountHolder = holder,
                                            accountNumber = (bank.opt("account_number")?.toString() ?: "").trim(),
                                            iban = bank.optString("iban", "").trim(),
                                            isEnabled = bank.optBoolean("is_enabled", true),
                                            qrImageUrl = bank.optString("qr_image_url", "").trim()
                                        ))
                                    }
                                }
                                result.toList()
                            }
                            if (continuation.isActive) continuation.resume(banks)
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }
                })
            }
        }

    private fun formatBankDisplayName(key: String, accountHolder: String): String {
        return when {
            accountHolder.contains("Raast", ignoreCase = true) -> "Raast / SadaPay"
            accountHolder.contains("EasyPaisa", ignoreCase = true) -> "Easypaisa"
            accountHolder.contains("JazzCash", ignoreCase = true) -> "JazzCash"
            accountHolder.contains("Meezan", ignoreCase = true) -> "Meezan Bank"
            accountHolder.contains("HBL", ignoreCase = true) -> "Habib Bank Limited (HBL)"
            accountHolder.contains("UBL", ignoreCase = true) -> "United Bank Limited (UBL)"
            accountHolder.contains("MCB", ignoreCase = true) -> "MCB Bank"
            accountHolder.contains("Allied", ignoreCase = true) -> "Allied Bank"
            accountHolder.contains("Alfalah", ignoreCase = true) -> "Bank Alfalah"
            accountHolder.contains("Faysal", ignoreCase = true) -> "Faysal Bank"
            accountHolder.contains("Nayapay", ignoreCase = true) -> "NayaPay"
            accountHolder.contains("Sadapay", ignoreCase = true) -> "SadaPay"
            key.startsWith("bank_", ignoreCase = true) -> {
                val num = key.removePrefix("bank_").replace("_", " ")
                "Bank Account $num"
            }
            else -> key.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
}
