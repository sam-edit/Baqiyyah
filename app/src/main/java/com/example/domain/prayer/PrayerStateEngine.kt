package com.example.domain.prayer

import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerState
import com.example.domain.model.PrayerType
import java.time.Duration
import java.time.LocalTime
import java.util.Locale

/**
 * Evaluates current prayer state, negative live countdown, interval progress, and upcoming prayer.
 */
object PrayerStateEngine {

    fun calculateCurrentState(
        schedule: DailyPrayerSchedule,
        currentTime: LocalTime = LocalTime.now(),
        isQuietMode: Boolean = false
    ): PrayerState {
        val fajr = schedule.fajr
        val sunrise = schedule.sunrise
        val dhuhr = schedule.dhuhr
        val asr = schedule.asr
        val maghrib = schedule.maghrib
        val isha = schedule.isha

        // Determine prayer interval
        val (currentPrayer, start, end, upcomingPrayer, nextStart) = when {
            currentTime.isBefore(fajr) -> {
                // Night before Fajr: current interval is Isha (from previous night) until Fajr
                Pentad(PrayerType.ISHA, isha, fajr, PrayerType.FAJR, fajr)
            }
            currentTime.isBefore(dhuhr) -> {
                // From Fajr until Dhuhr: Previous prayer was Fajr, next upcoming prayer is Dhuhr.
                Pentad(PrayerType.FAJR, fajr, dhuhr, PrayerType.DHUHR, dhuhr)
            }
            currentTime.isBefore(asr) -> {
                Pentad(PrayerType.DHUHR, dhuhr, asr, PrayerType.ASR, asr)
            }
            currentTime.isBefore(maghrib) -> {
                Pentad(PrayerType.ASR, asr, maghrib, PrayerType.MAGHRIB, maghrib)
            }
            currentTime.isBefore(isha) -> {
                Pentad(PrayerType.MAGHRIB, maghrib, isha, PrayerType.ISHA, isha)
            }
            else -> {
                // After Isha until midnight: upcoming is Fajr
                Pentad(PrayerType.ISHA, isha, fajr, PrayerType.FAJR, fajr)
            }
        }

        // Remaining seconds until target next prayer boundary
        val targetBoundary = end
        val totalIntervalSeconds = calculateIntervalSeconds(start, targetBoundary)
        val elapsedSeconds = calculateElapsedSeconds(start, currentTime)
        val remainingSeconds = (totalIntervalSeconds - elapsedSeconds).coerceAtLeast(0)

        val progress = if (totalIntervalSeconds > 0) {
            (elapsedSeconds.toFloat() / totalIntervalSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val formattedCountdown = formatNegativeCountdown(remainingSeconds)

        return PrayerState(
            currentPrayer = currentPrayer,
            currentPrayerTime = start,
            currentPrayerStart = start,
            currentPrayerEnd = end,
            upcomingPrayer = upcomingPrayer,
            upcomingPrayerTime = nextStart,
            remainingSeconds = remainingSeconds,
            negativeCountdownFormatted = formattedCountdown,
            progress = progress,
            isQuietMode = isQuietMode
        )
    }

    private data class Pentad(
        val currentPrayer: PrayerType,
        val start: LocalTime,
        val end: LocalTime,
        val upcomingPrayer: PrayerType,
        val nextStart: LocalTime
    )

    private fun calculateIntervalSeconds(start: LocalTime, end: LocalTime): Long {
        return if (end.isAfter(start)) {
            Duration.between(start, end).seconds
        } else {
            // crosses midnight
            (86400 - start.toSecondOfDay()) + end.toSecondOfDay().toLong()
        }
    }

    private fun calculateElapsedSeconds(start: LocalTime, current: LocalTime): Long {
        return if (current.isAfter(start) || current == start) {
            Duration.between(start, current).seconds
        } else {
            // crossed midnight
            (86400 - start.toSecondOfDay()) + current.toSecondOfDay().toLong()
        }
    }

    fun formatNegativeCountdown(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "-%02d:%02d:%02d", h, m, s)
    }
}
