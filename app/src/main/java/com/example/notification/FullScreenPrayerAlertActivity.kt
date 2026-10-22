package com.example.notification

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PrayerAlertType
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import kotlinx.coroutines.delay

class FullScreenPrayerAlertActivity : ComponentActivity() {

    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure activity to turn screen on and show over lock screen
        configureWindowOverLockscreen()

        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerUrdu = intent.getStringExtra("prayer_urdu") ?: "نماز"
        val cityName = intent.getStringExtra("city_name") ?: ""
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""
        val prayerType = intent.getStringExtra("prayer_type")
        val alertTypeId = intent.getStringExtra("alert_type") ?: PrayerAlertType.MEDIUM.id
        val notificationId = intent.getIntExtra("notification_id", prayerName.hashCode())

        val alertType = PrayerAlertType.fromId(alertTypeId)
        val totalDuration = if (alertType == PrayerAlertType.STRONG) 10 else 5

        // Start vibration pattern
        startVibration(alertType)

        setContent {
            AlDeenTheme(themeMode = AppThemeMode.DARK) {
                FullScreenAlertContent(
                    prayerName = prayerName,
                    prayerUrdu = prayerUrdu,
                    cityName = cityName,
                    prayerTime = prayerTime,
                    alertType = alertType,
                    totalDuration = totalDuration,
                    onDismiss = {
                        cancelNotification(notificationId)
                        stopAndFinish()
                    },
                    onMarkPrayed = {
                        cancelNotification(notificationId)
                        Toast.makeText(
                            this@FullScreenPrayerAlertActivity,
                            "Masha'Allah! May Allah accept your prayer (ماشاء اللہ! اللہ آپ کی نماز قبول فرمائے)",
                            Toast.LENGTH_LONG
                        ).show()
                        stopAndFinish()
                    },
                    onSnooze = {
                        cancelNotification(notificationId)
                        PrayerNotificationManager.scheduleSnooze(
                            context = this@FullScreenPrayerAlertActivity,
                            prayerName = prayerName,
                            prayerUrdu = prayerUrdu,
                            cityName = cityName,
                            prayerTime = prayerTime,
                            prayerType = prayerType,
                            minutes = 10
                        )
                        Toast.makeText(
                            this@FullScreenPrayerAlertActivity,
                            "Reminder set for 10 minutes later (۱۰ منٹ بعد یاد دہانی مقرر ہو گئی ہے)",
                            Toast.LENGTH_SHORT
                        ).show()
                        stopAndFinish()
                    },
                    onPlaySound = {
                        playSound()
                    },
                    onTimeout = {
                        stopAndFinish()
                    }
                )
            }
        }
    }

    private fun configureWindowOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "aldeen:prayer_alert_wakelock"
            )
            wakeLock?.acquire(15000L) // Safe 15 second auto-release
        } catch (_: Exception) {}
    }

    private fun startVibration(alertType: PrayerAlertType) {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = if (alertType == PrayerAlertType.STRONG) {
                longArrayOf(0, 600, 150, 600, 150)
            } else {
                longArrayOf(0, 400, 200, 400, 200)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun playSound() {
        try {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, soundUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun cancelNotification(notificationId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(notificationId)
    }

    private fun stopAndFinish() {
        try {
            vibrator?.cancel()
            ringtone?.stop()
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vibrator?.cancel()
            ringtone?.stop()
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
    }
}

@Composable
private fun FullScreenAlertContent(
    prayerName: String,
    prayerUrdu: String,
    cityName: String,
    prayerTime: String,
    alertType: PrayerAlertType,
    totalDuration: Int,
    onDismiss: () -> Unit,
    onMarkPrayed: () -> Unit,
    onSnooze: () -> Unit,
    onPlaySound: () -> Unit,
    onTimeout: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(totalDuration) }

    // Exactly 1 second delay before notification sound plays (as requested by user)
    LaunchedEffect(Unit) {
        delay(1000L)
        onPlaySound()
    }

    // Auto-dismiss countdown
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        onTimeout()
    }

    val progressFraction by animateFloatAsState(
        targetValue = remainingSeconds.toFloat() / totalDuration.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "alert_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041E11),
                        Color(0xFF0A3D24),
                        Color(0xFF04140B)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("fullscreen_prayer_alert_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Alert Mode Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = AlDeenTokens.ShapePill,
                    color = GoldAccent.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                ) {
                    Text(
                        text = if (alertType == PrayerAlertType.STRONG) "Strong Alert • 10s" else "Medium Alert • 5s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Quick dismiss X button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Alert",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Center: Glowing Islamic Mosque Icon & Prayer Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer.copy(alpha = 0.35f))
                        .border(2.dp, GoldAccent.copy(alpha = 0.8f), CircleShape)
                        .pulseEffect(minScale = 0.92f, maxScale = 1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Prayer Time Alert",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = prayerName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp
                    ),
                    textAlign = TextAlign.Center
                )

                AlDeenText(
                    text = "$prayerUrdu کی نماز کا وقت ہو گیا ہے",
                    style = AlDeenTypography.UrduBody.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (prayerTime.isNotBlank() || cityName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = listOfNotNull(prayerTime.ifBlank { null }, cityName.ifBlank { null }).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Countdown Progress Bar & Label
                Column(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Auto-closing in ${remainingSeconds}s (خودکار بندش)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            // Bottom Actions: 3 tactile buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. I've Prayed Button
                Button(
                    onClick = onMarkPrayed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("alert_prayed_button"),
                    shape = AlDeenTokens.ShapePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I've Prayed (نماز ادا کی)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // 2. Remind me after 10 mins
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("alert_snooze_button"),
                    shape = AlDeenTokens.ShapePill,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remind in 10 mins (۱۰ منٹ بعد یاد دہانی)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }

                // 3. Dismiss Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("alert_dismiss_button")
                ) {
                    Text(
                        text = "Dismiss (بند کریں)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
