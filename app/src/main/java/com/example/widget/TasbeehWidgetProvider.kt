package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class TasbeehWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
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

        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (action) {
            ACTION_INCREMENT -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Check duplicate tap prevention (debounce 80ms)
                    val clickTimestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L)
                    val lastHandled = lastHandledTimestamps[appWidgetId] ?: 0L
                    if (clickTimestamp > 0L && clickTimestamp <= lastHandled) {
                        return
                    }
                    lastHandledTimestamps[appWidgetId] = clickTimestamp

                    val phrase = TasbeehSharedRepository.getWidgetPhrase(context, appWidgetId)
                    val target = TasbeehSharedRepository.getTarget(context, appWidgetId, phrase)
                    val newCount = TasbeehSharedRepository.incrementCount(context, appWidgetId)

                    // Optional gentle haptic feedback
                    tryVibrate(context, strong = (target > 0 && newCount % target == 0))

                    // Update all widgets to reflect synchronized counts if sharing same phrase
                    updateAllWidgets(context)
                }
            }
            ACTION_REFRESH -> {
                updateAllWidgets(context)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = TasbeehSharedRepository.getPrefs(context).edit()
        for (id in appWidgetIds) {
            prefs.remove("widget_${id}_phrase_id")
            prefs.remove("widget_${id}_target")
            lastHandledTimestamps.remove(id)
        }
        prefs.apply()
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.widget.ACTION_INCREMENT"
        const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"
        const val EXTRA_TIMESTAMP = "extra_click_timestamp"

        private val lastHandledTimestamps = mutableMapOf<Int, Long>()

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

            // Select layout based on width and height
            val isSmall = minWidth < 180 || minHeight < 100
            val layoutId = if (isSmall) R.layout.widget_tasbeeh_small else R.layout.widget_tasbeeh_medium

            val views = RemoteViews(context.packageName, layoutId)

            val phrase = TasbeehSharedRepository.getWidgetPhrase(context, appWidgetId)
            val count = TasbeehSharedRepository.getCount(context, phrase.id)
            val target = TasbeehSharedRepository.getTarget(context, appWidgetId, phrase)
            val total = TasbeehSharedRepository.getCumulativeCount(context, phrase.id) + count
            val laps = if (target > 0) count / target else 0

            // Set Dhikr Details
            views.setTextViewText(R.id.widget_dhikr_arabic, phrase.arabic)
            if (!isSmall) {
                views.setTextViewText(R.id.widget_dhikr_translit, phrase.transliteration)
                views.setTextViewText(
                    R.id.widget_laps_text,
                    if (target > 0) "Lap $laps • Target: $target" else "Total: $total"
                )
            }

            // Set Target Badge
            if (target > 0) {
                views.setViewVisibility(R.id.widget_target_badge, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_target_badge, "/$target")
            } else {
                views.setViewVisibility(R.id.widget_target_badge, android.view.View.GONE)
            }

            // Set Main Count Text
            views.setTextViewText(R.id.widget_count_text, count.toString())

            // Set Progress Bar
            val progressPercent = if (target > 0) {
                (((count % target).toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 100
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

            // 1. PendingIntent for Root / Count Area -> Open Full Tasbeeh Counter screen in Baqiyyah
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "tasbeeh")
                putExtra("selected_phrase_id", phrase.id)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // 2. PendingIntent for +1 Increment Button
            val incrementIntent = Intent(context, TasbeehWidgetProvider::class.java).apply {
                action = ACTION_INCREMENT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            }
            val incrementPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                incrementIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_increment, incrementPendingIntent)

            // 3. PendingIntent for Reset -> Open Confirmation Dialog Activity
            val resetDialogIntent = Intent(context, TasbeehWidgetResetDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("phrase_id", phrase.id)
                putExtra("phrase_name", phrase.transliteration)
                putExtra("phrase_arabic", phrase.arabic)
                putExtra("current_count", count)
            }
            val resetDialogPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                resetDialogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_reset, resetDialogPendingIntent)

            // 4. PendingIntent for Settings (on medium layout) -> Open Config/Settings Activity
            if (!isSmall) {
                val settingsIntent = Intent(context, TasbeehWidgetConfigureActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val settingsPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 4,
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_settings, settingsPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TasbeehWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun tryVibrate(context: Context, strong: Boolean) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val vibrateEnabled = TasbeehSharedRepository.getPrefs(context).getBoolean("vibrate_enabled", true)
                if (vibrateEnabled && vibrator != null && vibrator.hasVibrator()) {
                    val duration = if (strong) 70L else 25L
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
