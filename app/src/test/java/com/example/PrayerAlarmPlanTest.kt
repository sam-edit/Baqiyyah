package com.example

import com.example.domain.model.*
import com.example.domain.prayer.PrayerCalculationEngine
import com.example.notification.PrayerAlarmPlan
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class PrayerAlarmPlanTest {
    private val zone = ZoneId.of("Asia/Karachi")
    private fun schedule(date: LocalDate) = PrayerCalculationEngine.calculateDailySchedule(
        date, 32.336, 74.353, CalculationMethod.KARACHI, JuristicSchool.HANAFI, zone)

    @Test fun passedPrayerUsesTomorrowCalculation() {
        val today = LocalDate.of(2026, 10, 15)
        val now = today.atTime(23, 59).atZone(zone)
        val next = PrayerAlarmPlan.next(PrayerType.FAJR, now) { date ->
            schedule(date).copy(fajr = if (date == today) LocalTime.of(5, 0) else LocalTime.of(5, 7))
        }
        assertEquals(today.plusDays(1), next.toLocalDate())
        assertEquals(LocalTime.of(5, 7), next.toLocalTime())
    }

    @Test fun everyPrayerHasUniqueAlarmAndFutureOccurrence() {
        val now = LocalDate.of(2026, 10, 15).atTime(12, 0).atZone(zone)
        assertEquals(PrayerType.values().size, PrayerType.values().map { PrayerAlarmPlan.requestCode(it) }.toSet().size)
        PrayerType.values().forEach { type ->
            assertTrue(PrayerAlarmPlan.next(type, now, ::schedule).isAfter(now))
        }
    }

    @Test fun jummahOnlyFridayAndPassedFridayMovesAWeek() {
        val now = LocalDate.of(2026, 10, 16).atTime(23, 0).atZone(zone)
        val next = PrayerAlarmPlan.next(PrayerType.JUMMAH, now, ::schedule)
        assertEquals(DayOfWeek.FRIDAY, next.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 23), next.toLocalDate())
    }

    @Test fun midnightTracksLocalCalendarAcrossDst() {
        val london = ZoneId.of("Europe/London")
        val spring = LocalDate.of(2026, 3, 29).atStartOfDay(london)
        val autumn = LocalDate.of(2026, 10, 25).atStartOfDay(london)
        assertEquals(23L, Duration.between(spring, PrayerAlarmPlan.nextMidnight(spring)).toHours())
        assertEquals(25L, Duration.between(autumn, PrayerAlarmPlan.nextMidnight(autumn)).toHours())
    }
}
