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
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.repository.SettingsRepository
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.PrayerAlertType
import com.example.domain.model.PrayerType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aldeen:receiver_wake")
        try {
            wakeLock?.acquire(8000L)
        } catch (_: Exception) {}

        try {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            PrayerNotificationManager.ACTION_MIDNIGHT_REFRESH -> {
                // Boot/clock changes invalidate old registrations; midnight retains due prayers.
                if (intent.action != PrayerNotificationManager.ACTION_MIDNIGHT_REFRESH) {
                    context.getSharedPreferences("prayer_alarm_state", Context.MODE_PRIVATE)
                        .edit().remove("signature").apply()
                }
                PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
            }
            PrayerNotificationManager.ACTION_TEST_NOTIFICATION -> {
                PrayerNotificationManager.showTestNotification(context)
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

                val type = runCatching { PrayerType.valueOf(prayerType ?: "") }.getOrNull()
                val trigger = intent.getLongExtra("scheduled_at", 0)
                val alarmPrefs = context.getSharedPreferences("prayer_alarm_state", Context.MODE_PRIVATE)
                if (trigger != 0L && type != null) {
                    if (alarmPrefs.getLong("scheduled:${type.name}", 0) != trigger ||
                        alarmPrefs.getLong("delivered:${type.name}", 0) == trigger) return
                    alarmPrefs.edit().putLong("delivered:${type.name}", trigger).apply()
                }
                try {
                PrayerNotificationManager.showPrayerNotification(
                    context = context,
                    prayerName = prayerName,
                    prayerUrdu = prayerUrdu,
                    cityName = cityName,
                    prayerTime = prayerTime,
                    prayerType = prayerType
                )

                } finally {
                    // Re-arm only this prayer: do not cancel simultaneous pending alerts.
                    if (type != null) PrayerNotificationManager.rescheduleAlarmsFromSavedState(context, type)
                }
            }
        }
        } finally {
            runCatching { if (wakeLock?.isHeld == true) wakeLock.release() }
        }
    }
}

object PrayerNotificationManager {
    const val CHANNEL_ID = "al_deen_prayer_channel_exact"
    private const val CHANNEL_NAME = "Prayer Times & Adhan (اوقاتِ نماز)"
    const val ACTION_MARK_PRAYED = "com.example.notification.ACTION_MARK_PRAYED"
    const val ACTION_SNOOZE = "com.example.notification.ACTION_SNOOZE"
    const val ACTION_TEST_NOTIFICATION = "com.example.notification.ACTION_TEST_NOTIFICATION"
    const val ACTION_MIDNIGHT_REFRESH = "com.example.notification.ACTION_MIDNIGHT_REFRESH"
    private const val MIDNIGHT_REQUEST_CODE = 90001
    private const val TEST_NOTIFICATION_ID = 99999
    private const val TEST_ALARM_REQUEST_CODE = 99998

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
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val configuredType = runCatching { PrayerType.valueOf(prayerType ?: "") }.getOrNull()
        if (configuredType != null && SettingsRepository.getInstance(context).prayerNotifications.value[configuredType] != true) return

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

        // Full Screen Alert for Medium (5s) and Strong (10s) modes
        val alertType = try {
            SettingsRepository.getInstance(context).getPrayerAlertType()
        } catch (_: Exception) {
            PrayerAlertType.LIGHT
        }

