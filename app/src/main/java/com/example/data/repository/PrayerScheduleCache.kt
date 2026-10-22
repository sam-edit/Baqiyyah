package com.example.data.repository

import android.content.Context
import com.example.domain.model.*
import com.example.domain.prayer.PrayerCalculationEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.json.JSONObject

/**
 * Single-slot cache for the BACKGROUND location (primary city, or last GPS-detected city).
 * Shared by alarms, the foreground screen and widgets. No network is required.
 *
 * Rules:
 *  - An entry is valid for one calendar date in that city's own time zone, i.e. it lives until
 *    the city's local 12:00 AM and the next date is computed fresh.
 *  - The slot belongs to exactly one identity (lat|lon|zone|method|school). Any change of city,
 *    zone, method or school wipes the previous city's entries.
 *  - Days before today (in that city's zone) are pruned on every write.
 *  - A manually browsed city must call get(..., persist = false): it is only calculated, never
 *    stored, and never touches the slot.
 */
object PrayerScheduleCache {
    private const val PREFS = "prayer_schedule_cache_v1"
    private const val KEY_IDENTITY = "identity"
    private const val UPDATED_PREFIX = "updated:"

    @Synchronized
    fun get(
        context: Context, location: UserLocation, method: CalculationMethod,
        school: JuristicSchool, zone: ZoneId, date: LocalDate = LocalDate.now(zone),
        nowMillis: Long = System.currentTimeMillis(), persist: Boolean = true
    ): DailyPrayerSchedule {
        if (!persist) return PrayerCalculationEngine.calculateDailySchedule(
            date, location.latitude, location.longitude, method, school, zone)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val identity = "${location.latitude}|${location.longitude}|${zone.id}|${method.id}|${school.id}"
        val key = date.toString()
        val sameIdentity = prefs.getString(KEY_IDENTITY, null) == identity
        if (sameIdentity) {
            // Valid as long as it is for the requested date of this city (date rolls at the
            // city's local midnight, so no separate age check is needed).
            runCatching { decode(JSONObject(prefs.getString(key, "")!!)) }.getOrNull()
                ?.takeIf { it.date == date }?.let { return it }
        }
        val result = PrayerCalculationEngine.calculateDailySchedule(
            date, location.latitude, location.longitude, method, school, zone
        )
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().toString()
        val edit = prefs.edit()
        if (!sameIdentity) edit.clear()
        else prefs.all.keys
            .filter { it != KEY_IDENTITY && it.removePrefix(UPDATED_PREFIX) < today }
            .forEach { edit.remove(it) }
        edit.putString(KEY_IDENTITY, identity).putString(key, encode(result).toString())
            .putLong("$UPDATED_PREFIX$key", nowMillis).commit()
        return result
    }

    /** Drops every cached day (used when the primary / background city changes or is cleared). */
    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun encode(s: DailyPrayerSchedule) = JSONObject().apply {
        put("date", s.date.toString())
        listOf("fajr" to s.fajr, "sunrise" to s.sunrise, "dhuhr" to s.dhuhr,
            "asr" to s.asr, "sunset" to s.sunset, "maghrib" to s.maghrib,
            "isha" to s.isha, "imsak" to s.imsak, "suhurEnd" to s.suhurEnd,
            "iftar" to s.iftar, "tahajjud" to s.tahajjud, "ishraq" to s.ishraq,
            "chasht" to s.chasht, "zawal" to s.zawal, "jummah" to s.jummah)
            .forEach { (key, value) -> put(key, value.toString()) }
    }

    private fun decode(j: JSONObject): DailyPrayerSchedule {
        fun time(key: String) = LocalTime.parse(j.getString(key))
        return DailyPrayerSchedule(LocalDate.parse(j.getString("date")),
            time("fajr"), time("sunrise"), time("dhuhr"), time("asr"), time("sunset"),
            time("maghrib"), time("isha"), time("imsak"), time("suhurEnd"), time("iftar"),
            time("tahajjud"), time("ishraq"), time("chasht"), time("zawal"), time("jummah"))
    }
}
