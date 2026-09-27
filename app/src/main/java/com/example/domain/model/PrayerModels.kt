package com.example.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class PrayerType(val displayName: String, val urduName: String, val isFard: Boolean) {
    FAJR("Fajr", "فجر", true),
    SUNRISE("Sunrise", "طلوع آفتاب", false),
    DHUHR("Dhuhr", "ظہر", true),
    ASR("Asr", "عصر", true),
    SUNSET("Sunset", "غروب آفتاب", false),
    MAGHRIB("Maghrib", "مغرب", true),
    ISHA("Isha", "عشاء", true),
    // Additional / Derived
    TAHAJJUD("Tahajjud", "تہجد", false),
    ISHRAQ("Ishraq", "اشراق", false),
    CHASHT("Chasht", "چاشت", false),
    ZAWAL("Zawal", "زوال", false),
    JUMMAH("Jummah", "جمعہ", true)
}

data class DailyPrayerSchedule(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val sunset: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val imsak: LocalTime,
    // Derived times according to Islamic jurisprudence
    val suhurEnd: LocalTime = fajr,
    val iftar: LocalTime = maghrib,
    val tahajjud: LocalTime,
    val ishraq: LocalTime,
    val chasht: LocalTime,
    val zawal: LocalTime,
    val jummah: LocalTime = dhuhr
)

data class PrayerState(
    val currentPrayer: PrayerType,
    val currentPrayerTime: LocalTime,
    val currentPrayerStart: LocalTime,
    val currentPrayerEnd: LocalTime,
    val upcomingPrayer: PrayerType,
    val upcomingPrayerTime: LocalTime,
    val remainingSeconds: Long,
    val negativeCountdownFormatted: String, // e.g., "-01:24:35"
    val progress: Float, // 0.0f to 1.0f
    val isQuietMode: Boolean = false
)

data class UserLocation(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val isAutoDetected: Boolean = false
)

enum class CalculationMethod(val id: Int, val methodName: String, val displayName: String = methodName) {
    KARACHI(1, "University of Islamic Sciences, Karachi"),
    ISNA(2, "Islamic Society of North America (ISNA)"),
    MWL(3, "Muslim World League (MWL)"),
    MAKKAH(4, "Umm Al-Qura University, Makkah"),
    EGYPT(5, "Egyptian General Authority of Survey"),
    TEHRAN(7, "Institute of Geophysics, University of Tehran"),
    GULF(8, "Gulf Region"),
    KUWAIT(9, "Kuwait"),
    QATAR(10, "Qatar"),
    SINGAPORE(11, "Majlis Ugama Islam Singapura"),
    DIYANET(13, "Diyanet İşleri Başkanlığı, Turkey")
}

enum class JuristicSchool(
    val id: Int,
    val displayName: String,
    val urduName: String,
    val shadowFactor: Double,
    val description: String
) {
    HANAFI(
        1,
        "Hanafi",
        "حنفی (عصر وقتِ ثانی)",
        2.0,
        "Shadow reaches 2x object length. Standard in Pakistan, India, Bangladesh, Turkey & Central Asia."
    ),
    STANDARD(
        0,
        "Shafi'i / Standard",
        "شافعی، مالکی، حنبلی (عصر وقتِ اول)",
        1.0,
        "Shadow reaches 1x object length. Standard in Saudi Arabia, UAE, Egypt, Arab world & SE Asia."
    )
}

fun getRecommendedMethodForLocation(country: String, city: String = ""): CalculationMethod {
    val c = country.lowercase().trim()
    val ct = city.lowercase().trim()
    return when {
        c.contains("saudi") || ct.contains("makkah") || ct.contains("mecca") || ct.contains("madinah") || ct.contains("riyadh") -> CalculationMethod.MAKKAH
        c.contains("emirates") || c.contains("uae") || ct.contains("dubai") || ct.contains("abu dhabi") -> CalculationMethod.GULF
        c.contains("qatar") || ct.contains("doha") -> CalculationMethod.QATAR
        c.contains("kuwait") -> CalculationMethod.KUWAIT
        c.contains("egypt") || ct.contains("cairo") || ct.contains("alexandria") -> CalculationMethod.EGYPT
        c.contains("turkey") || ct.contains("istanbul") || ct.contains("ankara") -> CalculationMethod.DIYANET
        c.contains("united states") || c.contains("usa") || c.contains("canada") || ct.contains("new york") || ct.contains("chicago") || ct.contains("toronto") -> CalculationMethod.ISNA
        c.contains("united kingdom") || c.contains("uk") || c.contains("france") || c.contains("germany") || ct.contains("london") || ct.contains("paris") || ct.contains("berlin") -> CalculationMethod.MWL
        c.contains("iran") || ct.contains("tehran") -> CalculationMethod.TEHRAN
        c.contains("singapore") || c.contains("malaysia") || c.contains("indonesia") || ct.contains("jakarta") || ct.contains("kuala lumpur") -> CalculationMethod.SINGAPORE
        c.contains("pakistan") || c.contains("india") || c.contains("bangladesh") || c.contains("afghanistan") || ct.contains("karachi") || ct.contains("lahore") || ct.contains("delhi") || ct.contains("dhaka") -> CalculationMethod.KARACHI
        else -> CalculationMethod.KARACHI
    }
}

fun getRecommendedSchoolForLocation(country: String, city: String = ""): JuristicSchool {
    val c = country.lowercase().trim()
    val ct = city.lowercase().trim()
    return when {
        c.contains("saudi") || c.contains("emirates") || c.contains("uae") || c.contains("egypt") || c.contains("jordan") || c.contains("oman") || c.contains("kuwait") || c.contains("qatar") || c.contains("malaysia") || c.contains("indonesia") -> JuristicSchool.STANDARD
        else -> JuristicSchool.HANAFI
    }
}

