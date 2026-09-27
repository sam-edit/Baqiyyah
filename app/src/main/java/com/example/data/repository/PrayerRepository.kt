package com.example.data.repository

import com.example.data.remote.NetworkModule
import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import com.example.domain.model.PrayerState
import com.example.domain.model.UserLocation
import com.example.domain.prayer.PrayerCalculationEngine
import com.example.domain.prayer.PrayerStateEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerRepository(
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _dailySchedule = MutableStateFlow(
        calculateSchedule(
            date = settingsRepository.getTodayDateForLocation(),
            location = settingsRepository.userLocation.value,
            method = settingsRepository.calculationMethod.value,
            school = settingsRepository.juristicSchool.value
        )
    )
    val dailySchedule: StateFlow<DailyPrayerSchedule> = _dailySchedule.asStateFlow()

    private val _prayerState = MutableStateFlow(
        PrayerStateEngine.calculateCurrentState(
            schedule = _dailySchedule.value,
            currentTime = LocalTime.now(settingsRepository.getLocationZoneId()),
            isQuietMode = settingsRepository.isQuietMode.value
        )
    )
    val prayerState: StateFlow<PrayerState> = _prayerState.asStateFlow()

    init {
        // Ticker for live negative countdown and state progress every 1 second
        coroutineScope.launch {
            while (isActive) {
                val tz = settingsRepository.getLocationZoneId()
                val now = LocalTime.now(tz)
                val schedule = _dailySchedule.value
                val quiet = settingsRepository.isQuietMode.value
                _prayerState.value = PrayerStateEngine.calculateCurrentState(schedule, now, quiet)
                delay(1000L)
            }
        }

        // Listen for location, calculation method, or juristic school changes
        coroutineScope.launch {
            combine(
                settingsRepository.userLocation,
                settingsRepository.calculationMethod,
                settingsRepository.juristicSchool
            ) { location: UserLocation, method: CalculationMethod, school: JuristicSchool ->
                Triple(location, method, school)
            }.collectLatest { (location, method, school) ->
                val tz = settingsRepository.getLocationZoneId()
                val today = settingsRepository.getTodayDateForLocation()
                val newSchedule = calculateSchedule(today, location, method, school)
                _dailySchedule.value = newSchedule
                val quiet = settingsRepository.isQuietMode.value
                _prayerState.value = PrayerStateEngine.calculateCurrentState(newSchedule, LocalTime.now(tz), quiet)
                // Background API sync using the exact method and juristic school
                syncWithRemote(location, method, school, today)
            }
        }

        // Listen for quiet mode changes separately without recalculating schedule
        coroutineScope.launch {
            settingsRepository.isQuietMode.collect { quiet ->
                val tz = settingsRepository.getLocationZoneId()
                val currentSchedule = _dailySchedule.value
                _prayerState.value = PrayerStateEngine.calculateCurrentState(currentSchedule, LocalTime.now(tz), quiet)
            }
        }
    }

    private fun calculateSchedule(
        date: LocalDate,
        location: UserLocation,
        method: CalculationMethod = settingsRepository.calculationMethod.value,
        school: JuristicSchool = settingsRepository.juristicSchool.value
    ): DailyPrayerSchedule {
        return PrayerCalculationEngine.calculateDailySchedule(
            date = date,
            latitude = location.latitude,
            longitude = location.longitude,
            method = method,
            juristicSchool = school,
            timeZone = settingsRepository.getLocationZoneId()
        )
    }

    private suspend fun syncWithRemote(
        location: UserLocation,
        method: CalculationMethod = settingsRepository.calculationMethod.value,
        school: JuristicSchool = settingsRepository.juristicSchool.value,
        date: LocalDate = settingsRepository.getTodayDateForLocation()
    ) {
        if (location.latitude == 0.0 && location.longitude == 0.0) return
        try {
            withContext(Dispatchers.IO) {
                val dateStr = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val response = NetworkModule.alAdhanApi.getTimingsByCoordinates(
                    date = dateStr,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    method = method.id,
                    school = school.id
                )
                if (response.code == 200 && response.data?.timings != null) {
                    val t = response.data.timings
                    fun parseT(raw: String?): LocalTime? {
                        return try {
                            val clean = raw?.substringBefore(" ")?.trim() ?: return null
                            LocalTime.parse(clean, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val f = parseT(t["Fajr"])
                    val sr = parseT(t["Sunrise"])
                    val d = parseT(t["Dhuhr"])
                    val a = parseT(t["Asr"])
                    val ss = parseT(t["Sunset"])
                    val m = parseT(t["Maghrib"])
                    val i = parseT(t["Isha"])

                    if (f != null && sr != null && d != null && a != null && m != null && i != null) {
                        val current = _dailySchedule.value
                        val ishraqTime = sr.plusMinutes(15)
                        val midMorningMin = java.time.Duration.between(sr, d).toMinutes() / 2
                        val chashtTime = sr.plusMinutes(midMorningMin.coerceAtLeast(45))
                        val zawalTime = d.minusMinutes(10)
                        val todayDate = date
                        val ishaDt = java.time.LocalDateTime.of(todayDate, i)
                        val fajrDt = java.time.LocalDateTime.of(
                            if (f.isBefore(i)) todayDate.plusDays(1) else todayDate,
                            f
                        )
                        val adjustedFajrDt = if (fajrDt.isBefore(ishaDt)) fajrDt.plusDays(1) else fajrDt
                        val nightDurationMin = java.time.Duration.between(ishaDt, adjustedFajrDt).toMinutes()
                        val tahajjudTime = ishaDt.plusMinutes((nightDurationMin * 2) / 3).toLocalTime()

                        val updated = current.copy(
                            fajr = f,
                            sunrise = sr,
                            dhuhr = d,
                            asr = a,
                            sunset = ss ?: m,
                            maghrib = m,
                            isha = i,
                            suhurEnd = f,
                            iftar = m,
                            tahajjud = tahajjudTime,
                            ishraq = ishraqTime,
                            chasht = chashtTime,
                            zawal = zawalTime,
                            jummah = d
                        )
                        _dailySchedule.value = updated
                        val tz = settingsRepository.getLocationZoneId()
                        _prayerState.value = PrayerStateEngine.calculateCurrentState(
                            schedule = updated,
                            currentTime = LocalTime.now(tz),
                            isQuietMode = settingsRepository.isQuietMode.value
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Offline or network error - astronomical calculations remain 100% active
        }
    }

    fun formatTime(time: LocalTime, is24Hour: Boolean): String {
        return if (is24Hour) {
            time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
        } else {
            time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
        }
    }
}
