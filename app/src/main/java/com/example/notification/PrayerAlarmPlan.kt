package com.example.notification

import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object PrayerAlarmPlan {
    fun time(schedule: DailyPrayerSchedule, type: PrayerType): LocalTime = when (type) {
        PrayerType.FAJR -> schedule.fajr
        PrayerType.SUNRISE -> schedule.sunrise
        PrayerType.DHUHR -> schedule.dhuhr
        PrayerType.ASR -> schedule.asr
        PrayerType.SUNSET -> schedule.sunset
        PrayerType.MAGHRIB -> schedule.maghrib
        PrayerType.ISHA -> schedule.isha
        PrayerType.TAHAJJUD -> schedule.tahajjud
        PrayerType.ISHRAQ -> schedule.ishraq
        PrayerType.CHASHT -> schedule.chasht
        PrayerType.ZAWAL -> schedule.zawal
        PrayerType.JUMMAH -> schedule.jummah
    }

    fun requestCode(type: PrayerType): Int = when (type) {
        PrayerType.FAJR -> 101
        PrayerType.DHUHR -> 102
        PrayerType.ASR -> 103
        PrayerType.MAGHRIB -> 104
        PrayerType.ISHA -> 105
        else -> 200 + type.ordinal
    }

    /** Tomorrow's occurrence uses tomorrow's calculated schedule, including DST. */
    fun next(type: PrayerType, now: ZonedDateTime, schedule: (LocalDate) -> DailyPrayerSchedule): ZonedDateTime {
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (type == PrayerType.JUMMAH && date.dayOfWeek != DayOfWeek.FRIDAY) continue
            val candidate = date.atTime(time(schedule(date), type)).atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        error("No prayer occurrence in next week")
    }

    fun nextMidnight(now: ZonedDateTime): ZonedDateTime = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
}
