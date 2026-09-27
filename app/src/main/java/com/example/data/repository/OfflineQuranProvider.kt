package com.example.data.repository

import android.content.Context
import com.example.domain.model.Ayah
import com.example.domain.model.QuranWord
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

/**
 * Built-in complete Quran provider.
 * Loads and caches all 114 Surahs and 6,236 Ayahs from quran_complete.json.gz
 * so users have 100% offline access to the complete Holy Quran without downloading.
 */
object OfflineQuranProvider {
    private val surahsCache = ConcurrentHashMap<Int, List<Ayah>>()
    private val allAyahsList = ArrayList<Ayah>(6236)
    @Volatile
    private var isLoaded = false
    private val lock = Any()

    fun preload(context: Context) {
        if (isLoaded) return
        ensureLoaded(context)
    }

    fun getRandomAyah(context: Context?, excludeGlobalNumber: Int? = null): Ayah {
        if (context != null && !isLoaded) {
            ensureLoaded(context)
        }
        if (allAyahsList.isNotEmpty()) {
            val count = allAyahsList.size
            var picked = allAyahsList.random()
            if (excludeGlobalNumber != null && count > 1 && picked.number == excludeGlobalNumber) {
                picked = allAyahsList.random()
            }
            return picked
        }
        return AuthoritativeContentProvider.getAyahsForSurah(1).firstOrNull() ?: Ayah(
            number = 1,
            surahNumber = 1,
            numberInSurah = 1,
            textArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            textTranslation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
            urduTranslation = "شروع اللہ کے نام سے جو بڑا مہربان نہایت رحم والا ہے۔",
            audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3"
        )
    }

    fun getAyahsForSurah(context: Context?, surahNumber: Int): List<Ayah> {
        val cached = surahsCache[surahNumber]
        if (cached != null && cached.isNotEmpty()) {
            if (cached.first().words.isNotEmpty()) return cached
            val wordsMap = BuiltInWbwProvider.getWordsForSurah(context, surahNumber)
            if (wordsMap.isNotEmpty()) {
                val withWords = cached.map { a ->
                    val w = wordsMap[a.numberInSurah].orEmpty()
                    if (w.isNotEmpty()) a.copy(words = w) else a
                }
                surahsCache[surahNumber] = withWords
                return withWords
            }
            return cached
        }
        if (context != null) {
            ensureLoaded(context)
            val newlyCached = surahsCache[surahNumber]
            if (newlyCached != null && newlyCached.isNotEmpty()) {
                val wordsMap = BuiltInWbwProvider.getWordsForSurah(context, surahNumber)
                if (wordsMap.isNotEmpty()) {
                    val withWords = newlyCached.map { a ->
                        val w = wordsMap[a.numberInSurah].orEmpty()
                        if (w.isNotEmpty()) a.copy(words = w) else a
                    }
                    surahsCache[surahNumber] = withWords
                    return withWords
                }
                return newlyCached
            }
        }
        return AuthoritativeContentProvider.getAyahsForSurah(surahNumber)
    }

    fun getAllSurahAyahs(context: Context?): Map<Int, List<Ayah>> {
        if (context != null) {
            ensureLoaded(context)
        }
        return surahsCache
    }

    private fun ensureLoaded(context: Context) {
        if (isLoaded) return
        synchronized(lock) {
            if (isLoaded) return
            try {
                val inputStream = try {
                    GZIPInputStream(context.assets.open("quran_complete.json.gz"))
                } catch (_: Throwable) {
                    try {
                        context.assets.open("quran_complete.json")
                    } catch (_: Throwable) {
                        GZIPInputStream(context.assets.open("quran_complete.gz"))
                    }
                }
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 65536).use { reader ->
                    val jsonReader = android.util.JsonReader(reader)
                    val tempAllAyahs = ArrayList<Ayah>(6236)
                    
                    jsonReader.beginObject()
                    while (jsonReader.hasNext()) {
                        if (jsonReader.nextName() == "surahs") {
                            jsonReader.beginArray()
                            while (jsonReader.hasNext()) {
                                jsonReader.beginObject()
                                var sNum = 1
                                val ayahsList = ArrayList<Ayah>()
                                while (jsonReader.hasNext()) {
                                    val sName = jsonReader.nextName()
                                    if (sName == "number") {
                                        sNum = jsonReader.nextInt()
                                    } else if (sName == "ayahs") {
                                        jsonReader.beginArray()
                                        while (jsonReader.hasNext()) {
                                            jsonReader.beginObject()
                                            var globalNum = 1
                                            var inSurah = 1
                                            var ar = ""
                                            var ur = ""
                                            var en = ""
                                            val parsedWords = ArrayList<QuranWord>()
                                            while (jsonReader.hasNext()) {
                                                when (jsonReader.nextName()) {
                                                    "num" -> globalNum = jsonReader.nextInt()
                                                    "inSurah" -> inSurah = jsonReader.nextInt()
                                                    "ar" -> ar = jsonReader.nextString().replace("\uFEFF", "").trim()
                                                    "ur" -> ur = jsonReader.nextString()
                                                    "en" -> en = jsonReader.nextString()
                                                    "w" -> {
                                                        jsonReader.beginArray()
                                                        var pos = 1
                                                        while (jsonReader.hasNext()) {
                                                            jsonReader.beginArray()
                                                            val wAr = if (jsonReader.hasNext()) jsonReader.nextString() else ""
                                                            val wUr = if (jsonReader.hasNext()) jsonReader.nextString() else ""
                                                            val wEnd = if (jsonReader.hasNext()) (jsonReader.nextInt() == 1) else false
                                                            while (jsonReader.hasNext()) jsonReader.skipValue()
                                                            jsonReader.endArray()
                                                            parsedWords.add(
                                                                QuranWord(
                                                                    id = (globalNum * 100L + pos),
                                                                    position = pos,
                                                                    textUthmani = wAr,
                                                                    urduTranslation = wUr,
                                                                    englishTranslation = "",
                                                                    activeTranslation = wUr,
                                                                    isVerseEndMarker = wEnd
                                                                )
                                                            )
                                                            pos++
                                                        }
                                                        jsonReader.endArray()
                                                    }
                                                    else -> jsonReader.skipValue()
                                                }
                                            }
                                            jsonReader.endObject()
                                            
                                            val words = if (parsedWords.isNotEmpty()) parsedWords else BuiltInWbwProvider.getWordsForAyah(context, sNum, inSurah)
                                            val ayah = Ayah(
                                                number = globalNum,
                                                surahNumber = sNum,
                                                numberInSurah = inSurah,
                                                textArabic = ar,
                                                textTranslation = en,
                                                urduTranslation = ur,
                                                audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalNum.mp3",
                                                words = words
                                            )
                                            ayahsList.add(ayah)
                                            tempAllAyahs.add(ayah)
                                        }
                                        jsonReader.endArray()
                                    } else {
                                        jsonReader.skipValue()
                                    }
                                }
                                jsonReader.endObject()
                                surahsCache[sNum] = ayahsList
                            }
                            jsonReader.endArray()
                        } else {
                            jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()

                    synchronized(allAyahsList) {
                        allAyahsList.clear()
                        allAyahsList.addAll(tempAllAyahs)
                    }
                    isLoaded = true
                }
            } catch (e: Throwable) {
                android.util.Log.e("OfflineQuranProvider", "Error parsing quran_complete.json.gz", e)
            }
        }
    }
}
