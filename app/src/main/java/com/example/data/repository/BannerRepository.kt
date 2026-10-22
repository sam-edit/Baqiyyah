package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * One app-entry check = one [BannerSession]. All four ad slots share its single keep-alive
 * connection (one TLS handshake instead of one per request). [close] cuts everything for good:
 * running calls are cancelled, sockets are evicted and the dispatcher is shut down.
 */
class BannerSession {
    @Volatile var closed = false
        private set
    private val clientLazy = lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(2, 15, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                if (closed) throw IOException("Banner visit ended")
                chain.proceed(chain.request())
            }.build()
    }
    val client: OkHttpClient get() = clientLazy.value

    fun close() {
        closed = true
        if (clientLazy.isInitialized()) {
            try {
                client.dispatcher.cancelAll()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            } catch (_: Exception) { }
        }
    }
}

/**
 * Persistent banner cache + one-shot sync of ONE slot.
 *
 * Order of checks (every step stops the slot immediately when it says "no"):
 *  1. outer switch  (is_banner / is_popup / is_side_loader)  -> ~5 bytes. false = stop, cache cleared.
 *  2. ad/is_ad                                               -> ~5 bytes. false = stop, cache cleared.
 *  3. only when both are true: the slot's remaining values (ad details + popup settings) in ONE
 *     conditional GET (If-None-Match). Unchanged -> 304, nothing downloaded, cache is used.
 *
 * The old cache is deleted only AFTER the new image has been downloaded and validated.
 * No Firebase SDK listeners: plain REST over the shared [BannerSession].
 */
