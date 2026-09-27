package com.example.domain.prayer

import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

/**
 * Astronomical calculation engine for Islamic prayer times.
 * Provides accurate prayer times offline based on standard conventions (Karachi, ISNA, MWL, Makkah, Egypt).
 */
object PrayerCalculationEngine {

    fun calculateDailySchedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.KARACHI,
        juristicSchool: JuristicSchool = JuristicSchool.HANAFI,
        timeZone: ZoneId = ZoneId.systemDefault()
    ): DailyPrayerSchedule {
        // Calculation parameters based on Method
        val (fajrAngle, ishaAngle, ishaIntervalMin) = when (method) {
            CalculationMethod.KARACHI -> Triple(18.0, 18.0, 0)
            CalculationMethod.ISNA -> Triple(15.0, 15.0, 0)
            CalculationMethod.MWL -> Triple(18.0, 17.0, 0)
            CalculationMethod.MAKKAH -> Triple(18.5, 0.0, 90) // 90 min after Maghrib
            CalculationMethod.EGYPT -> Triple(19.5, 17.5, 0)
            CalculationMethod.TEHRAN -> Triple(17.7, 14.0, 0)
            CalculationMethod.GULF -> Triple(19.5, 0.0, 90)
            CalculationMethod.KUWAIT -> Triple(18.0, 17.5, 0)
            CalculationMethod.QATAR -> Triple(18.0, 0.0, 90)
            CalculationMethod.SINGAPORE -> Triple(20.0, 18.0, 0)
            CalculationMethod.DIYANET -> Triple(18.0, 17.0, 0)
        }

        // Julian Day calculation
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth

        val julianDay = getJulianDay(year, month, day) - longitude / (15.0 * 24.0)

        // Solar coordinates
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))) / 15.0

        // Declination & Equation of Time
        val sinD = sin(Math.toRadians(e)) * sin(Math.toRadians(l))
        val dec = Math.toDegrees(asin(sinD))
        val eqt = q / 15.0 - fixHour(ra)

        // Base time offset for timezone:
        // Use the passed ZoneId for exact timezone and DST offset
        val tzOffsetHours = try {
            timeZone.rules.getOffset(date.atStartOfDay()).totalSeconds / 3600.0
        } catch (_: Exception) {
            Math.round(longitude / 15.0).toDouble()
        }

        // Midday (Dhuhr)
        val dhuhrBase = 12.0 + tzOffsetHours - longitude / 15.0 - eqt

        // Sun angle helper
        fun sunAngleTime(angle: Double, direction: Boolean): Double {
            val cosH = (-sin(Math.toRadians(angle)) - sin(Math.toRadians(latitude)) * sin(Math.toRadians(dec))) /
                    (cos(Math.toRadians(latitude)) * cos(Math.toRadians(dec)))
            if (cosH > 1.0 || cosH < -1.0) return dhuhrBase // Polar fallback
            val h = Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0
            return if (direction) dhuhrBase - h else dhuhrBase + h
        }

        // Asr time helper (Shafi'i/Standard: shadow factor 1.0; Hanafi: shadow factor 2.0)
        fun asrTime(factor: Double = 1.0): Double {
            val d = Math.toDegrees(atan(factor + tan(Math.toRadians(abs(latitude - dec)))))
            val cosH = (sin(Math.toRadians(90.0 - d)) - sin(Math.toRadians(latitude)) * sin(Math.toRadians(dec))) /
                    (cos(Math.toRadians(latitude)) * cos(Math.toRadians(dec)))
            val h = Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0
            return dhuhrBase + h
        }

        val fajrDec = sunAngleTime(fajrAngle, true)
        val sunriseDec = sunAngleTime(0.833, true)
        val dhuhrDec = dhuhrBase
        val asrFactor = juristicSchool.shadowFactor
        val asrDec = asrTime(asrFactor)
        val sunsetDec = sunAngleTime(0.833, false)
        val maghribDec = sunsetDec + (3.0 / 60.0) // ~3 min after sunset

        val ishaDec = if (ishaIntervalMin > 0) {
            maghribDec + (ishaIntervalMin / 60.0)
        } else {
            sunAngleTime(ishaAngle, false)
        }

        val fajrTime = decimalToTime(fajrDec)
        val sunriseTime = decimalToTime(sunriseDec)
        val dhuhrTime = decimalToTime(dhuhrDec)
        val asrTime = decimalToTime(asrDec)
        val sunsetTime = decimalToTime(sunsetDec)
        val maghribTime = decimalToTime(maghribDec)
        val ishaTime = decimalToTime(ishaDec)
        val imsakTime = fajrTime.minusMinutes(10)

        // Derived Islamic times
        // Ishraq: ~15 mins after Sunrise
        val ishraqTime = sunriseTime.plusMinutes(15)
        // Chasht (Duha): midway between Sunrise and Dhuhr
        val midMorningMin = java.time.Duration.between(sunriseTime, dhuhrTime).toMinutes() / 2
        val chashtTime = sunriseTime.plusMinutes(midMorningMin.coerceAtLeast(45))
        // Zawal: 10 mins before Dhuhr
        val zawalTime = dhuhrTime.minusMinutes(10)
        // Tahajjud: last third of the night between Isha and Fajr (crossing midnight)
        val todayDate = date
        val ishaDt = java.time.LocalDateTime.of(todayDate, ishaTime)
        val fajrDt = java.time.LocalDateTime.of(
            if (fajrTime.isBefore(ishaTime)) todayDate.plusDays(1) else todayDate,
            fajrTime
        )
        val adjustedFajrDt = if (fajrDt.isBefore(ishaDt)) fajrDt.plusDays(1) else fajrDt
        val nightDurationMin = java.time.Duration.between(ishaDt, adjustedFajrDt).toMinutes()
        val tahajjudTime = ishaDt.plusMinutes((nightDurationMin * 2) / 3).toLocalTime()

        return DailyPrayerSchedule(
            date = date,
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            sunset = sunsetTime,
            maghrib = maghribTime,
            isha = ishaTime,
            imsak = imsakTime,
            suhurEnd = fajrTime,
            iftar = maghribTime,
            tahajjud = tahajjudTime,
            ishraq = ishraqTime,
            chasht = chashtTime,
            zawal = zawalTime,
            jummah = dhuhrTime
        )
    }

    private fun getJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(a: Double): Double {
        var angle = a - 360.0 * floor(a / 360.0)
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(a: Double): Double {
        var hour = a - 24.0 * floor(a / 24.0)
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun decimalToTime(decimalHours: Double): LocalTime {
        val fixed = fixHour(decimalHours)
        val hours = fixed.toInt().coerceIn(0, 23)
        val minutes = ((fixed - hours) * 60).toInt().coerceIn(0, 59)
        val seconds = ((((fixed - hours) * 60) - minutes) * 60).toInt().coerceIn(0, 59)
        return LocalTime.of(hours, minutes, seconds)
    }
}
