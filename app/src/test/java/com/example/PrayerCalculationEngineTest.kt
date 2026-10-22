package com.example

import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import com.example.domain.prayer.HighLatitudeRule
import com.example.domain.prayer.PrayerCalculationEngine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerCalculationEngineTest {

    private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

    private fun assertTime(label: String, expected: String, actual: LocalTime, toleranceMin: Int = 1) {
        val parts = expected.split(":")
        val expectedMin = parts[0].toInt() * 60 + parts[1].toInt()
        val diff = kotlin.math.abs(expectedMin - actual.minuteOfDay())
        assertTrue("$label expected ~$expected but was $actual", diff <= toleranceMin)
    }

    private fun assertOrdered(label: String, s: DailyPrayerSchedule) {
        val v = listOf(s.fajr, s.sunrise, s.dhuhr, s.asr, s.sunset, s.maghrib, s.isha).map { it.minuteOfDay() }
        assertTrue("$label fajr <= sunrise", v[0] <= v[1])
        assertTrue("$label sunrise < dhuhr", v[1] < v[2])
        assertTrue("$label dhuhr < asr", v[2] < v[3])
        assertTrue("$label asr <= sunset", v[3] <= v[4])
        assertTrue("$label sunset <= maghrib", v[4] <= v[5])
        assertTrue("$label maghrib <= isha", v[5] <= v[6])
    }

    @Test
    fun karachi_hanafi_knownTimes() {
        val s = PrayerCalculationEngine.calculateDailySchedule(
            date = LocalDate.of(2026, 10, 11),
            latitude = 24.8607,
            longitude = 67.0011,
            method = CalculationMethod.KARACHI,
            juristicSchool = JuristicSchool.HANAFI,
            timeZone = ZoneId.of("Asia/Karachi")
        )
        assertTime("fajr", "05:12", s.fajr)
        assertTime("sunrise", "06:28", s.sunrise)
        assertTime("dhuhr", "12:19", s.dhuhr)
        assertTime("asr", "16:31", s.asr)
        assertTime("maghrib", "18:10", s.maghrib)
        assertTime("isha", "19:26", s.isha)
        assertOrdered("karachi", s)
    }

    @Test
    fun hanafiAsr_isLaterThanStandardAsr() {
        val date = LocalDate.of(2026, 10, 11)
        val zone = ZoneId.of("Asia/Karachi")
        val hanafi = PrayerCalculationEngine.calculateDailySchedule(
            date, 24.8607, 67.0011, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone
        )
        val standard = PrayerCalculationEngine.calculateDailySchedule(
            date, 24.8607, 67.0011, CalculationMethod.KARACHI, JuristicSchool.STANDARD, zone
        )
        assertTrue(hanafi.asr.isAfter(standard.asr))
    }

    @Test
    fun makkah_ishaIsNinetyMinutesAfterMaghrib() {
        val s = PrayerCalculationEngine.calculateDailySchedule(
            date = LocalDate.of(2026, 10, 11),
            latitude = 21.4225,
            longitude = 39.8262,
            method = CalculationMethod.MAKKAH,
            juristicSchool = JuristicSchool.STANDARD,
            timeZone = ZoneId.of("Asia/Riyadh")
        )
        val gap = s.isha.minuteOfDay() - s.maghrib.minuteOfDay()
        assertTrue("gap was $gap", gap in 89..91)
    }

    @Test
    fun dst_endDay_usesOffsetInForceAtNoon() {
        // London clocks go back on 2026-10-25 (01:00 BST -> 00:00 GMT). Daytime times must be GMT.
        val zone = ZoneId.of("Europe/London")
        fun dhuhr(day: Int) = PrayerCalculationEngine.calculateDailySchedule(
            LocalDate.of(2026, 10, day), 51.5074, -0.1278,
            CalculationMethod.MWL, JuristicSchool.STANDARD, zone
        ).dhuhr
        assertEquals(12, dhuhr(24).hour) // still BST
        assertEquals(11, dhuhr(25).hour) // transition day: GMT all day
        assertEquals(11, dhuhr(26).hour)
    }

    @Test
    fun dst_startDay_usesOffsetInForceAtNoon() {
        // New York clocks go forward on 2026-03-08. Dhuhr must already be in EDT (about 13:07).
        val s = PrayerCalculationEngine.calculateDailySchedule(
            date = LocalDate.of(2026, 3, 8),
            latitude = 40.7128,
            longitude = -74.0060,
            method = CalculationMethod.ISNA,
            juristicSchool = JuristicSchool.STANDARD,
            timeZone = ZoneId.of("America/New_York")
        )
        assertEquals(13, s.dhuhr.hour)
    }

    @Test
    fun highLatitude_summer_fajrAndIshaAreNotCollapsedOntoDhuhr() {
        // Oslo, midsummer: the sun never reaches -18 / -17 degrees.
        val s = PrayerCalculationEngine.calculateDailySchedule(
            date = LocalDate.of(2026, 6, 21),
            latitude = 59.9139,
            longitude = 10.7522,
            method = CalculationMethod.MWL,
            juristicSchool = JuristicSchool.STANDARD,
            timeZone = ZoneId.of("Europe/Oslo")
        )
        assertTrue(s.fajr != s.dhuhr)
        assertTrue(s.isha != s.dhuhr)
        assertTrue(s.fajr.isBefore(s.sunrise))
        assertTrue(!s.isha.isBefore(s.maghrib))
        assertOrdered("oslo", s)
    }

    @Test
    fun highLatitude_allRules_keepOrderedSchedule() {
        for (rule in HighLatitudeRule.values()) {
            val s = PrayerCalculationEngine.calculateDailySchedule(
                date = LocalDate.of(2026, 6, 21),
                latitude = 55.0,
                longitude = 0.0,
                method = CalculationMethod.MWL,
                juristicSchool = JuristicSchool.STANDARD,
                timeZone = ZoneId.of("UTC"),
                highLatitudeRule = rule
            )
            assertOrdered("rule=$rule", s)
        }
    }

    @Test
    fun polarDayAndNight_doNotCrashAndStayOrdered() {
        val zone = ZoneId.of("Europe/Oslo")
        for (date in listOf(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
            val s = PrayerCalculationEngine.calculateDailySchedule(
                date = date,
                latitude = 69.6492,
                longitude = 18.9553,
                method = CalculationMethod.MWL,
                juristicSchool = JuristicSchool.STANDARD,
                timeZone = zone
            )
            assertOrdered("tromso $date", s)
        }
    }

    @Test
    fun sweep_allMethodsAndSchools_keepOrderedSchedule() {
        val zone = ZoneId.of("UTC")
        var lat = -60
        while (lat <= 60) {
            var dayOfYear = 1
            while (dayOfYear <= 365) {
                val date = LocalDate.ofYearDay(2026, dayOfYear)
                for (method in CalculationMethod.values()) {
                    for (school in JuristicSchool.values()) {
                        val s = PrayerCalculationEngine.calculateDailySchedule(
                            date, lat.toDouble(), 0.0, method, school, zone
                        )
                        assertOrdered("lat=$lat date=$date method=$method school=$school", s)
                    }
                }
                dayOfYear += 30
            }
            lat += 10
        }
    }

    @Test
    fun tehran_maghribIsAfterSunsetByAngle() {
        val s = PrayerCalculationEngine.calculateDailySchedule(
            date = LocalDate.of(2026, 10, 11),
            latitude = 35.6892,
            longitude = 51.3890,
            method = CalculationMethod.TEHRAN,
            juristicSchool = JuristicSchool.STANDARD,
            timeZone = ZoneId.of("Asia/Tehran")
        )
        val gap = s.maghrib.minuteOfDay() - s.sunset.minuteOfDay()
        assertTrue("gap was $gap", gap in 10..30)
    }
}
