package com.example.widget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TasbeehDhikr(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val urduMeaning: String,
    val defaultTarget: Int = 33,
    val isCustom: Boolean = false
)

/**
 * Shared repository for Tasbeeh data, guaranteeing that the home screen widget,
 * in-app Tasbeeh screen, and any background updates stay 100% in sync using the
 * same SharedPreferences file ("tasbeeh_data") and schema.
 */
object TasbeehSharedRepository {

    const val PREFS_NAME = "tasbeeh_data"

    // Default authentic dhikr list matching TasbeehScreen exactly
    val defaultPhrases = listOf(
        TasbeehDhikr("subhanallah", "سُبْحَانَ اللَّٰهِ", "SubhanAllah", "اللہ پاک ہے", 33),
        TasbeehDhikr("alhamdulillah", "الْحَمْدُ لِلَّٰهِ", "Alhamdulillah", "تمام تعریفیں اللہ کے لیے ہیں", 33),
        TasbeehDhikr("allahuakbar", "اللَّٰهُ أَكْبَرُ", "Allahu Akbar", "اللہ سب سے بڑا ہے", 34),
        TasbeehDhikr("astaghfirullah", "أَسْتَغْفِرُ اللَّٰهَ", "Astaghfirullah", "میں اللہ سے بخشش مانگتا ہوں", 100),
        TasbeehDhikr("lailahaillallah", "لَا إِلَٰهَ إِلَّا اللَّٰهُ", "La ilaha illallah", "اللہ کے سوا کوئی معبود نہیں", 100),
        TasbeehDhikr("durood", "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Durood Shareef", "اے اللہ! محمد ﷺ پر رحمت بھیج", 100),
        TasbeehDhikr("hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّٰهِ", "La Hawla Wa La Quwwata", "اللہ کے سوا کوئی طاقت و قوت نہیں", 33),
        TasbeehDhikr("subhanallahi_bihamdihi", "سُبْحَانَ اللَّٰهِ وَبِحَمْدِهِ", "SubhanAllahi Wa Bihamdihi", "اللہ پاک ہے اپنی حمد کے ساتھ", 100)
    )

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns all available phrases including any custom phrases created by the user in-app.
     */
    fun getAllPhrases(context: Context): List<TasbeehDhikr> {
        val prefs = getPrefs(context)
        val customJson = prefs.getString("custom_phrases", "[]") ?: "[]"
        val customList = mutableListOf<TasbeehDhikr>()
        try {
            val array = JSONArray(customJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                customList.add(
                    TasbeehDhikr(
                        id = obj.getString("id"),
                        arabic = obj.getString("arabic"),
                        transliteration = obj.getString("transliteration"),
                        urduMeaning = obj.optString("urduMeaning", ""),
                        defaultTarget = obj.optInt("defaultTarget", 33),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}
        return defaultPhrases + customList
    }

    /**
     * Gets the configured phrase for a specific widget instance or falls back to the globally selected phrase.
     */
    fun getWidgetPhrase(context: Context, appWidgetId: Int): TasbeehDhikr {
        val prefs = getPrefs(context)
        val all = getAllPhrases(context)

        val widgetKey = "widget_${appWidgetId}_phrase_id"
        val assignedId = prefs.getString(widgetKey, null)
            ?: prefs.getString("active_phrase_id", "subhanallah")
            ?: "subhanallah"

        return all.firstOrNull { it.id == assignedId } ?: defaultPhrases.first()
    }

    fun setWidgetPhrase(context: Context, appWidgetId: Int, phraseId: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString("widget_${appWidgetId}_phrase_id", phraseId).apply()
    }

    /**
     * Gets the current count for a phrase.
     */
    fun getCount(context: Context, phraseId: String): Int {
        return getPrefs(context).getInt("current_count_$phraseId", 0)
    }

    /**
     * Gets the target limit for a phrase (either per-widget override or phrase default).
     */
    fun getTarget(context: Context, appWidgetId: Int, phrase: TasbeehDhikr): Int {
        val prefs = getPrefs(context)
        val widgetTargetKey = "widget_${appWidgetId}_target"
        if (prefs.contains(widgetTargetKey)) {
            val t = prefs.getInt(widgetTargetKey, phrase.defaultTarget)
            if (t > 0) return t
        }
        return prefs.getInt("target_limit_${phrase.id}", phrase.defaultTarget)
    }

    fun setWidgetTarget(context: Context, appWidgetId: Int, target: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt("widget_${appWidgetId}_target", target).apply()
    }

    /**
     * Increments the count for a phrase, records history on completed cycle, and returns the new count.
     */
    @Synchronized
    fun incrementCount(context: Context, appWidgetId: Int): Int {
        val prefs = getPrefs(context)
        val phrase = getWidgetPhrase(context, appWidgetId)
        val current = prefs.getInt("current_count_${phrase.id}", 0)
        val newCount = current + 1
        val target = getTarget(context, appWidgetId, phrase)

        val editor = prefs.edit()
        editor.putInt("current_count_${phrase.id}", newCount)

        // If target reached, record to history
        if (target > 0 && newCount % target == 0) {
            recordHistory(prefs, phrase, target)
        }

        // Keep active phrase updated for seamless in-app alignment
        editor.putString("active_phrase_id", phrase.id)
        editor.apply()

        return newCount
    }

    /**
     * Resets count for a phrase back to 0, saving current count to history.
     */
    @Synchronized
    fun resetCount(context: Context, appWidgetId: Int): Int {
        val prefs = getPrefs(context)
        val phrase = getWidgetPhrase(context, appWidgetId)
        val current = prefs.getInt("current_count_${phrase.id}", 0)

        if (current > 0) {
            recordHistory(prefs, phrase, current)
        }

        prefs.edit()
            .putInt("current_count_${phrase.id}", 0)
            .apply()

        return 0
    }

    fun getCumulativeCount(context: Context, phraseId: String): Int {
        return getPrefs(context).getInt("total_count_$phraseId", 0)
    }

    private fun recordHistory(prefs: SharedPreferences, phrase: TasbeehDhikr, countToSave: Int) {
        if (countToSave <= 0) return
        try {
            val historyJson = prefs.getString("history_records", "[]") ?: "[]"
            val array = JSONArray(historyJson)
            val obj = JSONObject()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now))
            obj.put("phraseId", phrase.id)
            obj.put("phraseName", phrase.transliteration)
            obj.put("count", countToSave)
            obj.put("timestamp", now)
            obj.put("formattedDate", dateStr)
            array.put(obj)

            val totalKey = "total_count_${phrase.id}"
            val existingTotal = prefs.getInt(totalKey, 0)

            prefs.edit()
                .putString("history_records", array.toString())
                .putInt(totalKey, existingTotal + countToSave)
                .apply()
        } catch (_: Exception) {}
    }
}
