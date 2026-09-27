package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            "android.intent.action.TIME_SET" -> {
                // When phone reboots or time zone/date updates, recalculate and re-register exact alarms immediately
                PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
            }
            PrayerNotificationManager.ACTION_MARK_PRAYED -> {
                val notificationId = intent.getIntExtra("notification_id", 0)
                val prayerName = intent.getStringExtra("prayer_name") ?: "prayer"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(notificationId)
                Toast.makeText(context, "Masha'Allah! May Allah accept your $prayerName.", Toast.LENGTH_LONG).show()
            }
            PrayerNotificationManager.ACTION_SNOOZE -> {
                val notificationId = intent.getIntExtra("notification_id", 0)
                val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
                val prayerUrdu = intent.getStringExtra("prayer_urdu") ?: "نماز"
                val cityName = intent.getStringExtra("city_name") ?: ""
                val prayerTime = intent.getStringExtra("prayer_time") ?: ""
                val prayerType = intent.getStringExtra("prayer_type")
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(notificationId)

                // Schedule snooze reminder in 10 minutes exact
                PrayerNotificationManager.scheduleSnooze(context, prayerName, prayerUrdu, cityName, prayerTime, prayerType, 10)
                Toast.makeText(context, "Reminder set for 10 minutes later.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
                val prayerUrdu = intent.getStringExtra("prayer_urdu") ?: "نماز"
                val cityName = intent.getStringExtra("city_name") ?: ""
                val prayerTime = intent.getStringExtra("prayer_time") ?: ""
                val prayerType = intent.getStringExtra("prayer_type")

                PrayerNotificationManager.showPrayerNotification(
                    context = context,
                    prayerName = prayerName,
                    prayerUrdu = prayerUrdu,
                    cityName = cityName,
                    prayerTime = prayerTime,
                    prayerType = prayerType
                )

                // Automatically re-arm all upcoming alarms for tomorrow / next cycle
                // So even if the user never opens the app for days or weeks, exact alarms keep rolling forever!
                PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
            }
        }
    }
}

object PrayerNotificationManager {
    const val CHANNEL_ID = "al_deen_prayer_channel_exact"
    private const val CHANNEL_NAME = "Prayer Times & Adhan (اوقاتِ نماز)"
    const val ACTION_MARK_PRAYED = "com.example.notification.ACTION_MARK_PRAYED"
    const val ACTION_SNOOZE = "com.example.notification.ACTION_SNOOZE"
    private const val TEST_NOTIFICATION_ID = 99999

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Accurate prayer time alerts and reminders with audio/vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                enableLights(true)
                lightColor = Color.parseColor("#0F5132")
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getAppLogoBitmap(context: Context): android.graphics.Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun showPrayerNotification(
        context: Context,
        prayerName: String,
        prayerUrdu: String,
        cityName: String,
        prayerTime: String = "",
        prayerType: String? = null
    ) {
        createNotificationChannel(context)

        val notificationId = prayerName.hashCode()

        // 1. Main Intent: Opens app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: Mark as Prayed (نماز ادا کی)
        val prayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_MARK_PRAYED
            putExtra("notification_id", notificationId)
            putExtra("prayer_name", prayerUrdu)
            putExtra("prayer_type", prayerType)
        }
        val prayedPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            prayedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Snooze (10 منٹ بعد)
        val snoozeIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("notification_id", notificationId)
            putExtra("prayer_name", prayerName)
            putExtra("prayer_urdu", prayerUrdu)
            putExtra("city_name", cityName)
            putExtra("prayer_time", prayerTime)
            putExtra("prayer_type", prayerType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = prayerName
        val timeDisplay = prayerTime
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(timeDisplay)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setColor(Color.parseColor("#0F5132"))
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "I've Prayed", prayedPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Remind Me Later", snoozePendingIntent)

        val logoBitmap = getAppLogoBitmap(context)
        if (logoBitmap != null) {
            builder.setLargeIcon(logoBitmap)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, builder.build())
    }

