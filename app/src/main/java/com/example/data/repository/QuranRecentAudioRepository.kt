package com.example.data.repository

import android.content.Context
import com.example.domain.model.RecentAudioSurah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class QuranRecentAudioRepository(context: Context) {
    private val prefs = context.getSharedPreferences("quran_recent_audio_prefs", Context.MODE_PRIVATE)

    private val _recentAudioSurahs = MutableStateFlow<List<RecentAudioSurah>>(emptyList())
    val recentAudioSurahs: StateFlow<List<RecentAudioSurah>> = _recentAudioSurahs.asStateFlow()

    init {
        loadRecentSurahs()
    }

    private fun loadRecentSurahs() {
        val jsonStr = prefs.getString("recent_audio_list", null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RecentAudioSurah>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentAudioSurah(
                        surahNumber = obj.getInt("surahNumber"),
                        surahEnglishName = obj.optString("surahEnglishName", ""),
                        surahArabicName = obj.optString("surahArabicName", ""),
                        surahUrduName = obj.optString("surahUrduName", ""),
                        reciterId = obj.optString("reciterId", "ar.alafasy"),
                        reciterName = obj.optString("reciterName", "Mishary Rashid Alafasy"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        lastPositionMs = obj.optLong("lastPositionMs", 0L),
                        durationMs = obj.optLong("durationMs", 0L)
                    )
                )
            }
            _recentAudioSurahs.value = list
        } catch (_: Exception) {}
    }

    fun recordRecentPlay(
        surahNumber: Int,
        surahEnglishName: String,
        surahArabicName: String,
        surahUrduName: String,
        reciterId: String,
        reciterName: String,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        val currentList = _recentAudioSurahs.value.toMutableList()
        // Remove existing item for this surah if present so we can bump to top
        currentList.removeAll { it.surahNumber == surahNumber && it.reciterId == reciterId }

        val newEntry = RecentAudioSurah(
            surahNumber = surahNumber,
            surahEnglishName = surahEnglishName,
            surahArabicName = surahArabicName,
            surahUrduName = surahUrduName,
            reciterId = reciterId,
            reciterName = reciterName,
            timestamp = System.currentTimeMillis(),
            lastPositionMs = positionMs,
            durationMs = durationMs
        )
        currentList.add(0, newEntry)
        // Keep max 20 recent
        val trimmed = currentList.take(20)
        _recentAudioSurahs.value = trimmed
        saveToPrefs(trimmed)
    }

    fun updateProgress(surahNumber: Int, reciterId: String, positionMs: Long, durationMs: Long) {
        val currentList = _recentAudioSurahs.value.toMutableList()
        val index = currentList.indexOfFirst { it.surahNumber == surahNumber && it.reciterId == reciterId }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(
                lastPositionMs = positionMs,
                durationMs = durationMs,
                timestamp = System.currentTimeMillis()
            )
            _recentAudioSurahs.value = currentList
            saveToPrefs(currentList)
        }
    }

    fun clearRecentSurahs() {
        _recentAudioSurahs.value = emptyList()
        prefs.edit().remove("recent_audio_list").apply()
    }

    fun getLastPlayedSurah(): RecentAudioSurah? {
        return _recentAudioSurahs.value.firstOrNull()
    }

    private fun saveToPrefs(list: List<RecentAudioSurah>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("surahNumber", item.surahNumber)
                    put("surahEnglishName", item.surahEnglishName)
                    put("surahArabicName", item.surahArabicName)
                    put("surahUrduName", item.surahUrduName)
                    put("reciterId", item.reciterId)
                    put("reciterName", item.reciterName)
                    put("timestamp", item.timestamp)
                    put("lastPositionMs", item.lastPositionMs)
                    put("durationMs", item.durationMs)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("recent_audio_list", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }
}
