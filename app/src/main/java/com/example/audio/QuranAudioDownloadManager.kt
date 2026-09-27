package com.example.audio

import android.content.Context
import com.example.data.remote.NetworkModule
import com.example.domain.model.QuranReciter
import com.example.domain.model.SUPPORTED_RECITERS
import com.example.domain.model.Surah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.Locale

class QuranAudioDownloadManager(private val context: Context) {

    // Application-level background scope so downloads survive user navigating away
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Internal app private storage directory - isolated in app data, not visible externally
    private val audioDir: File by lazy {
        File(context.filesDir, "quran_audio").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // Map of Surah Number -> Set of downloaded reciter IDs
    private val _downloadedRecitersMap = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val downloadedRecitersMap: StateFlow<Map<Int, Set<String>>> = _downloadedRecitersMap.asStateFlow()

    // Surahs that have at least one reciter downloaded
    private val _downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSurahs: StateFlow<Set<Int>> = _downloadedSurahs.asStateFlow()

    // Download in-progress keys: "${surahNumber}_${reciterId}"
    private val _downloadingKeys = MutableStateFlow<Set<String>>(emptySet())
    val downloadingKeys: StateFlow<Set<String>> = _downloadingKeys.asStateFlow()

    // Download progress map: "${surahNumber}_${reciterId}" -> Float (0.0f .. 1.0f)
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    // Download status text map: "${surahNumber}_${reciterId}" -> String (e.g. "45% (1.8 MB / 4.0 MB)")
    private val _downloadStatusMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadStatusMap: StateFlow<Map<String, String>> = _downloadStatusMap.asStateFlow()

    init {
        refreshDownloadedAudios()
    }

    fun refreshDownloadedAudios() {
        val files = audioDir.listFiles() ?: emptyArray()
        val map = mutableMapOf<Int, MutableSet<String>>()

        for (file in files) {
            if (file.isFile && file.length() > 0 && file.name.endsWith(".mp3")) {
                val base = file.name.removeSuffix(".mp3")
                // Format: surah_{surahNumber}_{reciterId} OR legacy: surah_{surahNumber}
                if (base.startsWith("surah_")) {
                    val parts = base.removePrefix("surah_").split("_", limit = 2)
                    val surahNum = parts.getOrNull(0)?.toIntOrNull()
                    if (surahNum != null) {
                        val reciterId = if (parts.size > 1) parts[1] else "ar.alafasy"
                        map.getOrPut(surahNum) { mutableSetOf() }.add(reciterId)
                    }
                }
            }
        }

        _downloadedRecitersMap.value = map
        _downloadedSurahs.value = map.keys.toSet()
    }

    fun isReciterDownloaded(surahNumber: Int, reciterId: String): Boolean {
        val set = _downloadedRecitersMap.value[surahNumber] ?: emptySet()
        if (set.contains(reciterId)) return true
        if (reciterId == "ar.alafasy") {
            val legacy = File(audioDir, "surah_${surahNumber}.mp3")
            if (legacy.exists() && legacy.length() > 0) return true
        }
        return false
    }

    fun areAllRecitersDownloaded(surahNumber: Int): Boolean {
        if (SUPPORTED_RECITERS.isEmpty()) return false
        return SUPPORTED_RECITERS.all { isReciterDownloaded(surahNumber, it.id) }
    }

    fun getDownloadedRecitersForSurah(surahNumber: Int): Set<String> {
        val set = _downloadedRecitersMap.value[surahNumber]?.toMutableSet() ?: mutableSetOf()
        val legacy = File(audioDir, "surah_${surahNumber}.mp3")
        if (legacy.exists() && legacy.length() > 0) {
            set.add("ar.alafasy")
        }
        return set
    }

    fun isReciterDownloading(surahNumber: Int, reciterId: String): Boolean {
        return _downloadingKeys.value.contains("${surahNumber}_${reciterId}")
    }

    fun isSurahDownloadingAny(surahNumber: Int): Boolean {
        return _downloadingKeys.value.any { it.startsWith("${surahNumber}_") }
    }

    fun isDownloaded(surahNumber: Int): Boolean {
        return _downloadedSurahs.value.contains(surahNumber)
    }

    fun getLocalAudioFile(surahNumber: Int, reciterId: String): File? {
        val file = File(audioDir, "surah_${surahNumber}_${reciterId}.mp3")
        if (file.exists() && file.length() > 0) return file
        if (reciterId == "ar.alafasy") {
            val legacy = File(audioDir, "surah_${surahNumber}.mp3")
            if (legacy.exists() && legacy.length() > 0) return legacy
        }
        return null
    }

    suspend fun downloadReciterAudio(surahNumber: Int, reciter: QuranReciter): Boolean = withContext(Dispatchers.IO) {
        val key = "${surahNumber}_${reciter.id}"
        if (_downloadingKeys.value.contains(key)) return@withContext false

        _downloadingKeys.value = _downloadingKeys.value + key
        _downloadProgressMap.value = _downloadProgressMap.value + (key to 0f)
        _downloadStatusMap.value = _downloadStatusMap.value + (key to "Starting...")
        val tempFile = File(audioDir, "surah_${surahNumber}_${reciter.id}.tmp")
        val targetFile = File(audioDir, "surah_${surahNumber}_${reciter.id}.mp3")

        try {
            val url = reciter.getAudioUrl(surahNumber)
            val request = Request.Builder().url(url).build()
            val response = NetworkModule.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                tempFile.delete()
                _downloadingKeys.value = _downloadingKeys.value - key
                _downloadProgressMap.value = _downloadProgressMap.value - key
                _downloadStatusMap.value = _downloadStatusMap.value - key
                return@withContext false
            }

            val body = response.body ?: run {
                tempFile.delete()
                _downloadingKeys.value = _downloadingKeys.value - key
                _downloadProgressMap.value = _downloadProgressMap.value - key
                _downloadStatusMap.value = _downloadStatusMap.value - key
                return@withContext false
            }

            val contentLength = body.contentLength()
            var bytesRead = 0L
            val buffer = ByteArray(8192)

            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) {
                            val progress = (bytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            _downloadProgressMap.value = _downloadProgressMap.value + (key to progress)
                            val percent = (progress * 100).toInt()
                            _downloadStatusMap.value = _downloadStatusMap.value + (key to "$percent%")
                        } else {
                            _downloadStatusMap.value = _downloadStatusMap.value + (key to "Downloading...")
                        }
                    }
                }
            }

            if (tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                refreshDownloadedAudios()
                _downloadingKeys.value = _downloadingKeys.value - key
                _downloadProgressMap.value = _downloadProgressMap.value - key
                _downloadStatusMap.value = _downloadStatusMap.value - key
                return@withContext true
            } else {
                tempFile.delete()
                _downloadingKeys.value = _downloadingKeys.value - key
                _downloadProgressMap.value = _downloadProgressMap.value - key
                _downloadStatusMap.value = _downloadStatusMap.value - key
                return@withContext false
            }
        } catch (_: Exception) {
            tempFile.delete()
            _downloadingKeys.value = _downloadingKeys.value - key
            _downloadProgressMap.value = _downloadProgressMap.value - key
            _downloadStatusMap.value = _downloadStatusMap.value - key
            return@withContext false
        }
    }

    /**
     * Starts background download using the application-level scope.
     * Keeps running even if the user leaves the Surah reader or closes the activity.
     */
    fun startBackgroundDownload(surahNumber: Int, reciter: QuranReciter) {
        appScope.launch {
            downloadReciterAudio(surahNumber, reciter)
        }
    }

    /**
     * Starts background download for multiple selected reciters.
     */
    fun startBackgroundDownloadSelected(surahNumber: Int, reciters: List<QuranReciter>) {
        appScope.launch {
            for (reciter in reciters) {
                if (!isReciterDownloaded(surahNumber, reciter.id)) {
                    downloadReciterAudio(surahNumber, reciter)
                }
            }
        }
    }

    suspend fun downloadAllReciters(surahNumber: Int): Boolean = withContext(Dispatchers.IO) {
        var anyFailed = false
        for (reciter in SUPPORTED_RECITERS) {
            if (!isReciterDownloaded(surahNumber, reciter.id)) {
                val ok = downloadReciterAudio(surahNumber, reciter)
                if (!ok) anyFailed = true
            }
        }
        return@withContext !anyFailed
    }

    suspend fun downloadSurahAudio(surah: Surah): Boolean {
        val defaultReciter = SUPPORTED_RECITERS.firstOrNull { it.id == "ar.alafasy" } ?: SUPPORTED_RECITERS.first()
        return downloadReciterAudio(surah.number, defaultReciter)
    }

    fun deleteReciterAudio(surahNumber: Int, reciterId: String): Boolean {
        val file = File(audioDir, "surah_${surahNumber}_${reciterId}.mp3")
        var deleted = if (file.exists()) file.delete() else false
        if (reciterId == "ar.alafasy") {
            val legacy = File(audioDir, "surah_${surahNumber}.mp3")
            if (legacy.exists()) {
                deleted = legacy.delete() || deleted
            }
        }
        refreshDownloadedAudios()
        return deleted
    }

    fun deleteAllRecitersAudio(surahNumber: Int): Boolean {
        var anyDeleted = false
        for (reciter in SUPPORTED_RECITERS) {
            val del = deleteReciterAudio(surahNumber, reciter.id)
            if (del) anyDeleted = true
        }
        val legacy = File(audioDir, "surah_${surahNumber}.mp3")
        if (legacy.exists()) {
            legacy.delete()
            anyDeleted = true
        }
        refreshDownloadedAudios()
        return anyDeleted
    }

    fun deleteSurahAudio(surahNumber: Int): Boolean {
        return deleteAllRecitersAudio(surahNumber)
    }
}