        if (alertType != PrayerAlertType.LIGHT) {
            val fullScreenIntent = Intent(context, FullScreenPrayerAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("prayer_name", prayerName)
                putExtra("prayer_urdu", prayerUrdu)
                putExtra("city_name", cityName)
                putExtra("prayer_time", prayerTime)
                putExtra("prayer_type", prayerType)
                putExtra("alert_type", alertType.id)
                putExtra("notification_id", notificationId)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 99,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)

            try {
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {}
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        runCatching { manager?.notify(notificationId, builder.build()) }
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

        // Full Screen Alert for Medium (5s) and Strong (10s) modes during test
        val alertType = try {
            SettingsRepository.getInstance(context).getPrayerAlertType()
        } catch (_: Exception) {
            PrayerAlertType.LIGHT
        }

        if (alertType != PrayerAlertType.LIGHT) {
            val fullScreenIntent = Intent(context, FullScreenPrayerAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("prayer_name", "Test Prayer Alert")
                putExtra("prayer_urdu", "ٹیسٹ نماز الرٹ")
                putExtra("city_name", "Al Deen App")
                putExtra("prayer_time", currentTimeStr)
                putExtra("alert_type", alertType.id)
                putExtra("notification_id", TEST_NOTIFICATION_ID)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                TEST_NOTIFICATION_ID + 99,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)

            try {
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {}
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        runCatching { manager?.notify(TEST_NOTIFICATION_ID, builder.build()) }
    }

    fun scheduleTestNotificationAlarm(context: Context, delaySeconds: Int = 3) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TEST_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerEpochMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        scheduleExactAlarm(alarmManager, triggerEpochMillis, pendingIntent)
    }

    fun cancelTestNotificationAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_TEST_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // Kept for existing callers; background location owns notification scheduling.
    fun scheduleDailyPrayerAlarms(
        context: Context, schedule: DailyPrayerSchedule, cityName: String,
        notificationSettings: Map<PrayerType, Boolean>
    ) = rescheduleAlarmsFromSavedState(context)

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerEpochMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }

    @Synchronized
    fun rescheduleAlarmsFromSavedState(context: Context, onlyPrayer: PrayerType? = null) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val settings = SettingsRepository.getInstance(context)
        val location = settings.getBackgroundLocation()
        val prefs = context.getSharedPreferences("prayer_alarm_state", Context.MODE_PRIVATE)
        val midnightIntent = Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION_MIDNIGHT_REFRESH)
        val midnightPending = PendingIntent.getBroadcast(context, MIDNIGHT_REQUEST_CODE, midnightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (location == null) {
            PrayerType.values().forEach { type ->
                val pending = PendingIntent.getBroadcast(context, PrayerAlarmPlan.requestCode(type),
                    Intent(context, PrayerAlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                if (pending != null) manager.cancel(pending)
            }
            manager.cancel(midnightPending)
            prefs.edit().clear().apply()
            return
        }
        val zone = settings.getZoneIdForLocation(location)
        val now = java.time.ZonedDateTime.now(zone)
        val method = settings.calculationMethod.value
        val school = settings.juristicSchool.value
        val signature = "${location.latitude}|${location.longitude}|${zone.id}|${method.id}|${school.id}"
        val sameLocation = prefs.getString("signature", null) == signature
        fun schedule(date: java.time.LocalDate) = com.example.data.repository.PrayerScheduleCache.get(
            context, location, method, school, zone, date)
        // Refresh today's persistent cache even when all notification toggles are off.
        schedule(now.toLocalDate())
        val format = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH)
        PrayerType.values().filter { onlyPrayer == null || it == onlyPrayer }.forEach { type ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val code = PrayerAlarmPlan.requestCode(type)
            if (settings.prayerNotifications.value[type] != true) {
                val pending = PendingIntent.getBroadcast(context, code, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                if (pending != null) manager.cancel(pending)
                prefs.edit().remove("scheduled:${type.name}").apply()
                return@forEach
            }
            val previous = prefs.getLong("scheduled:${type.name}", 0)
            val nowMillis = now.toInstant().toEpochMilli()
            // Preserve an already-due broadcast while another simultaneous prayer is handled.
            if (sameLocation && previous > 0 && nowMillis - previous in 0L..120_000L &&
                prefs.getLong("delivered:${type.name}", 0) != previous) return@forEach
            val next = PrayerAlarmPlan.next(type, now, ::schedule)
            val trigger = next.toInstant().toEpochMilli()
            intent.putExtra("prayer_name", type.displayName)
                .putExtra("prayer_urdu", type.urduName).putExtra("city_name", location.cityName)
                .putExtra("prayer_time", next.toLocalTime().format(format))
                .putExtra("prayer_type", type.name).putExtra("scheduled_at", trigger)
            val pending = PendingIntent.getBroadcast(context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            scheduleExactAlarm(manager, trigger, pending)
            prefs.edit().putLong("scheduled:${type.name}", trigger).apply()
        }
        prefs.edit().putString("signature", signature).apply()
        scheduleExactAlarm(manager, PrayerAlarmPlan.nextMidnight(now).toInstant().toEpochMilli(), midnightPending)
        if (onlyPrayer == null) runCatching {
            com.example.widget.PrayerWidgetProvider.updateAllWidgets(context)
            com.example.widget.PrayerWidgetProvider.scheduleNextBoundaryUpdate(context)
        }
    }
}
