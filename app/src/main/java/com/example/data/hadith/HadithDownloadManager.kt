package com.example.data.hadith

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class HadithLanguage(
    val code: String, // "urd", "eng", "ben", "fra", "ind", "tur", "rus", "tam"
    val englishName: String,
    val nativeName: String
)

sealed class HadithDownloadState {
    object NotDownloaded : HadithDownloadState()
    data class Downloading(val progress: Float) : HadithDownloadState()
    object Downloaded : HadithDownloadState()
    data class Error(val message: String) : HadithDownloadState()
}

class HadithDownloadManager(private val context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val hadithDir = File(context.filesDir, "hadiths").apply { mkdirs() }
    private val prefs: SharedPreferences = context.getSharedPreferences("hadith_prefs", Context.MODE_PRIVATE)

    val supportedLanguages = listOf(
        HadithLanguage("urd", "Urdu", "اردو"),
        HadithLanguage("eng", "English", "English"),
        HadithLanguage("ben", "Bengali", "বাংলা"),
        HadithLanguage("fra", "French", "Français"),
        HadithLanguage("ind", "Indonesian", "Bahasa Indonesia"),
        HadithLanguage("tur", "Turkish", "Türkçe"),
        HadithLanguage("rus", "Russian", "Русский"),
        HadithLanguage("tam", "Tamil", "தமிழ்")
    )

    private val bookLanguageCodes: Map<String, List<String>> = mapOf(
        "sahih-bukhari" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus", "tam"),
        "bukhari" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus", "tam"),
        "sahih-muslim" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus", "tam"),
        "muslim" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus", "tam"),
        "sunan-abi-dawud" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus"),
        "abudawud" to listOf("urd", "eng", "ben", "fra", "ind", "tur", "rus"),
        "sunan-an-nasai" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "nasai" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "jami-at-tirmidhi" to listOf("urd", "eng", "ben", "ind", "tur"),
        "tirmidhi" to listOf("urd", "eng", "ben", "ind", "tur"),
        "sunan-ibn-majah" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "ibnmajah" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "muwatta-malik" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "malik" to listOf("urd", "eng", "ben", "fra", "ind", "tur"),
        "arbaeen-nawawi" to listOf("urd", "eng", "ben", "fra", "tur"),
        "nawawi" to listOf("urd", "eng", "ben", "fra", "tur"),
        "hadith-qudsi" to listOf("urd", "eng", "fra"),
        "qudsi" to listOf("urd", "eng", "fra")
    )

    // User preference: default translation is ON (Urdu translation displayed under Arabic)
    private val _isTranslationEnabled = MutableStateFlow(prefs.getBoolean("is_translation_enabled", true))
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val _selectedLanguageCode = MutableStateFlow(prefs.getString("selected_lang_code", "urd") ?: "urd")
    val selectedLanguageCode: StateFlow<String> = _selectedLanguageCode.asStateFlow()

    private val _translationRevision = MutableStateFlow(0)
    val translationRevision: StateFlow<Int> = _translationRevision.asStateFlow()

    fun notifyTranslationsChanged() {
        _translationRevision.value++
    }

    // Status map for books (key: bookSlug)
    private val _downloadStatusMap = MutableStateFlow<Map<String, HadithDownloadState>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<String, HadithDownloadState>> = _downloadStatusMap.asStateFlow()

    // Status map for language translations (key: "${bookSlug}_${langCode}" or "${langCode}")
    private val _languageDownloadStatusMap = MutableStateFlow<Map<String, HadithDownloadState>>(emptyMap())
    val languageDownloadStatusMap: StateFlow<Map<String, HadithDownloadState>> = _languageDownloadStatusMap.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    init {
        extractBundledAssetsIfNeeded()
        refreshAllStatuses()
    }

    private fun extractBundledAssetsIfNeeded() {
        try {
            val nawawiAra = File(hadithDir, "arbaeen-nawawi_ara.json")
            if (!nawawiAra.exists() || nawawiAra.length() < 1000) {
                try {
                    context.assets.open("hadiths/ara-nawawi.json.gz").use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            nawawiAra.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                } catch (_: Exception) {}
            }

            val nawawiUrd = File(hadithDir, "arbaeen-nawawi_urd.json")
            val nawawiUrdu = File(hadithDir, "arbaeen-nawawi_urdu.json")
            if (!nawawiUrd.exists() || nawawiUrd.length() < 1000) {
                try {
                    context.assets.open("hadiths/urd-nawawi.json.gz").use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            nawawiUrd.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                    if (nawawiUrd.exists() && nawawiUrd.length() > 500) {
                        nawawiUrd.copyTo(nawawiUrdu, overwrite = true)
                    }
                } catch (_: Exception) {}
            } else if (!nawawiUrdu.exists()) {
                try { nawawiUrd.copyTo(nawawiUrdu, overwrite = true) } catch (_: Exception) {}
            }

            val qudsiAra = File(hadithDir, "hadith-qudsi_ara.json")
            if (!qudsiAra.exists() || qudsiAra.length() < 1000) {
                try {
                    context.assets.open("hadiths/ara-qudsi.json.gz").use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            qudsiAra.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                } catch (_: Exception) {}
            }

            val qudsiUrd = File(hadithDir, "hadith-qudsi_urd.json")
            val qudsiUrdu = File(hadithDir, "hadith-qudsi_urdu.json")
            if (!qudsiUrd.exists() || qudsiUrd.length() < 1000) {
                try {
                    context.assets.open("hadiths/urd-qudsi.json.gz").use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            qudsiUrd.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                    if (qudsiUrd.exists() && qudsiUrd.length() > 500) {
                        qudsiUrd.copyTo(qudsiUrdu, overwrite = true)
                    }
                } catch (_: Exception) {}
            } else if (!qudsiUrdu.exists()) {
                try { qudsiUrd.copyTo(qudsiUrdu, overwrite = true) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun getEditionName(bookSlug: String): String {
        return when (bookSlug) {
            "sahih-bukhari", "bukhari" -> "bukhari"
            "sahih-muslim", "muslim" -> "muslim"
            "sunan-an-nasai", "nasai" -> "nasai"
            "sunan-abi-dawud", "abudawud", "abu-dawud" -> "abudawud"
            "jami-at-tirmidhi", "tirmidhi" -> "tirmidhi"
            "sunan-ibn-majah", "ibnmajah", "ibn-majah" -> "ibnmajah"
            "muwatta-malik", "malik" -> "malik"
            "musnad-ahmad", "ahmad" -> "ahmad"
            "arbaeen-nawawi", "nawawi" -> "nawawi"
            "hadith-qudsi", "qudsi" -> "qudsi"
            else -> bookSlug.replace("sahih-", "").replace("sunan-", "").replace("jami-at-", "").replace("muwatta-", "")
        }
    }

    fun normalizeLang(code: String): String {
        return when (code.lowercase().trim()) {
            "ur", "urdu", "urd" -> "urd"
            "en", "english", "eng" -> "eng"
            "bn", "bengali", "ben" -> "ben"
            "fr", "french", "fra" -> "fra"
            "id", "indonesian", "ind" -> "ind"
            "tr", "turkish", "tur" -> "tur"
            "ru", "russian", "rus" -> "rus"
            "ta", "tamil", "tam" -> "tam"
            else -> code.lowercase().trim()
        }
    }

    fun isLanguageFileValid(file: File?, langCode: String): Boolean {
        if (file == null || !file.exists() || file.length() < 1000) return false
        val norm = normalizeLang(langCode)
        return try {
            if (file.length() > 20_000) {
                val sample = ByteArray(32768)
                val read = java.io.FileInputStream(file).use { it.read(sample) }
                if (read <= 0) return false
                val sampleText = String(sample, 0, read, Charsets.UTF_8)
                sampleText.contains("\"hadiths\"") || sampleText.contains("hadiths") || sampleText.trimStart().startsWith("{")
            } else {
                file.length() > 1000
            }
        } catch (_: Exception) {
            file.length() > 1000
        }
    }

    fun isBookDownloaded(bookSlug: String): Boolean {
        if (bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi")) {
            return true
        }
        val araFile = File(hadithDir, "${bookSlug}_ara.json")
        val urdFile = File(hadithDir, "${bookSlug}_urdu.json")
        val urdShortFile = File(hadithDir, "${bookSlug}_urd.json")
        return (araFile.exists() && araFile.length() > 2000) ||
                (urdFile.exists() && isLanguageFileValid(urdFile, "urd")) ||
                (urdShortFile.exists() && isLanguageFileValid(urdShortFile, "urd"))
    }

    fun isLanguageDownloaded(bookSlug: String, langCode: String): Boolean {
        val norm = normalizeLang(langCode)
        if (norm == "urd" && bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi")) {
            return true
        }
        val file = File(hadithDir, "${bookSlug}_${norm}.json")
        if (isLanguageFileValid(file, norm)) return true
        if (norm == "urd") {
            val oldUrd = File(hadithDir, "${bookSlug}_urdu.json")
            if (isLanguageFileValid(oldUrd, "urd")) return true
            val urdShort = File(hadithDir, "${bookSlug}_urd.json")
            if (isLanguageFileValid(urdShort, "urd")) return true
        } else if (norm == "eng") {
            val engFile = File(hadithDir, "${bookSlug}_english.json")
            if (isLanguageFileValid(engFile, "eng")) return true
            val engShort = File(hadithDir, "${bookSlug}_eng.json")
            if (isLanguageFileValid(engShort, "eng")) return true
        }
        return false
    }

    fun isLanguageDownloading(bookSlug: String, langCode: String): Boolean {
        val normLang = normalizeLang(langCode)
        val key = "${bookSlug}_$normLang"
        return activeJobs[key]?.isActive == true ||
                _languageDownloadStatusMap.value[key] is HadithDownloadState.Downloading
    }

    fun getLanguagesForBook(bookSlug: String): List<HadithLanguage> {
        return supportedLanguages
    }

    fun isLanguageDownloadedAnyBook(langCode: String): Boolean {
        if (langCode == "ar" || langCode == "ara") {
            return supportedLanguages.any { isBookDownloaded("sahih-bukhari") } || hadithDir.listFiles { _, name -> name.endsWith("_ara.json") }?.isNotEmpty() == true
        }
        val normLang = normalizeLang(langCode)
        val allSlugs = listOf("sahih-bukhari", "sahih-muslim", "sunan-an-nasai", "sunan-abi-dawud", "jami-at-tirmidhi", "sunan-ibn-majah", "muwatta-malik", "musnad-ahmad", "arbaeen-nawawi", "hadith-qudsi")
        return allSlugs.any { isLanguageDownloaded(it, normLang) }
    }

    fun getBookFile(bookSlug: String): File? {
        val targetSlug = when (bookSlug) {
            "nawawi", "arbaeen-nawawi" -> "arbaeen-nawawi"
            "qudsi", "hadith-qudsi" -> "hadith-qudsi"
            else -> bookSlug
        }
        val araFile = File(hadithDir, "${targetSlug}_ara.json")
        if (araFile.exists() && araFile.length() > 500) return araFile
        val urdFile = File(hadithDir, "${targetSlug}_urdu.json")
        if (isLanguageFileValid(urdFile, "urd")) return urdFile
        val urdShort = File(hadithDir, "${targetSlug}_urd.json")
        if (isLanguageFileValid(urdShort, "urd")) return urdShort
        return null
    }

    fun getLanguageFile(bookSlug: String, langCode: String): File? {
        val norm = normalizeLang(langCode)
        val targetSlug = when (bookSlug) {
            "nawawi", "arbaeen-nawawi" -> "arbaeen-nawawi"
            "qudsi", "hadith-qudsi" -> "hadith-qudsi"
            else -> bookSlug
        }
        val file = File(hadithDir, "${targetSlug}_${norm}.json")
        if (isLanguageFileValid(file, norm)) return file
        if (norm == "urd") {
            val oldUrd = File(hadithDir, "${targetSlug}_urdu.json")
            if (isLanguageFileValid(oldUrd, "urd")) return oldUrd
            val urdShort = File(hadithDir, "${targetSlug}_urd.json")
            if (isLanguageFileValid(urdShort, "urd")) return urdShort
        }
        return null
    }

    fun setTranslationEnabled(enabled: Boolean) {
        _isTranslationEnabled.value = enabled
        prefs.edit().putBoolean("is_translation_enabled", enabled).apply()
    }

    fun toggleTranslation() {
        setTranslationEnabled(!_isTranslationEnabled.value)
    }

    fun selectLanguage(langCode: String) {
        _selectedLanguageCode.value = langCode
        prefs.edit().putString("selected_lang_code", langCode).apply()
        notifyTranslationsChanged()
    }

    fun refreshAllStatuses() {
        val slugs = listOf(
            "sahih-bukhari", "sahih-muslim", "sunan-an-nasai",
            "sunan-abi-dawud", "jami-at-tirmidhi", "sunan-ibn-majah",
            "muwatta-malik", "arbaeen-nawawi", "hadith-qudsi"
        )
        val current = _downloadStatusMap.value.toMutableMap()
        slugs.forEach { slug ->
            if (isBookDownloaded(slug)) {
                current[slug] = HadithDownloadState.Downloaded
            } else if (current[slug] !is HadithDownloadState.Downloading) {
                current[slug] = HadithDownloadState.NotDownloaded
            }
        }
        _downloadStatusMap.value = current

        refreshLanguageStatuses()
    }

    fun refreshLanguageStatuses() {
        val slugs = listOf(
            "sahih-bukhari", "sahih-muslim", "sunan-an-nasai",
            "sunan-abi-dawud", "jami-at-tirmidhi", "sunan-ibn-majah",
            "muwatta-malik", "arbaeen-nawawi", "hadith-qudsi"
        )
        val langMap = _languageDownloadStatusMap.value.toMutableMap()
        slugs.forEach { slug ->
            val bookLangs = getLanguagesForBook(slug)
            bookLangs.forEach { lang ->
                val isDownloaded = isLanguageDownloaded(slug, lang.code)
                val state = if (isDownloaded) {
                    HadithDownloadState.Downloaded
                } else if (langMap["${slug}_${lang.code}"] is HadithDownloadState.Downloading) {
                    langMap["${slug}_${lang.code}"]!!
                } else {
                    HadithDownloadState.NotDownloaded
                }
                langMap["${slug}_${lang.code}"] = state
                val norm = normalizeLang(lang.code)
                langMap["${slug}_$norm"] = state
            }
        }
        _languageDownloadStatusMap.value = langMap
    }

    private val cdnBases = listOf(
        "https://raw.githubusercontent.com/fawazahmed0/hadith-api/1/editions",
        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions",
        "https://fastly.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions",
        "https://gcore.jsdelivr.net/gh/fawazahmed0/hadith-api/1/editions",
        "https://cdn.statically.io/gh/fawazahmed0/hadith-api/1/editions"
    )

    suspend fun downloadBookSuspend(bookSlug: String): Boolean {
        if (bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi")) {
            extractBundledAssetsIfNeeded()
            updateBookStatus(bookSlug, HadithDownloadState.Downloaded)
            updateLanguageStatus(bookSlug, "urd", HadithDownloadState.Downloaded)
            refreshLanguageStatuses()
            notifyTranslationsChanged()
            return true
        }

        if (isBookDownloaded(bookSlug) && isLanguageDownloaded(bookSlug, "urd")) {
            val map = _downloadStatusMap.value.toMutableMap()
            map[bookSlug] = HadithDownloadState.Downloaded
            _downloadStatusMap.value = map
            return true
        }

        val edition = getEditionName(bookSlug)
        val targetAraFile = File(hadithDir, "${bookSlug}_ara.json")
        val targetUrdFile = File(hadithDir, "${bookSlug}_urd.json")
        val targetEngFile = File(hadithDir, "${bookSlug}_eng.json")

        updateBookStatus(bookSlug, HadithDownloadState.Downloading(0.05f))

        // 1. Download Arabic first (primary Arabic narration)
        var okAra = targetAraFile.exists() && targetAraFile.length() > 2000
        if (!okAra) {
            okAra = downloadWithFallbacks("ara-$edition.min.json", targetAraFile) { progress ->
                updateBookStatus(bookSlug, HadithDownloadState.Downloading(progress * 0.45f))
            }
        }

        // 2. Download Urdu translation together with the book (Urdu only, always from the API)
        var okUrd = isLanguageFileValid(targetUrdFile, "urd") || isLanguageDownloaded(bookSlug, "urd")
        if (!okUrd) {
            okUrd = downloadWithFallbacks("urd-$edition.min.json", targetUrdFile) { progress ->
                updateBookStatus(bookSlug, HadithDownloadState.Downloading(0.45f + progress * 0.4f))
            }
        }

        // 3. Also download English translation for bilingual coverage
        if (edition.isNotBlank() && !targetEngFile.exists()) {
            downloadWithFallbacks("eng-$edition.min.json", targetEngFile) { progress ->
                updateBookStatus(bookSlug, HadithDownloadState.Downloading(0.85f + progress * 0.15f))
            }
        }

        val success = (okAra || (targetAraFile.exists() && targetAraFile.length() > 2000))

        if (success) {
            updateBookStatus(bookSlug, HadithDownloadState.Downloaded)
            refreshLanguageStatuses()
            notifyTranslationsChanged()
        } else {
            updateBookStatus(bookSlug, HadithDownloadState.Error("Failed to download Hadith book"))
        }
        return success
    }

    fun downloadBook(bookSlug: String) {
        if (isBookDownloaded(bookSlug)) {
            val map = _downloadStatusMap.value.toMutableMap()
            map[bookSlug] = HadithDownloadState.Downloaded
            _downloadStatusMap.value = map
            return
        }

        if (activeJobs[bookSlug]?.isActive == true) return

        val job = appScope.launch {
            try {
                downloadBookSuspend(bookSlug)
            } catch (e: Exception) {
                updateBookStatus(bookSlug, HadithDownloadState.Error(e.localizedMessage ?: "Download failed"))
            } finally {
                activeJobs.remove(bookSlug)
            }
        }

        activeJobs[bookSlug] = job
    }

    fun downloadLanguage(bookSlug: String, langCode: String) {
        val normLang = normalizeLang(langCode)
        val key = "${bookSlug}_$normLang"
        if (isLanguageDownloaded(bookSlug, normLang)) {
            updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloaded)
            return
        }

        if (activeJobs[key]?.isActive == true) return

        val job = appScope.launch {
            val edition = getEditionName(bookSlug)
            val targetFile = File(hadithDir, "${bookSlug}_${normLang}.json")
            try {
                // 1. Ensure book is downloaded FIRST if not already downloaded
                if (!isBookDownloaded(bookSlug)) {
                    updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloading(0.05f))
                    val bookOk = downloadBookSuspend(bookSlug)
                    if (!bookOk && !isBookDownloaded(bookSlug)) {
                        updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Error("Book download failed"))
                        return@launch
                    }
                }

                // 2. Now proceed to download requested language translation
                updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloading(0.15f))

                // Urdu is fetched together with the book; nothing more to do if it is already there
                if (normLang == "urd" && isLanguageDownloaded(bookSlug, normLang)) {
                    updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloaded)
                    notifyTranslationsChanged()
                    return@launch
                }

                // Direct CDN download from API (strictly genuine translation)
                val ok = downloadWithFallbacks("${normLang}-$edition.min.json", targetFile) { progress ->
                    updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloading(0.15f + progress * 0.8f))
                }

                if (ok && isLanguageFileValid(targetFile, normLang)) {
                    updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Downloaded)
                    notifyTranslationsChanged()
                } else {
                    targetFile.delete()
                    updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Error("Translation download failed"))
                }
            } catch (e: Exception) {
                targetFile.delete()
                updateLanguageStatus(bookSlug, normLang, HadithDownloadState.Error(e.localizedMessage ?: "Download failed"))
            } finally {
                activeJobs.remove(key)
            }
        }

        activeJobs[key] = job
    }

    private suspend fun downloadWithFallbacks(fileName: String, targetFile: File, onProgress: (Float) -> Unit): Boolean {
        val candidates = if (fileName.endsWith(".min.json")) {
            listOf(fileName, fileName.replace(".min.json", ".json"))
        } else {
            listOf(fileName)
        }
        for (fName in candidates) {
            for (base in cdnBases) {
                val url = "$base/$fName"
                val success = downloadFileDirect(url, targetFile, onProgress)
                if (success && targetFile.exists() && targetFile.length() > 2000) return true
            }
        }
        return false
    }

    private suspend fun downloadFileDirect(url: String, targetFile: File, onProgress: (Float) -> Unit): Boolean {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()
                val response = NetworkModule.okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext false
                }

                val body = response.body
                if (body == null) {
                    response.close()
                    return@withContext false
                }
                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = 0L

                try {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate > 150L) {
                                    val progress = if (totalBytes > 0) {
                                        (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.95f)
                                    } else {
                                        val estimatedSize = 3_000_000f
                                        (totalRead.toFloat() / estimatedSize).coerceIn(0.08f, 0.92f)
                                    }
                                    onProgress(progress)
                                    lastProgressUpdate = now
                                }
                            }
                            output.flush()
                        }
                    }
                } finally {
                    response.close()
                }

                if (tempFile.exists() && tempFile.length() > 2000) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    true
                } else {
                    tempFile.delete()
                    false
                }
            } catch (_: Exception) {
                if (tempFile.exists()) tempFile.delete()
                false
            }
        }
    }

    fun deleteBook(bookSlug: String) {
        activeJobs[bookSlug]?.cancel()
        activeJobs.remove(bookSlug)

        // Delete all files related to this book
        hadithDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(bookSlug)) {
                file.delete()
            }
        }

        updateBookStatus(bookSlug, HadithDownloadState.NotDownloaded)
        refreshLanguageStatuses()
    }

    fun deleteLanguage(bookSlug: String, langCode: String) {
        val norm = normalizeLang(langCode)
        val key = "${bookSlug}_$norm"
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
        activeJobs["${bookSlug}_$langCode"]?.cancel()
        activeJobs.remove("${bookSlug}_$langCode")

        val file = File(hadithDir, "${bookSlug}_${norm}.json")
        if (file.exists()) file.delete()
        val oldFile = if (norm == "urd") File(hadithDir, "${bookSlug}_urdu.json") else null
        if (oldFile?.exists() == true) oldFile.delete()

        updateLanguageStatus(bookSlug, norm, HadithDownloadState.NotDownloaded)
    }

    private fun updateBookStatus(bookSlug: String, state: HadithDownloadState) {
        val current = _downloadStatusMap.value.toMutableMap()
        current[bookSlug] = state
        _downloadStatusMap.value = current
        if (state is HadithDownloadState.Downloaded || state is HadithDownloadState.NotDownloaded) {
            notifyTranslationsChanged()
        }
    }

    private fun updateLanguageStatus(bookSlug: String, langCode: String, state: HadithDownloadState) {
        val norm = normalizeLang(langCode)
        val current = _languageDownloadStatusMap.value.toMutableMap()
        current["${bookSlug}_$norm"] = state
        current["${bookSlug}_$langCode"] = state
        _languageDownloadStatusMap.value = current
        if (state is HadithDownloadState.Downloaded || state is HadithDownloadState.NotDownloaded) {
            notifyTranslationsChanged()
        }
    }

    companion object {
        @Volatile
        private var instance: HadithDownloadManager? = null

        fun getInstance(context: Context): HadithDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: HadithDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
