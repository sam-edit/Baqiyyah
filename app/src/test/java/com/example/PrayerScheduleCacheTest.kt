package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PrayerScheduleCache
import com.example.domain.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrayerScheduleCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Asia/Karachi")
    private val city = UserLocation("Daska", "Pakistan", 32.336, 74.353, true)
    private val date = LocalDate.now(zone)
    private val prefs get() = context.getSharedPreferences("prayer_schedule_cache_v1", Context.MODE_PRIVATE)
    @Before fun clear() { prefs.edit().clear().commit() }

    private fun midnightOf(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun sameDayReusesCacheUntilCityMidnightThenRefreshes() {
        val first = PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date, 1000)
        val second = PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date, 2000)
        assertEquals(first, second)
        // Cache hit: nothing is rewritten.
        assertEquals(1000L, prefs.getLong("updated:$date", 0))
        // Just before the city's midnight it is still today's entry.
        val almostMidnight = midnightOf(date.plusDays(1)) - 1
        PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date, almostMidnight)
        assertEquals(1000L, prefs.getLong("updated:$date", 0))
        // After midnight the new date is calculated and yesterday's entry is pruned.
        val tomorrow = date.plusDays(1)
        val next = PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, tomorrow, midnightOf(tomorrow))
        assertEquals(tomorrow, next.date)
        assertFalse(prefs.contains(date.toString()))
        assertTrue(prefs.contains(tomorrow.toString()))
    }

    @Test fun midnightIsDecidedByTheCitysOwnZone() {
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val tokyo = UserLocation("Tokyo", "Japan", 35.6762, 139.6503, false)
        val tokyoToday = LocalDate.now(tokyoZone)
        PrayerScheduleCache.get(context, tokyo, CalculationMethod.KARACHI, JuristicSchool.HANAFI, tokyoZone, tokyoToday, 1000)
        val tokyoTomorrow = tokyoToday.plusDays(1)
        val atTokyoMidnight = tokyoTomorrow.atStartOfDay(tokyoZone).toInstant().toEpochMilli()
        PrayerScheduleCache.get(context, tokyo, CalculationMethod.KARACHI, JuristicSchool.HANAFI, tokyoZone, tokyoTomorrow, atTokyoMidnight)
        assertFalse(prefs.contains(tokyoToday.toString()))
    }

    @Test fun clearRemovesEverything() {
        PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date)
        PrayerScheduleCache.clear(context)
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun cityAndSchoolChangesInvalidateCacheImmediately() {
        PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date)
        val identity = prefs.getString("identity", null)
        val karachi = city.copy(cityName = "Karachi", latitude = 24.8607, longitude = 67.0011)
        val hanafi = PrayerScheduleCache.get(context, karachi, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date)
        assertNotEquals(identity, prefs.getString("identity", null))
        // Only the new city's entry remains for the date.
        assertEquals(setOf("identity", date.toString(), "updated:$date"), prefs.all.keys)
        val standard = PrayerScheduleCache.get(context, karachi, CalculationMethod.KARACHI, JuristicSchool.STANDARD, zone, date)
        assertTrue(hanafi.asr.isAfter(standard.asr))
    }

    @Test fun temporaryPreviewDoesNotOverwriteSavedLocation() {
        PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date)
        val before = prefs.all
        PrayerScheduleCache.get(context, city.copy(latitude = 24.86), CalculationMethod.KARACHI,
            JuristicSchool.HANAFI, zone, date, persist = false)
        assertEquals(before, prefs.all)
    }

    @Test fun corruptCacheRecoversAndNewDateGetsNewSchedule() {
        PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date)
        prefs.edit().putString(date.toString(), "broken").commit()
        assertEquals(date, PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, date).date)
        val tomorrow = date.plusDays(1)
        assertEquals(tomorrow, PrayerScheduleCache.get(context, city, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone, tomorrow).date)
    }
}
