package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.repository.SettingsRepository
import com.example.domain.model.CalculationMethod
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import com.example.domain.model.PrayerState
import com.example.domain.model.PrayerType
import com.example.domain.model.UserLocation
import com.example.domain.prayer.PrayerCalculationEngine
import com.example.domain.prayer.PrayerStateEngine
import com.example.ui.more.convertGregorianToHijri
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextBoundaryUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_UPDATE_PRAYERS,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            "android.intent.action.TIME_SET" -> {
                updateAllWidgets(context)
                scheduleNextBoundaryUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_PRAYERS = "com.example.widget.ACTION_UPDATE_PRAYERS"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val settingsRepo = SettingsRepository.getInstance(context.applicationContext)
            val location: UserLocation = settingsRepo.getNotificationLocation()

            val method = settingsRepo.calculationMethod.value
            val school = settingsRepo.juristicSchool.value
            val is24Hour = settingsRepo.is24HourFormat.value
            val hijriAdj = settingsRepo.hijriAdjustment.value
            val timeZone = settingsRepo.getZoneIdForLocation(location)

            val now = LocalTime.now(timeZone)
            val today = java.time.LocalDate.now(timeZone)

            // Calculate schedule accurately using existing astronomical engine
            val schedule: DailyPrayerSchedule = com.example.data.repository.PrayerScheduleCache.get(
                context, location, method, school, timeZone, today,
                persist = settingsRepo.getBackgroundLocation() != null
            )

            val prayerState: PrayerState = PrayerStateEngine.calculateCurrentState(
                schedule = schedule,
                currentTime = now,
                isQuietMode = settingsRepo.isQuietMode.value
            )

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val effectiveWidth = if (isLandscape) {
                val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
                if (maxW > 0) maxW else (options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140) ?: 140)
            } else {
                val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
                if (minW > 0) minW else (options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 140) ?: 140)
            }
            val effectiveHeight = if (isLandscape) {
                val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
                if (minH > 0) minH else (options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110) ?: 110)
            } else {
                val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
                if (maxH > 0) maxH else (options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) ?: 110)
            }

            val isSmall = effectiveWidth < 180 || effectiveHeight < 110
            val isLarge = effectiveWidth >= 220 && effectiveHeight >= 150

            val views: RemoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val smallViews = buildSmallRemoteViews(context, appWidgetId, prayerState, is24Hour)
                val mediumViews = buildMediumRemoteViews(context, appWidgetId, schedule, prayerState, location, is24Hour)
                val largeViews = buildLargeRemoteViews(context, appWidgetId, schedule, prayerState, location, method, school, today, hijriAdj, is24Hour)

                RemoteViews(
                    mapOf(
                        SizeF(120f, 80f) to smallViews,
                        SizeF(200f, 95f) to mediumViews,
                        SizeF(220f, 150f) to largeViews
                    )
                )
            } else {
                when {
                    isSmall -> buildSmallRemoteViews(context, appWidgetId, prayerState, is24Hour)
                    isLarge -> buildLargeRemoteViews(context, appWidgetId, schedule, prayerState, location, method, school, today, hijriAdj, is24Hour)
                    else -> buildMediumRemoteViews(context, appWidgetId, schedule, prayerState, location, is24Hour)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatTime(time: LocalTime, is24Hour: Boolean, includeAmPm: Boolean = true): String {
            val formatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            val formatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
            val shortFormatter12 = DateTimeFormatter.ofPattern("hh:mm", Locale.ENGLISH)
            return if (is24Hour) {
                time.format(formatter24)
            } else if (includeAmPm) {
                time.format(formatter12)
            } else {
                time.format(shortFormatter12)
            }
        }

        private fun createOpenAppPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "prayer_dashboard")
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId * 20 + 1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createOpenLocationPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val openLocationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "city_picker")
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId * 20 + 2,
                openLocationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun buildSmallRemoteViews(
            context: Context,
            appWidgetId: Int,
            prayerState: PrayerState,
            is24Hour: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_small)
            val upcoming = prayerState.upcomingPrayer
            val upcomingTimeFormatted = formatTime(prayerState.upcomingPrayerTime, is24Hour)
            val countdownFormatted = prayerState.negativeCountdownFormatted

            views.setOnClickPendingIntent(R.id.widget_prayer_root, createOpenAppPendingIntent(context, appWidgetId))
            views.setTextViewText(
                R.id.widget_next_prayer_name,
                "${upcoming.displayName} (${upcoming.urduName})"
            )
            views.setTextViewText(R.id.widget_next_prayer_time, upcomingTimeFormatted)
            views.setTextViewText(R.id.widget_countdown_text, countdownFormatted)
            return views
        }

        private fun buildMediumRemoteViews(
            context: Context,
            appWidgetId: Int,
            schedule: DailyPrayerSchedule,
            prayerState: PrayerState,
            location: UserLocation,
            is24Hour: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_medium)
            val upcoming = prayerState.upcomingPrayer
            val countdownFormatted = prayerState.negativeCountdownFormatted

            views.setOnClickPendingIntent(R.id.widget_prayer_root, createOpenAppPendingIntent(context, appWidgetId))
            val cityDisplay = if (location.cityName.isNotBlank()) location.cityName else "Baqiyyah"
            views.setTextViewText(R.id.widget_city_name, cityDisplay)
            views.setOnClickPendingIntent(R.id.widget_location_btn, createOpenLocationPendingIntent(context, appWidgetId))

            views.setTextViewText(
                R.id.widget_next_prayer_title,
                "Next: ${upcoming.displayName} (${upcoming.urduName})"
            )
            views.setTextViewText(R.id.widget_countdown_text, countdownFormatted)

            // 5 Prayer Times columns
            bindPrayerRow(views, R.id.widget_item_fajr, R.id.widget_name_fajr, R.id.widget_sub_fajr, R.id.widget_time_fajr, PrayerType.FAJR, formatTime(schedule.fajr, is24Hour, includeAmPm = false), upcoming)
            bindPrayerRow(views, R.id.widget_item_dhuhr, R.id.widget_name_dhuhr, R.id.widget_sub_dhuhr, R.id.widget_time_dhuhr, PrayerType.DHUHR, formatTime(schedule.dhuhr, is24Hour, includeAmPm = false), upcoming)
            bindPrayerRow(views, R.id.widget_item_asr, R.id.widget_name_asr, R.id.widget_sub_asr, R.id.widget_time_asr, PrayerType.ASR, formatTime(schedule.asr, is24Hour, includeAmPm = false), upcoming)
            bindPrayerRow(views, R.id.widget_item_maghrib, R.id.widget_name_maghrib, R.id.widget_sub_maghrib, R.id.widget_time_maghrib, PrayerType.MAGHRIB, formatTime(schedule.maghrib, is24Hour, includeAmPm = false), upcoming)
            bindPrayerRow(views, R.id.widget_item_isha, R.id.widget_name_isha, R.id.widget_sub_isha, R.id.widget_time_isha, PrayerType.ISHA, formatTime(schedule.isha, is24Hour, includeAmPm = false), upcoming)

            return views
        }

        private fun buildLargeRemoteViews(
            context: Context,
            appWidgetId: Int,
            schedule: DailyPrayerSchedule,
            prayerState: PrayerState,
            location: UserLocation,
            method: CalculationMethod,
            school: JuristicSchool,
            today: LocalDate,
            hijriAdj: Int,
            is24Hour: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_large)
            val upcoming = prayerState.upcomingPrayer
            val upcomingTimeFormatted = formatTime(prayerState.upcomingPrayerTime, is24Hour)
            val countdownFormatted = prayerState.negativeCountdownFormatted

            views.setOnClickPendingIntent(R.id.widget_prayer_root, createOpenAppPendingIntent(context, appWidgetId))

            val cityDisplay = if (location.cityName.isNotBlank()) {
                if (location.countryName.isNotBlank()) "${location.cityName}, ${location.countryName}"
                else location.cityName
            } else "Karachi, Pakistan"
            views.setTextViewText(R.id.widget_city_name, cityDisplay)
            views.setOnClickPendingIntent(R.id.widget_location_btn, createOpenLocationPendingIntent(context, appWidgetId))

            val hijri = convertGregorianToHijri(today.year, today.monthValue, today.dayOfMonth, hijriAdj)
            views.setTextViewText(
                R.id.widget_hijri_date,
                "${hijri.day} ${hijri.monthNameEnglish} ${hijri.year} AH • ${hijri.monthNameArabic}"
            )

            views.setTextViewText(
                R.id.widget_method_badge,
                "${school.displayName} (${method.methodName.take(12)}...)"
            )

            views.setTextViewText(
                R.id.widget_status_text,
                "Upcoming Prayer (${upcoming.urduName})"
            )

            views.setTextViewText(
                R.id.widget_next_prayer_title,
                "${upcoming.displayName} • $upcomingTimeFormatted"
            )
            views.setTextViewText(R.id.widget_countdown_text, countdownFormatted)

            val progressInt = (prayerState.progress * 100).toInt().coerceIn(0, 100)
            views.setProgressBar(R.id.widget_progress_bar, 100, progressInt, false)

            // Populate 5 prayers with active highlight and subtitle highlight
            bindPrayerRow(views, R.id.widget_item_fajr, R.id.widget_name_fajr, R.id.widget_sub_fajr, R.id.widget_time_fajr, PrayerType.FAJR, formatTime(schedule.fajr, is24Hour), upcoming)
            bindPrayerRow(views, R.id.widget_item_dhuhr, R.id.widget_name_dhuhr, R.id.widget_sub_dhuhr, R.id.widget_time_dhuhr, PrayerType.DHUHR, formatTime(schedule.dhuhr, is24Hour), upcoming)
            bindPrayerRow(views, R.id.widget_item_asr, R.id.widget_name_asr, R.id.widget_sub_asr, R.id.widget_time_asr, PrayerType.ASR, formatTime(schedule.asr, is24Hour), upcoming)
            bindPrayerRow(views, R.id.widget_item_maghrib, R.id.widget_name_maghrib, R.id.widget_sub_maghrib, R.id.widget_time_maghrib, PrayerType.MAGHRIB, formatTime(schedule.maghrib, is24Hour), upcoming)
            bindPrayerRow(views, R.id.widget_item_isha, R.id.widget_name_isha, R.id.widget_sub_isha, R.id.widget_time_isha, PrayerType.ISHA, formatTime(schedule.isha, is24Hour), upcoming)

            return views
        }

        private fun bindPrayerRow(
            views: RemoteViews,
            itemId: Int,
            nameId: Int,
            subId: Int,
            timeId: Int,
            type: PrayerType,
            timeStr: String,
            upcoming: PrayerType
        ) {
            val isNext = (type == upcoming)
            views.setInt(
                itemId,
                "setBackgroundResource",
                if (isNext) R.drawable.widget_prayer_active_bg else R.drawable.widget_prayer_item_bg
            )
            views.setTextColor(
                nameId,
                if (isNext) 0xFF2DD4A2.toInt() else 0xFFA0B5AC.toInt()
            )
            if (subId != 0) {
                views.setTextColor(
                    subId,
                    if (isNext) 0xFF2DD4A2.toInt() else 0xFF759186.toInt()
                )
            }
            views.setTextViewText(timeId, timeStr)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PrayerWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        /**
         * Schedules an exact AlarmManager trigger at the boundary of the upcoming prayer
         * so the widget transitions to the next prayer immediately upon prayer time arrival,
         * without draining battery with continuous loops.
         */
        fun scheduleNextBoundaryUpdate(context: Context) {
            try {
                val settingsRepo = SettingsRepository.getInstance(context.applicationContext)
                val location = settingsRepo.getNotificationLocation()
                val timeZone = settingsRepo.getZoneIdForLocation(location)
                val today = java.time.LocalDate.now(timeZone)
                val now = LocalTime.now(timeZone)

                val schedule = com.example.data.repository.PrayerScheduleCache.get(
                    context, location, settingsRepo.calculationMethod.value,
                    settingsRepo.juristicSchool.value, timeZone, today,
                    persist = settingsRepo.getBackgroundLocation() != null
                )

                val state = PrayerStateEngine.calculateCurrentState(schedule, now)
                val nextPrayerTime = state.upcomingPrayerTime

                val targetDateTime = if (nextPrayerTime.isAfter(now)) {
                    today.atTime(nextPrayerTime)
                } else {
                    today.plusDays(1).atTime(nextPrayerTime)
                }

                val prayerBoundaryMillis = targetDateTime.atZone(timeZone).toInstant().toEpochMilli()
                val fifteenMinutesLater = System.currentTimeMillis() + 15 * 60 * 1000
                val triggerMillis = minOf(prayerBoundaryMillis, fifteenMinutesLater)

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_PRAYERS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    99881,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (alarmManager != null && triggerMillis > System.currentTimeMillis()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