    fun scheduleSnooze(
        context: Context,
        prayerName: String,
        prayerUrdu: String,
        cityName: String,
        prayerTime: String,
        prayerType: String?,
        minutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayerName)
            putExtra("prayer_urdu", prayerUrdu)
            putExtra("city_name", cityName)
            putExtra("prayer_time", prayerTime)
            putExtra("prayer_type", prayerType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (prayerName + "_snooze").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerEpochMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleExactAlarm(alarmManager, triggerEpochMillis, pendingIntent)
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Prayed
        val prayedIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_MARK_PRAYED
            putExtra("notification_id", TEST_NOTIFICATION_ID)
            putExtra("prayer_name", "Test Prayer")
        }
        val prayedPendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_NOTIFICATION_ID + 1,
            prayedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("notification_id", TEST_NOTIFICATION_ID)
            putExtra("prayer_name", "Test Prayer")
            putExtra("prayer_urdu", "Test Prayer")
            putExtra("city_name", "")
            putExtra("prayer_time", "")
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_NOTIFICATION_ID + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentTimeStr = LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH))

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prayer Alert Test")
            .setContentText("Time: $currentTimeStr • Notification Alert")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Test notification successful!\nNotifications will arrive on time with app icon, prayer name, and scheduled time."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.parseColor("#0F5132"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "I've Prayed", prayedPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Remind Me Later", snoozePendingIntent)

        val logoBitmap = getAppLogoBitmap(context)
        if (logoBitmap != null) {
            builder.setLargeIcon(logoBitmap)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(TEST_NOTIFICATION_ID, builder.build())
    }

    fun scheduleDailyPrayerAlarms(
        context: Context,
        schedule: DailyPrayerSchedule,
        cityName: String,
        notificationSettings: Map<PrayerType, Boolean>
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val settingsRepo = com.example.data.repository.SettingsRepository.getInstance(context)
        val zone = settingsRepo.getNotificationZoneId()

        val prayersToSchedule = listOf(
            Triple(PrayerType.FAJR, schedule.fajr, 101),
            Triple(PrayerType.DHUHR, schedule.dhuhr, 102),
            Triple(PrayerType.ASR, schedule.asr, 103),
            Triple(PrayerType.MAGHRIB, schedule.maghrib, 104),
            Triple(PrayerType.ISHA, schedule.isha, 105)
        )

        val nowInZone = LocalDateTime.now(zone)
        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH)

        prayersToSchedule.forEach { (type, time, requestCode) ->
            val isEnabled = notificationSettings[type] ?: true
            val formattedTime = time.format(timeFormatter)

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("prayer_name", type.displayName)
                putExtra("prayer_urdu", type.urduName)
                putExtra("city_name", cityName)
                putExtra("prayer_time", formattedTime)
                putExtra("prayer_type", type.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!isEnabled) {
                alarmManager.cancel(pendingIntent)
                return@forEach
            }

            var prayerDateTime = LocalDateTime.of(schedule.date, time)
            if (prayerDateTime.isBefore(nowInZone)) {
                // If today's prayer time has passed in the location's timezone, schedule for tomorrow's occurrence
                prayerDateTime = prayerDateTime.plusDays(1)
            }

            val triggerEpochMillis = prayerDateTime.atZone(zone).toInstant().toEpochMilli()
            scheduleExactAlarm(alarmManager, triggerEpochMillis, pendingIntent)
        }
    }

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerEpochMillis: Long, pendingIntent: PendingIntent) {
        try {
            // AlarmClockInfo guarantees exact firing down to the second, bypassing Doze mode and manufacturer batching
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerEpochMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        }
    }

    /**
     * Recalculates and schedules the next exact prayer alarms using stored settings, location,
     * calculation method, and juristic school without requiring the user to open the app.
     */
    fun rescheduleAlarmsFromSavedState(context: Context) {
        try {
            val settingsRepo = com.example.data.repository.SettingsRepository.getInstance(context)
            val location = settingsRepo.getNotificationLocation()
            val zoneId = settingsRepo.getNotificationZoneId()
            val method = settingsRepo.calculationMethod.value
            val school = settingsRepo.juristicSchool.value
            val notifs = settingsRepo.prayerNotifications.value
            val todayDate = java.time.LocalDate.now(zoneId)

            val schedule = com.example.domain.prayer.PrayerCalculationEngine.calculateDailySchedule(
                date = todayDate,
                latitude = location.latitude,
                longitude = location.longitude,
                method = method,
                juristicSchool = school,
                timeZone = zoneId
            )

            scheduleDailyPrayerAlarms(
                context = context,
                schedule = schedule,
                cityName = location.cityName,
                notificationSettings = notifs
            )
        } catch (_: Exception) {
            // Guard against background startup issues
        }
    }
}
