package com.example.data.repository

import com.example.domain.model.*
import com.example.domain.prayer.PrayerCalculationEngine
import com.example.domain.prayer.PrayerStateEngine
import com.example.notification.PrayerNotificationManager
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
    // The primary city (or else the last detected city) is cached until that city's local 12 AM.
    // A manually browsed city is a preview: calculated for the moment, never cached, and it never
    // replaces the alarm/widget/cache location.
    private fun scheduleForDisplay(): DailyPrayerSchedule {
        val location = settingsRepository.userLocation.value
        val zone = settingsRepository.getZoneIdForLocation(location)
        val date = LocalDate.now(zone)
        val method = settingsRepository.calculationMethod.value
        val school = settingsRepository.juristicSchool.value
        return if (settingsRepository.isBackgroundLocation(location)) {
            PrayerScheduleCache.get(settingsRepository.context, location, method, school, zone, date)
        } else PrayerCalculationEngine.calculateDailySchedule(
            date, location.latitude, location.longitude, method, school, zone
        )
    }
    private val _dailySchedule = MutableStateFlow(scheduleForDisplay())
    val dailySchedule: StateFlow<DailyPrayerSchedule> = _dailySchedule.asStateFlow()
    private val _prayerState = MutableStateFlow(PrayerStateEngine.calculateCurrentState(
        _dailySchedule.value, LocalTime.now(settingsRepository.getLocationZoneId()), settingsRepository.isQuietMode.value))
    val prayerState: StateFlow<PrayerState> = _prayerState.asStateFlow()

    init {
        coroutineScope.launch {
            combine(settingsRepository.userLocation, settingsRepository.primaryLocation,
                settingsRepository.lastDetectedLocation, settingsRepository.calculationMethod,
                settingsRepository.juristicSchool) { _, _, _, _, _ -> Unit }
                .collectLatest {
                    // Display only. Alarms/widgets are rescheduled by SettingsRepository when the
                    // background city, method or school really changes (not on a city preview) and
                    // by the loop below at start-up and at each midnight.
                    _dailySchedule.value = scheduleForDisplay()
                }
        }
        coroutineScope.launch {
            var backgroundDate: LocalDate? = null
            while (isActive) {
                val zone = settingsRepository.getLocationZoneId()
                val today = LocalDate.now(zone)
                if (_dailySchedule.value.date != today) _dailySchedule.value = scheduleForDisplay()
                val alarmDate = LocalDate.now(settingsRepository.getNotificationZoneId())
                if (alarmDate != backgroundDate) {
                    PrayerNotificationManager.rescheduleAlarmsFromSavedState(settingsRepository.context)
                    backgroundDate = alarmDate
                }
                val now = LocalTime.now(zone)
                _prayerState.value = PrayerStateEngine.calculateCurrentState(
                    _dailySchedule.value, now, settingsRepository.isQuietMode.value)
                if (now.second == 0) runCatching {
                    com.example.widget.PrayerWidgetProvider.updateAllWidgets(settingsRepository.context)
                }
                delay(1000)
            }
        }
        coroutineScope.launch {
            combine(settingsRepository.is24HourFormat, settingsRepository.hijriAdjustment) { _, _ -> Unit }
                .collect { runCatching { com.example.widget.PrayerWidgetProvider.updateAllWidgets(settingsRepository.context) } }
        }
    }
    fun formatTime(time: LocalTime, is24Hour: Boolean): String = time.format(
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "hh:mm a", Locale.US))
}
