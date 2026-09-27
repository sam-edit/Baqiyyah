package com.example.data.repository

import android.content.Context
import com.example.domain.model.QuranWord
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

object BuiltInWbwProvider {
    private val surahWordsCache = ConcurrentHashMap<Int, Map<Int, List<QuranWord>>>()
    @Volatile
    private var allData: JSONObject? = null
    private val lock = Any()

    private fun ensureDataLoaded(context: Context?) {
        if (allData != null || context == null) return
        synchronized(lock) {
            if (allData != null) return
            try {
                context.assets.open("quran_wbw_ur.json.gz").use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        val reader = BufferedReader(InputStreamReader(gzipStream, Charsets.UTF_8))
                        val text = reader.readText()
                        allData = JSONObject(text)
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("BuiltInWbwProvider", "Failed to load quran_wbw_ur.json.gz", e)
            }
        }
    }

    fun getWordsForSurah(context: Context?, surahNumber: Int): Map<Int, List<QuranWord>> {
        surahWordsCache[surahNumber]?.let { return it }
        ensureDataLoaded(context)
        val data = allData ?: return emptyMap()

        val surahObj = data.optJSONObject(surahNumber.toString()) ?: return emptyMap()
        val result = mutableMapOf<Int, List<QuranWord>>()

        val keys = surahObj.keys()
        while (keys.hasNext()) {
            val ayahKey = keys.next()
            val ayahNum = ayahKey.toIntOrNull() ?: continue
            val wordsArray = surahObj.optJSONArray(ayahKey) ?: continue
            val wordsList = mutableListOf<QuranWord>()
            for (i in 0 until wordsArray.length()) {
                val wObj = wordsArray.optJSONObject(i) ?: continue
                val id = wObj.optLong("id", 0L)
                val pos = wObj.optInt("pos", i + 1)
                val ar = wObj.optString("ar", "")
                val ur = wObj.optString("ur", "")
                val en = wObj.optString("en", "")
                val isEnd = wObj.optBoolean("end", false)
                wordsList.add(
                    QuranWord(
                        id = id,
                        position = pos,
                        textUthmani = ar,
                        urduTranslation = ur,
                        englishTranslation = en,
                        activeTranslation = ur,
                        isVerseEndMarker = isEnd
                    )
                )
            }
            result[ayahNum] = wordsList
        }

        surahWordsCache[surahNumber] = result
        return result
    }

    fun getWordsForAyah(context: Context?, surahNumber: Int, ayahNumberInSurah: Int): List<QuranWord> {
        val surahMap = getWordsForSurah(context, surahNumber)
        return surahMap[ayahNumberInSurah] ?: emptyList()
    }
}
