package com.example.data.quran

import android.content.Context
import android.content.SharedPreferences
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class QuranTranslationLanguage(
    val id: String, // e.g. "ur.jalandhry", "en.sahih", "hi.hindi"
    val languageCode: String, // "ur", "en", "hi"
    val englishName: String,
    val nativeName: String,
    val author: String,
    val isBuiltIn: Boolean = false
)

sealed class TranslationDownloadState {
    object NotDownloaded : TranslationDownloadState()
    data class Downloading(val progress: Float) : TranslationDownloadState()
    object Downloaded : TranslationDownloadState()
    data class Error(val message: String) : TranslationDownloadState()
}

class QuranTranslationManager(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: QuranTranslationManager? = null

        fun getInstance(context: Context): QuranTranslationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuranTranslationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("quran_translations_prefs", Context.MODE_PRIVATE)
    private val translationsDir = File(context.filesDir, "quran_translations").apply { mkdirs() }

    val supportedLanguages: List<QuranTranslationLanguage> = listOf(
        QuranTranslationLanguage("ur.jalandhry", "ur", "Urdu", "اردو", "مولانا فتح محمد جالندھریؒ", isBuiltIn = true),
        QuranTranslationLanguage("en.sahih", "en", "English", "English", "Saheeh International", isBuiltIn = false),
        QuranTranslationLanguage("hi.hindi", "hi", "Hindi", "हिन्दी", "फ़ारूक़ ख़ान और अहमद"),
        QuranTranslationLanguage("bn.bengali", "bn", "Bengali", "বাংলা", "মুহিউদ্দীন খান"),
        QuranTranslationLanguage("id.indonesian", "id", "Indonesian", "Bahasa Indonesia", "Kementerian Agama RI"),
        QuranTranslationLanguage("tr.diyanet", "tr", "Turkish", "Türkçe", "Diyanet İşleri Başkanlığı"),
        QuranTranslationLanguage("fr.hamidullah", "fr", "French", "Français", "Muhammad Hamidullah"),
        QuranTranslationLanguage("es.cortes", "es", "Spanish", "Español", "Julio Cortes"),
        QuranTranslationLanguage("ru.kuliev", "ru", "Russian", "Русский", "Эльмир Кулиев"),
        QuranTranslationLanguage("de.bubenheim", "de", "German", "Deutsch", "A. S. F. Bubenheim and N. Elyas"),
        QuranTranslationLanguage("fa.ansarian", "fa", "Persian", "فارسی", "حسین انصاریان"),
        QuranTranslationLanguage("zh.jian", "zh", "Chinese", "中文", "马坚 (Ma Jian)"),
        QuranTranslationLanguage("ms.basmeih", "ms", "Malay", "Bahasa Melayu", "Abdullah Muhammad Basmeih"),
        QuranTranslationLanguage("ta.tamil", "ta", "Tamil", "தமிழ்", "ஜான் டிரஸ்ட் ஃபவுண்டேஷன்"),
        QuranTranslationLanguage("ps.abdulwali", "ps", "Pashto", "پښتو", "عبدالولي خان"),
        QuranTranslationLanguage("sd.amroti", "sd", "Sindhi", "سنڌي", "تاج محمود امروٽي"),
        QuranTranslationLanguage("bs.korkut", "bs", "Bosnian", "Bosanski", "Besim Korkut"),
        QuranTranslationLanguage("nl.siregar", "nl", "Dutch", "Nederlands", "Sofian S. Siregar"),
        QuranTranslationLanguage("it.piccardo", "it", "Italian", "Italiano", "Hamza Roberto Piccardo"),
        QuranTranslationLanguage("pt.elhayek", "pt", "Portuguese", "Português", "Samir El-Hayek"),
        QuranTranslationLanguage("sq.ahmeti", "sq", "Albanian", "Shqip", "Sherif Ahmeti"),
        QuranTranslationLanguage("sv.bernstrom", "sv", "Swedish", "Svenska", "Knut Bernström"),
        QuranTranslationLanguage("so.abduh", "so", "Somali", "Soomaali", "Maxamuud Maxamed Cabduh"),
        QuranTranslationLanguage("sw.barwani", "sw", "Swahili", "Kiswahili", "Ali Muhsin Al-Barwani"),
        QuranTranslationLanguage("ha.gumi", "ha", "Hausa", "Hausa", "Abubakar Mahmoud Gumi"),
        QuranTranslationLanguage("ja.japanese", "ja", "Japanese", "日本語", "リョウイチ・ミタ (Ryoichi Mita)"),
        QuranTranslationLanguage("ko.korean", "ko", "Korean", "한국어", "한국어 번역"),
        QuranTranslationLanguage("uz.sadiq", "uz", "Uzbek", "Oʻzbekcha", "Muhammad Sodiq Muhammad Yusuf"),
        QuranTranslationLanguage("ku.asan", "ku", "Kurdish", "Kurdî", "بورهان محه‌مه‌د ئه‌مین"),
        QuranTranslationLanguage("az.musayev", "az", "Azeri", "Azərbaycan", "Alixan Musayev")
    )

    private val _isTranslationEnabled = MutableStateFlow(prefs.getBoolean("is_translation_enabled", false))
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val _selectedLanguageId = MutableStateFlow(prefs.getString("selected_translation_id", "ur.jalandhry") ?: "ur.jalandhry")
    val selectedLanguageId: StateFlow<String> = _selectedLanguageId.asStateFlow()

    private val _downloadStatusMap = MutableStateFlow<Map<String, TranslationDownloadState>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<String, TranslationDownloadState>> = _downloadStatusMap.asStateFlow()

    private val _downloadedLanguageCodes = MutableStateFlow<Set<String>>(setOf("ur", "ar"))
    val downloadedLanguageCodes: StateFlow<Set<String>> = _downloadedLanguageCodes.asStateFlow()

    private val _translationRevision = MutableStateFlow(0)
    val translationRevision: StateFlow<Int> = _translationRevision.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    // In-memory cache for parsed translations: Map<editionId, Map<surahNum, Map<ayahInSurah, text>>>
    private val parsedTranslationsCache = ConcurrentHashMap<String, Map<Int, Map<Int, String>>>()
    // In-memory cache for parsed Word-by-Word data: Map<editionId, Map<surahNum, Map<ayahInSurah, List<QuranWord>>>>
    private val parsedWbwCache = ConcurrentHashMap<String, Map<Int, Map<Int, List<com.example.domain.model.QuranWord>>>>()

    init {
        refreshAllStatuses()
    }

    /**
     * Ensures translation and Word-by-Word data for a specific Surah are loaded in memory.
     * If not loaded, fetches on-demand from AlQuran Cloud and Quran.com APIs
     * and caches them in memory.
     */
    suspend fun ensureSurahTranslationLoaded(surahNumber: Int, editionId: String) = withContext(Dispatchers.IO) {
        if (editionId == "ur.jalandhry") return@withContext
        val existing = parsedTranslationsCache[editionId]?.get(surahNumber)
        val existingWbw = parsedWbwCache[editionId]?.get(surahNumber)
        if (existing != null && existing.isNotEmpty() && existingWbw != null && existingWbw.isNotEmpty()) return@withContext

        // Check if full edition or bundled WbW is cached on disk
        val fullFile = File(translationsDir, "$editionId.json")
        if (fullFile.exists() && fullFile.length() > 5000) {
            getTranslationForAyah(surahNumber, 1) // triggers disk parsing into cache
            getWbwWordsForSurah(surahNumber, editionId) // triggers WbW disk parsing into cache
            if (parsedTranslationsCache[editionId]?.get(surahNumber)?.isNotEmpty() == true) {
                return@withContext
            }
        }

        // Fetch Surah-specific translation from AlQuran Cloud API
        if (existing == null || existing.isEmpty()) {
            try {
                val url = "https://api.alquran.cloud/v1/surah/$surahNumber/$editionId"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = com.example.data.remote.NetworkModule.okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val data = root.optJSONObject("data")
                        val ayahsArray = data?.optJSONArray("ayahs")
                        if (ayahsArray != null) {
                            val ayahMap = mutableMapOf<Int, String>()
                            for (i in 0 until ayahsArray.length()) {
                                val aObj = ayahsArray.getJSONObject(i)
                                val inSurah = aObj.getInt("numberInSurah")
                                val text = aObj.getString("text")
                                ayahMap[inSurah] = text
                            }
                            val currentEditionMap = parsedTranslationsCache[editionId]?.toMutableMap() ?: mutableMapOf()
                            currentEditionMap[surahNumber] = ayahMap
                            parsedTranslationsCache[editionId] = currentEditionMap
                            _translationRevision.value++
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently handle transient network errors
            }
        }

        // Fetch Surah-specific Word-by-Word data from QuranDotComApi
        if (existingWbw == null || existingWbw.isEmpty()) {
            try {
                val lang = supportedLanguages.firstOrNull { it.id == editionId }
                val langCode = lang?.languageCode ?: "en"
                val qdcRes = com.example.data.remote.NetworkModule.quranDotComApi.getVersesByChapter(
                    chapterId = surahNumber,
                    language = langCode,
                    words = true,
                    wordFields = "text_uthmani,translation",
                    perPage = 300
                )
                val verses = qdcRes.verses
                if (!verses.isNullOrEmpty()) {
                    val ayahsWbwMap = mutableMapOf<Int, List<com.example.domain.model.QuranWord>>()
                    for (verse in verses) {
                        val wordsList = verse.words?.mapNotNull { wDto ->
                            val ar = wDto.textUthmani?.takeIf { it.isNotBlank() } ?: wDto.text?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            val tr = wDto.translation?.text?.trim().orEmpty()
                            val isEnd = wDto.charTypeName.equals("end", ignoreCase = true)
                            com.example.domain.model.QuranWord(
                                id = wDto.id,
                                position = wDto.position,
                                textUthmani = ar,
                                urduTranslation = "",
                                englishTranslation = if (langCode == "en") tr else "",
                                activeTranslation = tr,
                                isVerseEndMarker = isEnd
                            )
                        }.orEmpty()
                        ayahsWbwMap[verse.verseNumber] = wordsList
                    }
                    val currentWbwEditionMap = parsedWbwCache[editionId]?.toMutableMap() ?: mutableMapOf()
                    currentWbwEditionMap[surahNumber] = ayahsWbwMap
                    parsedWbwCache[editionId] = currentWbwEditionMap
                    _translationRevision.value++
                }
            } catch (_: Exception) {
                // Silently handle transient network errors
            }
        }
    }

    fun isDownloaded(id: String): Boolean {
        val lang = supportedLanguages.firstOrNull { it.id == id }
        if (lang?.isBuiltIn == true) return true
        val file = File(translationsDir, "${id}.json")
        return file.exists() && file.length() > 5000
    }

    fun isLanguageCodeDownloaded(langCode: String): Boolean {
        if (langCode == "ur" || langCode == "ar") return true
        val matched = supportedLanguages.firstOrNull { it.languageCode == langCode } ?: return false
        return isDownloaded(matched.id)
    }

    fun refreshAllStatuses() {
        val current = _downloadStatusMap.value.toMutableMap()
        val downloadedCodes = mutableSetOf("ur", "ar")
        supportedLanguages.forEach { lang ->
            if (lang.isBuiltIn) {
                current[lang.id] = TranslationDownloadState.Downloaded
                downloadedCodes.add(lang.languageCode)
            } else if (isDownloaded(lang.id)) {
                current[lang.id] = TranslationDownloadState.Downloaded
                downloadedCodes.add(lang.languageCode)
            } else if (current[lang.id] !is TranslationDownloadState.Downloading) {
                current[lang.id] = TranslationDownloadState.NotDownloaded
            }
        }
        _downloadStatusMap.value = current
        _downloadedLanguageCodes.value = downloadedCodes
    }

    fun setTranslationEnabled(enabled: Boolean) {
        _isTranslationEnabled.value = enabled
        prefs.edit().putBoolean("is_translation_enabled", enabled).apply()
    }

    fun toggleTranslationEnabled() {
        setTranslationEnabled(!_isTranslationEnabled.value)
    }

    fun selectLanguage(id: String) {
        _selectedLanguageId.value = id
        prefs.edit().putString("selected_translation_id", id).apply()
        _translationRevision.value++
    }

    fun downloadTranslation(id: String) {
        if (isDownloaded(id)) {
            val map = _downloadStatusMap.value.toMutableMap()
            map[id] = TranslationDownloadState.Downloaded
            _downloadStatusMap.value = map
            return
        }

        if (activeJobs[id]?.isActive == true) return

        val job = appScope.launch {
            val lang = supportedLanguages.firstOrNull { it.id == id }
            val langCode = lang?.languageCode ?: "en"

            val url = "https://api.alquran.cloud/v1/quran/$id"
            val targetFile = File(translationsDir, "$id.json")
            val tempFile = File(translationsDir, "$id.json.tmp")

            val targetWbwFile = File(translationsDir, "${id}_wbw.json.gz")
            val tempWbwFile = File(translationsDir, "${id}_wbw.json.gz.tmp")

            try {
                updateStatus(id, TranslationDownloadState.Downloading(0.05f))

                // Phase 1: Download Ba-Muhawra translation file (0.05f to 0.40f)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                val response = NetworkModule.okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    updateStatus(id, TranslationDownloadState.Error("Server returned code ${response.code}"))
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    updateStatus(id, TranslationDownloadState.Error("Empty response body"))
                    return@launch
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = 0L

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 200L && totalBytes > 0) {
                                val progress = (0.05f + (totalRead.toFloat() / totalBytes.toFloat()) * 0.35f).coerceIn(0.05f, 0.40f)
                                updateStatus(id, TranslationDownloadState.Downloading(progress))
                                lastProgressUpdate = now
                            }
                        }
                        output.flush()
                    }
                }

                if (!tempFile.exists() || tempFile.length() <= 5000) {
                    tempFile.delete()
                    updateStatus(id, TranslationDownloadState.Error("Downloaded translation file is invalid"))
                    return@launch
                }

                // Phase 2: Download corresponding Word-by-Word dataset (0.40f to 0.95f)
                updateStatus(id, TranslationDownloadState.Downloading(0.42f))
                val wbwSurahsMap = mutableMapOf<Int, Map<Int, List<com.example.domain.model.QuranWord>>>()
                val batchSize = 10
                val totalSurahs = 114
                var completedSurahs = 0

                val surahNumbers = (1..totalSurahs).toList()
                val chunks = surahNumbers.chunked(batchSize)

                for (chunk in chunks) {
                    val deferredList = chunk.map { sNum ->
                        async(Dispatchers.IO) {
                            var attempts = 0
                            var versesList: List<com.example.data.remote.QuranDotComVerseDto>? = null
                            while (attempts < 2 && versesList == null) {
                                try {
                                    val res = NetworkModule.quranDotComApi.getVersesByChapter(
                                        chapterId = sNum,
                                        language = langCode,
                                        words = true,
                                        wordFields = "text_uthmani,translation",
                                        perPage = 300
                                    )
                                    versesList = res.verses
                                } catch (_: Exception) {
                                    attempts++
                                    delay(200)
                                }
                            }
                            Pair(sNum, versesList)
                        }
                    }

                    val results = deferredList.awaitAll()
                    for ((sNum, verses) in results) {
                        if (verses != null) {
                            val ayahsMap = mutableMapOf<Int, List<com.example.domain.model.QuranWord>>()
                            for (v in verses) {
                                val wordsList = v.words?.mapNotNull { wDto ->
                                    val ar = wDto.textUthmani?.takeIf { it.isNotBlank() } ?: wDto.text?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                    val tr = wDto.translation?.text?.trim().orEmpty()
                                    val isEnd = wDto.charTypeName.equals("end", ignoreCase = true)
                                    com.example.domain.model.QuranWord(
                                        id = wDto.id,
                                        position = wDto.position,
                                        textUthmani = ar,
                                        urduTranslation = "",
                                        englishTranslation = if (langCode == "en") tr else "",
                                        activeTranslation = tr,
                                        isVerseEndMarker = isEnd
                                    )
                                }.orEmpty()
                                ayahsMap[v.verseNumber] = wordsList
                            }
                            wbwSurahsMap[sNum] = ayahsMap
                        }
                        completedSurahs++
                    }

                    val progress = (0.42f + (completedSurahs.toFloat() / totalSurahs.toFloat()) * 0.53f).coerceIn(0.42f, 0.95f)
                    updateStatus(id, TranslationDownloadState.Downloading(progress))
                }

                // Compress and write WbW dataset
                if (wbwSurahsMap.isNotEmpty()) {
                    val rootJson = JSONObject()
                    for ((sNum, ayahs) in wbwSurahsMap) {
                        val sObj = JSONObject()
                        for ((aNum, words) in ayahs) {
                            val wordsArr = org.json.JSONArray()
                            for (w in words) {
                                val wObj = JSONObject()
                                wObj.put("id", w.id)
                                wObj.put("pos", w.position)
                                wObj.put("ar", w.textUthmani)
                                wObj.put("tr", w.activeTranslation)
                                wObj.put("end", w.isVerseEndMarker)
                                wordsArr.put(wObj)
                            }
                            sObj.put(aNum.toString(), wordsArr)
                        }
                        rootJson.put(sNum.toString(), sObj)
                    }

                    val gzOut = java.util.zip.GZIPOutputStream(FileOutputStream(tempWbwFile))
                    gzOut.bufferedWriter(Charsets.UTF_8).use { it.write(rootJson.toString()) }
                }

                // Atomically finalize files
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                if (tempWbwFile.exists() && tempWbwFile.length() > 500) {
                    if (targetWbwFile.exists()) targetWbwFile.delete()
                    tempWbwFile.renameTo(targetWbwFile)
                }

                parsedTranslationsCache.remove(id)
                if (wbwSurahsMap.isNotEmpty()) {
                    parsedWbwCache[id] = wbwSurahsMap
                }

                updateStatus(id, TranslationDownloadState.Downloaded)
                refreshAllStatuses()
                _translationRevision.value++
            } catch (e: Exception) {
                tempFile.delete()
                tempWbwFile.delete()
                updateStatus(id, TranslationDownloadState.Error(e.localizedMessage ?: "Download failed"))
            } finally {
                activeJobs.remove(id)
            }
        }

        activeJobs[id] = job
    }

    fun isTranslationDownloaded(id: String): Boolean {
        if (id == "ur.jalandhry" || id == "en.sahih") return true
        val file = File(translationsDir, "$id.json")
        return file.exists() && file.length() > 5000
    }

    fun deleteTranslation(id: String) {
        val lang = supportedLanguages.firstOrNull { it.id == id }
        if (lang?.isBuiltIn == true) return // Never delete built-in translations

        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        parsedTranslationsCache.remove(id)
        parsedWbwCache.remove(id)

        val targetFile = File(translationsDir, "$id.json")
        if (targetFile.exists()) targetFile.delete()
        val tempFile = File(translationsDir, "$id.json.tmp")
        if (tempFile.exists()) tempFile.delete()

        val wbwFile = File(translationsDir, "${id}_wbw.json")
        if (wbwFile.exists()) wbwFile.delete()
        val wbwGzFile = File(translationsDir, "${id}_wbw.json.gz")
        if (wbwGzFile.exists()) wbwGzFile.delete()
        val tempWbwFile = File(translationsDir, "${id}_wbw.json.gz.tmp")
        if (tempWbwFile.exists()) tempWbwFile.delete()

        // If currently selected, revert to default Urdu
        if (_selectedLanguageId.value == id) {
            selectLanguage("ur.jalandhry")
        }

        updateStatus(id, TranslationDownloadState.NotDownloaded)
        refreshAllStatuses()
        _translationRevision.value++
    }

    fun getWbwWordsForSurah(surahNumber: Int, editionId: String): Map<Int, List<com.example.domain.model.QuranWord>>? {
        if (editionId == "ur.jalandhry" || editionId == "ur") {
            return com.example.data.repository.BuiltInWbwProvider.getWordsForSurah(context, surahNumber)
        }

        parsedWbwCache[editionId]?.get(surahNumber)?.let { return it }

        val wbwGzFile = File(translationsDir, "${editionId}_wbw.json.gz")
        val wbwFile = File(translationsDir, "${editionId}_wbw.json")
        val fileToRead = if (wbwGzFile.exists() && wbwGzFile.length() > 500) wbwGzFile
                         else if (wbwFile.exists() && wbwFile.length() > 500) wbwFile
                         else null

        if (fileToRead != null) {
            try {
                val isGz = fileToRead.name.endsWith(".gz")
                val inputStream = if (isGz) java.util.zip.GZIPInputStream(fileToRead.inputStream()) else fileToRead.inputStream()
                val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(text)
                val parsedSurahs = mutableMapOf<Int, Map<Int, List<com.example.domain.model.QuranWord>>>()
                val sKeys = root.keys()
                while (sKeys.hasNext()) {
                    val sKey = sKeys.next()
                    val sNum = sKey.toIntOrNull() ?: continue
                    val sObj = root.getJSONObject(sKey)
                    val ayahsMap = mutableMapOf<Int, List<com.example.domain.model.QuranWord>>()
                    val aKeys = sObj.keys()
                    while (aKeys.hasNext()) {
                        val aKey = aKeys.next()
                        val aNum = aKey.toIntOrNull() ?: continue
                        val wordsArr = sObj.getJSONArray(aKey)
                        val wordsList = mutableListOf<com.example.domain.model.QuranWord>()
                        for (wIdx in 0 until wordsArr.length()) {
                            val wObj = wordsArr.getJSONObject(wIdx)
                            val wordId = wObj.optLong("id", 0L)
                            val pos = wObj.optInt("pos", 0)
                            val ar = wObj.optString("ar", "")
                            val tr = wObj.optString("tr", "")
                            val isEnd = wObj.optBoolean("end", false)
                            wordsList.add(
                                com.example.domain.model.QuranWord(
                                    id = wordId,
                                    position = pos,
                                    textUthmani = ar,
                                    urduTranslation = "",
                                    englishTranslation = if (editionId == "en.sahih") tr else "",
                                    activeTranslation = tr,
                                    isVerseEndMarker = isEnd
                                )
                            )
                        }
                        ayahsMap[aNum] = wordsList
                    }
                    parsedSurahs[sNum] = ayahsMap
                }
                parsedWbwCache[editionId] = parsedSurahs
                return parsedSurahs[surahNumber]
            } catch (_: Exception) {
                // Graceful fallback
            }
        }
        return null
    }

    fun getTranslationForAyah(surahNumber: Int, ayahNumberInSurah: Int): String? {
        val currentId = _selectedLanguageId.value
        if (currentId == "ur.jalandhry") {
            // Built-in Urdu translation is already loaded in Ayah model
            return null
        }

        var translationMap = parsedTranslationsCache[currentId]
        if (translationMap == null) {
            val file = File(translationsDir, "$currentId.json")
            if (file.exists() && file.length() > 5000) {
                try {
                    val root = JSONObject(file.readText(Charsets.UTF_8))
                    val data = root.optJSONObject("data")
                    val surahs = data?.optJSONArray("surahs")
                    if (surahs != null) {
                        val newMap = mutableMapOf<Int, MutableMap<Int, String>>()
                        for (i in 0 until surahs.length()) {
                            val sObj = surahs.getJSONObject(i)
                            val sNum = sObj.getInt("number")
                            val ayahs = sObj.getJSONArray("ayahs")
                            val ayahMap = mutableMapOf<Int, String>()
                            for (j in 0 until ayahs.length()) {
                                val aObj = ayahs.getJSONObject(j)
                                val inSurah = aObj.getInt("numberInSurah")
                                val text = aObj.getString("text")
                                ayahMap[inSurah] = text
                            }
                            newMap[sNum] = ayahMap
                        }
                        parsedTranslationsCache[currentId] = newMap
                        translationMap = newMap
                    }
                } catch (_: Exception) {
                    return null
                }
            }
        }

        return translationMap?.get(surahNumber)?.get(ayahNumberInSurah)
    }

    private fun updateStatus(id: String, state: TranslationDownloadState) {
        val current = _downloadStatusMap.value.toMutableMap()
        current[id] = state
        _downloadStatusMap.value = current
    }
}