class BannerRepository(
    context: Context,
    private val slot: String,
    /** Name of the on/off switch of this slot: is_banner, is_popup or is_side_loader. */
    private val flagKey: String = "is_banner",
    private val session: BannerSession = BannerSession(),
    /**
     * true  = switch on + is_ad off keeps an empty blank card (all slots, including popup).
     * false = switch on + is_ad off shows nothing.
     */
    private val reserveSlotWithoutAd: Boolean = true
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("banner_store_$slot", Context.MODE_PRIVATE)
    // filesDir (not cacheDir) so Android never wipes the creative when storage is low.
    private val directory = File(appContext.filesDir, "banners/$slot").apply { mkdirs() }
    private val closed get() = session.closed

    private data class Payload(val code: Int, val bytes: ByteArray, val etag: String?)

    private suspend fun fetch(request: Request, limit: Long): Payload = suspendCancellableCoroutine { continuation ->
        val call = session.client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        if (closed || !continuation.isActive) {
            call.cancel()
            continuation.cancel()
        } else {
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val payload = response.use {
                            if (it.code != 304 && !it.isSuccessful) throw IOException("Banner HTTP ${it.code}")
                            val body = it.body
                            val bytes = if (body == null || it.code == 304) byteArrayOf() else {
                                val source = body.source()
                                source.request(limit + 1)
                                if (source.buffer.size > limit) throw IOException("Banner payload too large")
                                source.readByteArray()
                            }
                            Payload(it.code, bytes, it.header("ETag"))
                        }
                        if (continuation.isActive) continuation.resume(payload)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    // ---------------------------------------------------------------- cache

    /** Last verified ad, straight from disk. Only exists if switch AND is_ad were both true. */
    fun cached(): BannerState {
        val json = prefs.getString("ad_json", null) ?: return BannerState()
        return try {
            stateFrom(JSONObject(json), cachedImage())
        } catch (_: Exception) {
            clear()
            BannerState()
        }
    }

    private fun cachedImage(): File? =
        prefs.getString("image_file", null)?.let { File(directory, it) }?.takeIf { it.isFile && it.length() > 0 }

    /** Drop everything cached for this slot (switch turned off, or ad turned off). */
    fun clear() {
        prefs.edit().clear().apply()
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun stateFrom(ad: JSONObject, image: File?, onlyOneTime: Boolean? = null): BannerState {
        fun text(key: String) = (if (ad.isNull(key)) "" else ad.optString(key, "")).trim()
        return BannerState(
            visible = true, adEnabled = true, image = image, aspect = aspectOf(image),
            title = text("title"), description = text("description"), clickUrl = text("click_url"),
            start = timestamp(ad.opt("start_at")), end = timestamp(ad.opt("expires_at")),
            showOn = prefs.getString("show_on", "app_open") ?: "app_open",
            repeatMinutes = prefs.getLong("repeat_minutes", 1440L),
            isOnAppOpen = prefs.getBoolean("is_on_app_open", true),
            isOnlyOneTime = onlyOneTime ?: prefs.getBoolean("is_only_one_time", false)
        )
    }

    /** width / height of the cached creative (1:1 when there is none), limited to a sane range. */
    private fun aspectOf(image: File?): Float {
        if (image == null) return 1f
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(image.path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return 1f
        return (options.outWidth.toFloat() / options.outHeight).coerceIn(MIN_ASPECT, MAX_ASPECT)
    }

    /** Popup settings are cached separately from the creative, so unchanged ads need not download again. */
    private fun saveSettings(node: JSONObject) {
        val showOn = (node.opt("show_on") as? String)?.trim()?.lowercase()?.ifEmpty { null } ?: "app_open"
        val repeatMinutes = when (val v = node.opt("repeat_after_minute")) {
            is Number -> v.toLong()
            is String -> v.trim().toLongOrNull() ?: 1440L
            else -> 1440L
        }.coerceAtLeast(0L)
        val isOnAppOpen = node.opt("is_on_app_open") as? Boolean ?: true
        val isOnlyOneTime = node.opt("is_only_one_time") as? Boolean ?: false
        prefs.edit()
            .putString("show_on", showOn)
            .putLong("repeat_minutes", repeatMinutes)
            .putBoolean("is_on_app_open", isOnAppOpen)
            .putBoolean("is_only_one_time", isOnlyOneTime)
            .apply()
    }

    // ---------------------------------------------------------------- network

    private fun valueRequest(path: String): Request.Builder =
        Request.Builder().url("${DonateRepository.RTDB_URL}/app_config/$slot$path.json")

    /** A single boolean (a few bytes). Anything other than a real `true` counts as false. */
    private suspend fun readFlag(path: String): Boolean {
        val result = fetch(valueRequest(path).build(), 1024)
        return String(result.bytes, Charsets.UTF_8).trim() == "true"
    }

    /**
     * Full check of this slot. Returns the state to show (cache already updated when needed).
     * Throws when the network fails, so the caller keeps whatever is cached.
     */
    suspend fun check(): BannerState {
        // 1) Outer switch. false -> this slot is finished for the whole visit.
        if (!readFlag("/$flagKey")) {
            clear()
            return BannerState()
        }
        // 2) Inner ad switch. false -> leave the ad alone, nothing more is requested.
        if (!readFlag("/ad/is_ad")) {
            clear()
            if (!reserveSlotWithoutAd) return BannerState()
            // The empty card is still shown. Only the popup has timing settings that decide WHEN
            // it appears, so just for it the (tiny) settings are read; no ad data is touched.
            if (flagKey == "is_popup") {
                val result = fetch(valueRequest("").build(), 64 * 1024)
                (JSONTokener(String(result.bytes, Charsets.UTF_8)).nextValue() as? JSONObject)
                    ?.let { saveSettings(it) }
            }
            return BannerState(
                visible = true,
                showOn = prefs.getString("show_on", "app_open") ?: "app_open",
                repeatMinutes = prefs.getLong("repeat_minutes", 1440L),
                isOnAppOpen = prefs.getBoolean("is_on_app_open", true),
                isOnlyOneTime = prefs.getBoolean("is_only_one_time", false)
            )
        }
        // 3) Both true -> the remaining values, with a conditional GET.
        return refreshAd()
    }

    private suspend fun refreshAd(): BannerState {
        val oldJson = prefs.getString("ad_json", null)
        val oldImage = cachedImage()
        val storedImageUrl = prefs.getString("image_url", null).orEmpty()
        val cacheComplete = oldJson != null && (storedImageUrl.isEmpty() || oldImage != null)

        val request = valueRequest("").header("X-Firebase-ETag", "true")
        val etag = prefs.getString("node_etag", null)
        // Conditional GET: server answers 304 (no body) if nothing in the slot changed.
        if (etag != null && cacheComplete && flagKey != "is_popup") request.header("If-None-Match", etag)
        val result = fetch(request.build(), 64 * 1024)
        if (result.code == 304) return cached().takeIf { it.adEnabled } ?: BannerState(visible = true)

        val node = JSONTokener(String(result.bytes, Charsets.UTF_8)).nextValue() as? JSONObject
            ?: throw IOException("Banner node empty")
        val ad = node.optJSONObject("ad") ?: throw IOException("Banner ad missing")
        val isOnlyOneTime = node.opt("is_only_one_time") as? Boolean ?: false
        if (isOnlyOneTime && flagKey == "is_popup") {
            // One-time popup is fetched fresh on every app entry and is never stored as a banner cache.
            clear()
            val rawImage = if (ad.isNull("image_url")) "" else ad.optString("image_url", "").trim()
            val imageUrl = if (rawImage.startsWith("https://")) rawImage else ""
            val image = if (imageUrl.isNotEmpty()) try { downloadImage(imageUrl) } catch (_: Exception) { null } else null
            return stateFrom(ad, image, true)
        }
        saveSettings(node)

        val adText = ad.toString()
        val rawImage = if (ad.isNull("image_url")) "" else ad.optString("image_url", "").trim()
        val imageUrl = if (rawImage.startsWith("https://")) rawImage else ""
        val sameImage = if (imageUrl.isEmpty()) storedImageUrl.isEmpty() && oldJson != null
            else oldImage != null && storedImageUrl == imageUrl

        if (adText == oldJson && sameImage) {
            prefs.edit().putString("node_etag", result.etag).apply()
            return stateFrom(ad, oldImage)
        }

        // Changed. Get the new image first; the old cache stays untouched until it is ready.
        var imageFile: File? = if (sameImage) oldImage else null
        if (!sameImage && imageUrl.isNotEmpty()) {
            imageFile = try { downloadImage(imageUrl) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { null }
            if (imageFile == null) {
                // Download failed: keep the old good creative (retried on the next entry), or show
                // the text only this time without caching it, so the image is retried next entry.
                return if (oldJson != null) cached() else stateFrom(ad, null)
            }
        }
        if (closed) throw IOException("Banner visit ended")

        prefs.edit()
            .putString("ad_json", adText)
            .putString("node_etag", result.etag)
            .putString("image_url", imageUrl)
            .putString("image_file", imageFile?.name)
            .apply()
        // Now the old creative(s) can go.
        directory.listFiles()?.filter { it != imageFile }?.forEach { it.delete() }
        return stateFrom(ad, imageFile)
    }

    private suspend fun downloadImage(url: String): File? {
        val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val file = File(directory, name)
        val result = fetch(Request.Builder().url(url).build(), 12L * 1024 * 1024)
        if (closed) return null
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val temporary = File.createTempFile("banner", ".tmp", directory)
        try {
            temporary.writeBytes(result.bytes)
            if (!closed && temporary.renameTo(file)) return file
        } finally { temporary.delete() }
        return null
    }

    private companion object {
        const val MIN_ASPECT = 0.4f
        const val MAX_ASPECT = 3f
    }

    private fun timestamp(value: Any?): Long? {
        if (value == null || value == org.json.JSONObject.NULL || value == "") return null
        return try {
            val numeric = (value as? Number)?.toLong() ?: (value as? String)?.toLongOrNull()
            if (numeric != null) {
                if (numeric < 100_000_000_000L) Math.multiplyExact(numeric, 1000L) else numeric
            } else Instant.parse(value.toString()).toEpochMilli()
        } catch (_: Exception) { Long.MIN_VALUE }
    }
}

data class BannerState(
    val visible: Boolean = false,
    val adEnabled: Boolean = false,
    val image: File? = null,
    /** width / height of [image]; 1f (1:1) by default or when there is no image. */
    val aspect: Float = 1f,
    val title: String = "",
    val description: String = "",
    val clickUrl: String = "",
    val start: Long? = null,
    val end: Long? = null,
    /** Popup timing mode. If true, repeatMinutes is ignored and each app open is eligible. */
    val showOn: String = "app_open",
    val repeatMinutes: Long = 1440L,
    val isOnAppOpen: Boolean = true,
    val isOnlyOneTime: Boolean = false,
    /** True once this entry's remote check finished (or was skipped: offline / already done). */
    val startupCheckComplete: Boolean = false
) {
    fun inSchedule(now: Long = System.currentTimeMillis()): Boolean =
        adEnabled && start != Long.MIN_VALUE && end != Long.MIN_VALUE &&
            (start == null || now >= start) && (end == null || now < end)
}
