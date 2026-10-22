package com.example.domain.prayer

import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

/**
 * How Fajr/Isha are estimated when the sun never reaches the required angle
 * (high latitudes in summer) or when the angle-based time would be unrealistically far from
 * sunrise/sunset.
 */
enum class HighLatitudeRule {
    /** Night portion proportional to the twilight angle (angle / 60 of the night). */
    ANGLE_BASED,
    /** Fajr/Isha at most half of the night away from sunrise/sunset. */
    MIDDLE_OF_NIGHT,
    /** Fajr/Isha at most one seventh of the night away from sunrise/sunset. */
    ONE_SEVENTH
}

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
        timeZone: ZoneId = ZoneId.systemDefault(),
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED
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

        // Methods that define Maghrib by a sun depression angle instead of sunset
        val maghribAngle: Double? = if (method == CalculationMethod.TEHRAN) 4.5 else null

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

        // Base time offset for timezone. Read at local NOON (not midnight) so that on a DST
        // transition day (which switches during the night) all daytime prayer times use the
        // offset that is actually in force.
        val tzOffsetHours = try {
            timeZone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        } catch (_: Exception) {
            Math.round(longitude / 15.0).toDouble()
        }

        // Midday (Dhuhr)
        val dhuhrBase = 12.0 + tzOffsetHours - longitude / 15.0 - eqt

        // Sun angle helper. Returns null when the sun never reaches that angle on this date/latitude.
        // direction == true -> before solar noon, false -> after solar noon.
        fun sunAngleTimeOrNull(angle: Double, direction: Boolean): Double? {
            val cosH = (-sin(Math.toRadians(angle)) - sin(Math.toRadians(latitude)) * sin(Math.toRadians(dec))) /
                    (cos(Math.toRadians(latitude)) * cos(Math.toRadians(dec)))
            if (cosH.isNaN() || cosH > 1.0 || cosH < -1.0) return null
            val h = Math.toDegrees(acos(cosH)) / 15.0
            return if (direction) dhuhrBase - h else dhuhrBase + h
        }

        // Asr time helper (Shafi'i/Standard: shadow factor 1.0; Hanafi: shadow factor 2.0)
        // Returns null when the required sun altitude is never reached (polar day/night).
        fun computeAsr(factor: Double = 1.0): Double? {
            val d = Math.toDegrees(atan(factor + tan(Math.toRadians(abs(latitude - dec)))))
            val cosH = (sin(Math.toRadians(90.0 - d)) - sin(Math.toRadians(latitude)) * sin(Math.toRadians(dec))) /
                    (cos(Math.toRadians(latitude)) * cos(Math.toRadians(dec)))
            if (cosH.isNaN() || cosH > 1.0 || cosH < -1.0) return null
            val h = Math.toDegrees(acos(cosH)) / 15.0
            return dhuhrBase + h
        }

        // Sunrise / sunset. In polar day or polar night (no sunrise/sunset) fall back to a 12h day
        // centred on solar noon so every dependent calculation stays finite and ordered.
        val sunriseDec = sunAngleTimeOrNull(0.833, true) ?: (dhuhrBase - 6.0)
        val sunsetDec = sunAngleTimeOrNull(0.833, false) ?: (dhuhrBase + 6.0)
        val dhuhrDec = dhuhrBase
        // Asr must lie between solar noon and sunset; if it cannot be computed use ~60% of the way to sunset.
        val asrDec = (computeAsr(juristicSchool.shadowFactor) ?: (dhuhrBase + 0.6 * (sunsetDec - dhuhrBase)))
            .coerceIn(dhuhrBase, maxOf(sunsetDec, dhuhrBase))

        // Length of the night (sunset -> next sunrise) in hours, used by the high-latitude rules.
        val nightLength = 24.0 - (sunsetDec - sunriseDec)

        fun nightPortion(angle: Double): Double = when (highLatitudeRule) {
            HighLatitudeRule.ANGLE_BASED -> angle / 60.0 * nightLength
            HighLatitudeRule.MIDDLE_OF_NIGHT -> nightLength / 2.0
            HighLatitudeRule.ONE_SEVENTH -> nightLength / 7.0
        }

        // Fajr: use the real angle time unless it does not exist / is further from sunrise
        // than the allowed night portion.
        val fajrRaw = sunAngleTimeOrNull(fajrAngle, true)
        val fajrPortion = nightPortion(fajrAngle)
        val fajrAdjusted = if (fajrRaw == null || sunriseDec - fajrRaw > fajrPortion) {
            sunriseDec - fajrPortion
        } else {
            fajrRaw
        }
        // Fajr must stay within the same civil day and never be later than sunrise.
        val fajrDec = minOf(maxOf(fajrAdjusted, 0.0), sunriseDec)

        // Maghrib: exactly at sunset, or a sun-depression angle for methods that define it so (Tehran).
        val maghribDec = if (maghribAngle != null) {
            (sunAngleTimeOrNull(maghribAngle, false) ?: sunsetDec).coerceAtLeast(sunsetDec)
        } else {
            sunsetDec
        }

        val ishaUncapped = if (ishaIntervalMin > 0) {
            maghribDec + (ishaIntervalMin / 60.0)
        } else {
            val ishaRaw = sunAngleTimeOrNull(ishaAngle, false)
            val ishaPortion = nightPortion(ishaAngle)
            val adjusted = if (ishaRaw == null || ishaRaw - sunsetDec > ishaPortion) {
                sunsetDec + ishaPortion
            } else {
                ishaRaw
            }
            adjusted.coerceAtLeast(maghribDec)
        }
        // Keep Isha on the same civil day (23:59 at the latest) because the prayer-state engine and
        // the alarm scheduler work with a single day's LocalTime values; never earlier than Maghrib.
        val ishaDec = maxOf(minOf(ishaUncapped, 23.0 + 59.0 / 60.0), maghribDec)

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

    /** Converts decimal hours to a LocalTime rounded to the nearest minute (wraps around midnight). */
    private fun decimalToTime(decimalHours: Double): LocalTime {
        val totalMinutes = Math.floorMod(Math.round(decimalHours * 60.0), 1440L).toInt()
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
    }
}
