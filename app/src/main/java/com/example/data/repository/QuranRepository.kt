package com.example.data.repository

import android.content.Context
import com.example.data.remote.NetworkModule
import com.example.domain.model.Ayah
import com.example.domain.model.JuzInfo
import com.example.domain.model.QuranWord
import com.example.domain.model.Surah
import com.example.domain.util.ArabicNormalizationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class QuranRepository(private val context: Context? = null) {

    private val surahAyahsCache = ConcurrentHashMap<String, List<Ayah>>()

    fun getAllSurahs(): Flow<List<Surah>> = flow {
        emit(AuthoritativeContentProvider.allSurahs)
    }

    fun getAllJuz(): List<JuzInfo> {
        return AuthoritativeContentProvider.allJuz
    }

    fun getAyahsForSurah(surahNumber: Int, langCode: String = "ur"): Flow<List<Ayah>> = flow {
        val cacheKey = "${surahNumber}_$langCode"
        // 1. Emit from memory cache if available
        surahAyahsCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) {
                emit(cached)
                return@flow
            }
        }

        // 2. Emit built-in complete offline Quran data immediately (All 114 Surahs built-in with Urdu WbW!)
        val builtInAyahs = OfflineQuranProvider.getAyahsForSurah(context, surahNumber)
        if (langCode == "ur" || langCode == "ar" || langCode == "ara") {
            if (builtInAyahs.isNotEmpty()) {
                surahAyahsCache[cacheKey] = builtInAyahs
                emit(builtInAyahs)
                return@flow
            }
        }

        // 3. For other languages: Check if downloaded locally via translationManager
        val translationManager = context?.let { com.example.data.quran.QuranTranslationManager.getInstance(it) }
        val activeEdition = translationManager?.supportedLanguages?.firstOrNull { it.languageCode == langCode }
        val editionId = activeEdition?.id ?: "en.sahih"
        val downloadedWbw = translationManager?.getWbwWordsForSurah(surahNumber, editionId)
        if (downloadedWbw != null && downloadedWbw.isNotEmpty()) {
            val mappedAyahs = builtInAyahs.map { ayah ->
                val wbwForAyah = downloadedWbw[ayah.numberInSurah] ?: ayah.words
                val transText = translationManager?.getTranslationForAyah(surahNumber, ayah.numberInSurah)
                    ?: if (langCode == "en") ayah.textTranslation else ayah.urduTranslation
                ayah.copy(
                    textTranslation = transText,
                    words = wbwForAyah
                )
            }
            if (mappedAyahs.isNotEmpty()) {
                surahAyahsCache[cacheKey] = mappedAyahs
                emit(mappedAyahs)
                return@flow
            }
        }

        // Emit builtInAyahs as initial baseline so user never sees an empty screen while fetching
        if (builtInAyahs.isNotEmpty()) {
            emit(builtInAyahs)
        }

        // 4. Authoritative online source: Quran.com API with Word-by-Word in selected language
        try {
            val qdcResponse = withContext(Dispatchers.IO) {
                NetworkModule.quranDotComApi.getVersesByChapter(
                    chapterId = surahNumber,
                    language = langCode,
                    words = true,
                    translations = "234,131,20,85,167",
                    wordFields = "text_uthmani,translation",
                    perPage = 300
                )
            }

            val apiVerses = qdcResponse.verses
            if (!apiVerses.isNullOrEmpty()) {
                val mappedAyahs = apiVerses.map { verseDto ->
                    val wordsList = verseDto.words?.mapNotNull { wDto ->
                        val arabicWord = wDto.textUthmani?.takeIf { it.isNotBlank() }
                            ?: wDto.text?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        val apiMeaning = wDto.translation?.text?.trim() ?: ""
                        val glossaryPair = QuranWordGlossary.lookup(arabicWord)
                        val urduWordMeaning = if (langCode == "ur") apiMeaning.ifBlank { glossaryPair?.first ?: "" } else glossaryPair?.first ?: ""
                        val englishWordMeaning = if (langCode == "en") apiMeaning.ifBlank { glossaryPair?.second ?: "" } else glossaryPair?.second ?: ""
                        val activeLanguageMeaning = apiMeaning.ifBlank {
                            if (langCode == "ur") urduWordMeaning else englishWordMeaning
                        }
                        val isEnd = wDto.charTypeName.equals("end", ignoreCase = true)
                        QuranWord(
                            id = wDto.id,
                            position = wDto.position,
                            textUthmani = arabicWord,
                            urduTranslation = urduWordMeaning,
                            englishTranslation = englishWordMeaning,
                            activeTranslation = activeLanguageMeaning,
                            isVerseEndMarker = isEnd
                        )
                    }.orEmpty()

                    val urduFullText = verseDto.translations?.firstOrNull { it.resourceId == 234 }?.text
                        ?.replace(Regex("<[^>]*>"), "")?.trim()
                        ?: verseDto.translations?.firstOrNull()?.text?.replace(Regex("<[^>]*>"), "")?.trim()
                        ?: ""

                    val englishFullText = verseDto.translations?.firstOrNull { it.resourceId != 234 }?.text
                        ?.replace(Regex("<[^>]*>"), "")?.trim()
                        ?: builtInAyahs.firstOrNull { it.numberInSurah == verseDto.verseNumber }?.textTranslation
                        ?: ""

                    val arabicAyahText = wordsList.filter { !it.isVerseEndMarker }
                        .joinToString(" ") { it.textUthmani }
                        .ifBlank { "آیت ${verseDto.verseNumber}" }

                    Ayah(
                        number = verseDto.id.toInt(),
                        surahNumber = surahNumber,
                        numberInSurah = verseDto.verseNumber,
                        textArabic = arabicAyahText,
                        textTranslation = englishFullText,
                        urduTranslation = urduFullText,
                        audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/${verseDto.id}.mp3",
                        words = wordsList
                    )
                }

                if (mappedAyahs.isNotEmpty()) {
                    surahAyahsCache[cacheKey] = mappedAyahs
                    emit(mappedAyahs)
                    return@flow
                }
            }
        } catch (_: Exception) {
            // Quran.com failed or timeout -> fallback to secondary API
        }

        // 4. Secondary fallback: AlQuran Cloud API
        try {
            val response = withContext(Dispatchers.IO) {
                NetworkModule.alQuranApi.getSurahWithTranslations(surahNumber)
            }
            val editions = response.data
            if (!editions.isNullOrEmpty()) {
                val arabicEdition = editions.firstOrNull { it.name?.contains("uthmani", ignoreCase = true) == true }
                    ?: editions[0]
                val urduEdition = editions.firstOrNull { it.englishName?.contains("Jalandhry", ignoreCase = true) == true || it.name?.contains("جالندہری") == true }
                    ?: editions.getOrNull(1)
                val englishEdition = editions.firstOrNull { it.englishName?.contains("Saheeh", ignoreCase = true) == true }
                    ?: editions.getOrNull(2)

                val arabicAyahs = arabicEdition.ayahs.orEmpty()
                val urduAyahs = urduEdition?.ayahs.orEmpty()
                val englishAyahs = englishEdition?.ayahs.orEmpty()

                if (arabicAyahs.isNotEmpty()) {
                    val fullAyahs = arabicAyahs.mapIndexed { index, arAyah ->
                        Ayah(
                            number = arAyah.number,
                            surahNumber = surahNumber,
                            numberInSurah = arAyah.numberInSurah,
                            textArabic = arAyah.text,
                            textTranslation = englishAyahs.getOrNull(index)?.text
                                ?: "Verse ${arAyah.numberInSurah}",
                            urduTranslation = urduAyahs.getOrNull(index)?.text
                                ?: "آیت نمبر ${arAyah.numberInSurah}",
                            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/${arAyah.number}.mp3"
                        )
                    }
                    surahAyahsCache[cacheKey] = fullAyahs
                    emit(fullAyahs)
                }
            }
        } catch (_: Exception) {
            // If network failed and built-in was empty, fallback
            if (builtInAyahs.isEmpty()) {
                val fallbackList = OfflineQuranProvider.getAyahsForSurah(context, surahNumber)
                if (fallbackList.isNotEmpty()) {
                    emit(fallbackList)
                }
            }
        }
    }

    fun searchQuran(query: String): List<Surah> {
        if (query.isBlank()) return AuthoritativeContentProvider.allSurahs
        val trimmed = query.trim()
        val queryNumber = trimmed.toIntOrNull()

        return AuthoritativeContentProvider.allSurahs.filter { surah ->
            // Match Surah number
            if (queryNumber != null && surah.number == queryNumber) return@filter true

            // Match English name or English translation
            if (surah.englishName.contains(trimmed, ignoreCase = true) ||
                surah.englishTranslation.contains(trimmed, ignoreCase = true)) return@filter true

            // Match Urdu name
            if (ArabicNormalizationUtils.containsNormalized(surah.urduName, trimmed)) return@filter true

            // Match Arabic name normalized (diacritic-insensitive)
            if (ArabicNormalizationUtils.containsNormalized(surah.arabicName, trimmed)) return@filter true

            false
        }
    }

    /**
     * Searches Quran Ayahs strictly from local device storage (built-in 6,236 Ayahs).
     * Supports search by Ayah (e.g. "2:255", "Ayah 255", Surah name + number)
     * or by Word (in Arabic, English, Urdu, Hindi, Bengali, or Roman transliteration).
     */
    fun searchAyahsOffline(
        context: Context?,
        query: String,
        searchByAyah: Boolean,
        langCode: String, // "en", "ur", "hi", "bn", etc.
        translationManager: com.example.data.quran.QuranTranslationManager? = null
    ): List<Pair<Surah, Ayah>> {
        if (query.isBlank()) return emptyList()
        val trimmed = query.trim()
        val allSurahMap = AuthoritativeContentProvider.allSurahs.associateBy { it.number }
        val results = mutableListOf<Pair<Surah, Ayah>>()

        val allAyahsBySurah = OfflineQuranProvider.getAllSurahAyahs(context)

        // 1. By Ayah search flow (explicitly chosen or entered formatted reference like 2:255 or pure number)
        val colonRegex = Regex("^(\\d{1,3})\\s*[:\\s-]\\s*(\\d{1,3})$")
        val colonMatch = colonRegex.find(trimmed)
        if (colonMatch != null) {
            val sNum = colonMatch.groupValues[1].toIntOrNull() ?: 1
            val aNum = colonMatch.groupValues[2].toIntOrNull() ?: 1
            val surah = allSurahMap[sNum]
            val ayahs = allAyahsBySurah[sNum] ?: OfflineQuranProvider.getAyahsForSurah(context, sNum)
            val targetAyah = ayahs.firstOrNull { it.numberInSurah == aNum }
            if (surah != null && targetAyah != null) {
                val activeText = resolveTranslation(targetAyah, langCode, translationManager)
                results.add(Pair(surah, targetAyah.copy(textTranslation = activeText)))
                return results
            }
        }

        if (searchByAyah) {
            // Check if user entered a number like "255" or "5"
            val pureNumber = trimmed.toIntOrNull()
            if (pureNumber != null) {
                for (surah in AuthoritativeContentProvider.allSurahs) {
                    val ayahs = allAyahsBySurah[surah.number] ?: OfflineQuranProvider.getAyahsForSurah(context, surah.number)
                    val matching = ayahs.filter { it.numberInSurah == pureNumber }
                    for (a in matching) {
                        val activeText = resolveTranslation(a, langCode, translationManager)
                        results.add(Pair(surah, a.copy(textTranslation = activeText)))
                        if (results.size >= 114) break
                    }
                }
                if (results.isNotEmpty()) return results
            }

            // Check if query has Surah Name + Number (e.g. "Baqarah 255" or "Yasin 1")
            for (surah in AuthoritativeContentProvider.allSurahs) {
                if (trimmed.contains(surah.englishName, ignoreCase = true) ||
                    trimmed.contains(surah.urduName) ||
                    ArabicNormalizationUtils.containsNormalized(surah.arabicName, trimmed)) {
                    val numInQuery = Regex("\\d+").find(trimmed)?.value?.toIntOrNull()
                    val ayahs = allAyahsBySurah[surah.number] ?: OfflineQuranProvider.getAyahsForSurah(context, surah.number)
                    if (numInQuery != null) {
                        val matching = ayahs.filter { it.numberInSurah == numInQuery }
                        for (a in matching) {
                            val activeText = resolveTranslation(a, langCode, translationManager)
                            results.add(Pair(surah, a.copy(textTranslation = activeText)))
                        }
                    } else {
                        // Return all ayahs of this surah
                        for (a in ayahs) {
                            val activeText = resolveTranslation(a, langCode, translationManager)
                            results.add(Pair(surah, a.copy(textTranslation = activeText)))
                        }
                    }
                    if (results.isNotEmpty()) return results
                }
            }
        }

        // 2. Comprehensive Word Search Flow - Returns ALL matching Ayahs across the entire Quran with no artificial limits
        val expandedQueries = com.example.domain.util.IslamicSearchKeywords.getExpandedQueries(trimmed)

        for (surah in AuthoritativeContentProvider.allSurahs) {
            val ayahs = allAyahsBySurah[surah.number] ?: OfflineQuranProvider.getAyahsForSurah(context, surah.number)
            for (ayah in ayahs) {
                val activeText = resolveTranslation(ayah, langCode, translationManager)
                var isMatch = false

                for (q in expandedQueries) {
                    val isScript = q.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
                    if (isScript) {
                        if (ArabicNormalizationUtils.containsNormalized(ayah.textArabic, q) ||
                            ArabicNormalizationUtils.containsNormalized(ayah.urduTranslation, q) ||
                            ayah.urduTranslation.contains(q, ignoreCase = true) ||
                            (activeText.isNotBlank() && (ArabicNormalizationUtils.containsNormalized(activeText, q) || activeText.contains(q, ignoreCase = true)))
                        ) {
                            isMatch = true
                            break
                        }
                    } else {
                        // Latin / English query
                        if (ayah.textTranslation.contains(q, ignoreCase = true) ||
                            ayah.urduTranslation.contains(q, ignoreCase = true) ||
                            activeText.contains(q, ignoreCase = true) ||
                            ArabicNormalizationUtils.containsNormalized(ayah.textArabic, q)
                        ) {
                            isMatch = true
                            break
                        }
                    }
                }

                if (isMatch) {
                    results.add(Pair(surah, ayah.copy(textTranslation = activeText)))
                }
            }
        }

        return results
    }

    private fun resolveTranslation(
        ayah: Ayah,
        langCode: String,
        translationManager: com.example.data.quran.QuranTranslationManager?
    ): String {
        val code = langCode.lowercase()
        if (code == "ar" || code == "ara") return ayah.textArabic
        if (code == "ur" || code == "urd") return ayah.urduTranslation.ifBlank { ayah.textTranslation }
        if (code == "en" || code == "eng") return ayah.textTranslation.ifBlank { ayah.urduTranslation }

        // Attempt downloaded translation from manager
        val downloaded = translationManager?.getTranslationForAyah(ayah.surahNumber, ayah.numberInSurah)
        if (!downloaded.isNullOrBlank()) return downloaded

        return ayah.urduTranslation.ifBlank { ayah.textTranslation }
    }
}
