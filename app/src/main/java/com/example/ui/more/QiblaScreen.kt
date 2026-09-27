package com.example.ui.more

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserLocation
import com.example.domain.prayer.QiblaCalculator
import com.example.ui.components.AlDeenText
import com.example.ui.theme.ScriptLanguage
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Shortest angle difference in degrees, returning a value strictly in [-180, 180].
 * Uses Euclidean-safe wrapping to completely eliminate full 360-degree jump-spins.
 */
private fun shortestAngleDelta(target: Float, current: Float): Float {
    return ((target - current + 540f) % 360f) - 180f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    userLocation: UserLocation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }

    val qiblaBearing = remember(userLocation) {
        QiblaCalculator.calculateQiblaBearing(userLocation.latitude, userLocation.longitude).toFloat()
    }
    val distanceKm = remember(userLocation) {
        QiblaCalculator.calculateDistanceToKaabaKm(userLocation.latitude, userLocation.longitude)
    }

    // Continuous angles without 360-degree wrapping jumps
    var continuousHeading by remember { mutableFloatStateOf(0f) }
    var pitchAngle by remember { mutableFloatStateOf(0f) }
    var rollAngle by remember { mutableFloatStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensor by remember { mutableStateOf(false) }

    var isManualMode by remember { mutableStateOf(false) }
    var manualHeading by remember { mutableFloatStateOf(0f) }
    var hapticEnabled by remember { mutableStateOf(true) }
    var audioEnabled by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    // Magnetic Declination for True North correction
    val declination = remember(userLocation) {
        try {
            val field = android.hardware.GeomagneticField(
                userLocation.latitude.toFloat(),
                userLocation.longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            field.declination
        } catch (_: Exception) {
            0f
        }
    }

    // Sensor Listener Lifecycle
    DisposableEffect(sensorManager) {
        if (sensorManager == null) {
            hasSensor = false
            return@DisposableEffect onDispose {}
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastSmoothedHeading = 0f
        var internalContinuous = 0f
        var isFirstReading = true

        val listener = object : SensorEventListener {
            private val lastAccelerometer = FloatArray(3)
            private val lastMagnetometer = FloatArray(3)
            private var lastAccelerometerSet = false
            private var lastMagnetometerSet = false

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || isManualMode) return

                var rawDeg: Float? = null

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // Compensate for display rotation (portrait vs landscape)
                    val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.display?.rotation ?: android.view.Surface.ROTATION_0
                    } else {
                        @Suppress("DEPRECATION")
                        (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0
                    }

                    var axisX = SensorManager.AXIS_X
                    var axisY = SensorManager.AXIS_Y
                    when (displayRotation) {
                        android.view.Surface.ROTATION_90 -> {
                            axisX = SensorManager.AXIS_Y
                            axisY = SensorManager.AXIS_MINUS_X
                        }
                        android.view.Surface.ROTATION_180 -> {
                            axisX = SensorManager.AXIS_MINUS_X
                            axisY = SensorManager.AXIS_MINUS_Y
                        }
                        android.view.Surface.ROTATION_270 -> {
                            axisX = SensorManager.AXIS_MINUS_Y
                            axisY = SensorManager.AXIS_X
                        }
                    }

                    val remappedMatrix = FloatArray(9)
                    if (SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)) {
                        SensorManager.getOrientation(remappedMatrix, orientation)
                    } else {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                    }

                    // Check if device is held upright/tilted
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    if (abs(pitch) > 45f) {
                        val uprightMatrix = FloatArray(9)
                        if (SensorManager.remapCoordinateSystem(remappedMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, uprightMatrix)) {
                            SensorManager.getOrientation(uprightMatrix, orientation)
                        }
                    }

                    rawDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    pitchAngle = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    rollAngle = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    hasSensor = true
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val alpha = 0.25f
                    for (i in 0..2) {
                        lastAccelerometer[i] = lastAccelerometer[i] + alpha * (event.values[i] - lastAccelerometer[i])
                    }
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val alpha = 0.25f
                    for (i in 0..2) {
                        lastMagnetometer[i] = lastMagnetometer[i] + alpha * (event.values[i] - lastMagnetometer[i])
                    }
                    lastMagnetometerSet = true
                }

                if (rawDeg == null && lastAccelerometerSet && lastMagnetometerSet) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(r, orientation)
                        rawDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        pitchAngle = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        rollAngle = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        hasSensor = true
                    }
                }

                if (rawDeg != null) {
                    // Correct for magnetic declination to get True North accuracy
                    val targetNormalized = (rawDeg + declination + 360f) % 360f
                    if (isFirstReading) {
                        lastSmoothedHeading = targetNormalized
                        internalContinuous = targetNormalized
                        isFirstReading = false
                    } else {
                        // Shortest delta eliminates all 360-degree jump-spins
                        val delta = shortestAngleDelta(targetNormalized, lastSmoothedHeading)
                        // Smooth moving average for steady needle
                        val smoothFactor = 0.25f
                        lastSmoothedHeading = (lastSmoothedHeading + delta * smoothFactor + 360f) % 360f
                        internalContinuous += delta * smoothFactor
                    }
                    continuousHeading = internalContinuous
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                sensorAccuracy = accuracy
            }
        }

        var registeredAny = false
        if (rotationSensor != null) {
            registeredAny = true
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (accelSensor != null && magnetSensor != null) {
            registeredAny = true
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        hasSensor = registeredAny

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Effective heading in 0..360 range for display
    val currentHeading = if (isManualMode || !hasSensor) {
        manualHeading
    } else {
        (continuousHeading % 360f + 360f) % 360f
    }

    // Continuous dial rotation (negative heading so North points in actual physical direction)
    val targetDialRotation = if (isManualMode || !hasSensor) -manualHeading else -continuousHeading
    val animatedDialRotation by animateFloatAsState(
        targetValue = targetDialRotation,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 280f),
        label = "dial_rot"
    )

    // Angle of needle pointing to Qibla
    val relativeQiblaAngle = animatedDialRotation + qiblaBearing

    // Angular deviation from Kaaba
    val diffToQibla = shortestAngleDelta(qiblaBearing, currentHeading)
    val absDiff = abs(diffToQibla)
    val isFacingQibla = absDiff <= 3.5f

    // Spirit level calculation: flat if pitch & roll within +/- 12 degrees
    val isDeviceFlat = abs(pitchAngle) <= 12f && abs(rollAngle) <= 12f

    // Haptic vibration and audio chime when user aligns with Kaaba
    var lastHapticTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isFacingQibla) {
        if (isFacingQibla) {
            val now = System.currentTimeMillis()
            if (now - lastHapticTime > 1200) {
                lastHapticTime = now
                if (hapticEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 60), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(60)
                    }
                }
                if (audioEnabled) {
                    try {
                        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 70)
                        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 120)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val animatedGlowColor by animateColorAsState(
        targetValue = if (isFacingQibla) GoldAccent else EmeraldPrimary,
        animationSpec = tween(400),
        label = "compass_glow"
    )

    val cardinalDirection = remember(qiblaBearing) {
        when (qiblaBearing) {
            in 0.0..22.5 -> "N"
            in 22.5..67.5 -> "NE"
            in 67.5..112.5 -> "E"
            in 112.5..157.5 -> "SE"
            in 157.5..202.5 -> "S"
            in 202.5..247.5 -> "SW"
            in 247.5..292.5 -> "W"
            in 292.5..337.5 -> "NW"
            else -> "N"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "3D Qibla Compass (قبلہ نما)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = "${userLocation.cityName}, ${userLocation.countryName}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qibla_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                actions = {
                    // Calibration guide button
                    IconButton(onClick = { showCalibrationDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sensor Calibration",
                            tint = EmeraldPrimary
                        )
                    }

                    // Manual interactive rotation toggle
                    IconButton(
                        onClick = { isManualMode = !isManualMode },
                        modifier = Modifier.testTag("qibla_mode_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Toggle Manual Compass",
                            tint = if (isManualMode) GoldAccent else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("qibla_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFacingQibla) EmeraldContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isFacingQibla) GoldAccent else MintBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AlDeenText(
                            text = "Qibla (قبلہ رخ)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = "${String.format("%.1f", qiblaBearing)}° $cardinalDirection",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(34.dp), color = MintBorder)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AlDeenText(
                            text = "Heading (موجودہ رخ)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = "${currentHeading.roundToInt()}°",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFacingQibla) GoldAccent else TextPrimary
                            )
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(34.dp), color = MintBorder)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AlDeenText(
                            text = "Distance (فاصلہ)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = "${distanceKm.roundToInt()} km",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Real-time Direction Guidance Banner
            Surface(
                shape = AlDeenTokens.ShapePill,
                color = if (isFacingQibla) GoldAccent else if (!isDeviceFlat) MaterialTheme.colorScheme.surfaceVariant else EmeraldPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (isFacingQibla) GoldAccent else MintBorder),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isFacingQibla -> Icons.Default.CheckCircle
                            !isDeviceFlat -> Icons.Default.ScreenLockLandscape
                            diffToQibla > 0 -> Icons.Default.ArrowForward
                            else -> Icons.Default.ArrowBack
                        },
                        contentDescription = null,
                        tint = if (isFacingQibla) TextPrimary else EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AlDeenText(
                        text = when {
                            isFacingQibla -> "Aligned with Kaaba! (آپ قبلہ رخ ہیں)"
                            !isDeviceFlat -> "Hold phone flat on table/palm (فون کو سیدھا رکھیں)"
                            diffToQibla > 0 -> "Rotate Right ➔ ${absDiff.roundToInt()}°"
                            else -> "Rotate Left ⬅ ${absDiff.roundToInt()}°"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isFacingQibla) TextPrimary else EmeraldPrimary
                        ),
                        targetScript = ScriptLanguage.URDU
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 3D Precision Compass Dial & Needle
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .graphicsLayer {
                        rotationX = (-pitchAngle.coerceIn(-25f, 25f) * 0.35f)
                        rotationY = (rollAngle.coerceIn(-25f, 25f) * 0.35f)
                        cameraDistance = 14f * density
                        shadowElevation = if (isFacingQibla) 18.dp.toPx() else 10.dp.toPx()
                    }
                    .shadow(elevation = 12.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A2B), // Deep Islamic Emerald
                                Color(0xFF112219),
                                Color(0xFF09120D)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer 3D Metallic Bezel and Degree Ticks
                val mintBorderColor = MintBorder
                val goldAccentColor = GoldAccent
                val emeraldPrimaryColor = EmeraldPrimary

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedDialRotation)
                ) {
                    draw3DCompassBezel(
                        facingQibla = isFacingQibla,
                        glowColor = animatedGlowColor,
                        goldColor = goldAccentColor
                    )
                }

                // Kaaba 3D Compass Needle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(relativeQiblaAngle),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        draw3DNeedle(
                            isFacingQibla = isFacingQibla,
                            glowColor = animatedGlowColor,
                            primaryColor = emeraldPrimaryColor,
                            goldColor = goldAccentColor
                        )
                    }

                    // Golden Kaaba Indicator icon at the needle tip
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 18.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isFacingQibla) goldAccentColor else Color(0xFFD4AF37),
                            shadowElevation = if (isFacingQibla) 10.dp else 4.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Kaaba Direction",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // 3D Center Hub with Embedded Spirit Bubble Level
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2E4C39),
                                    Color(0xFF14271C),
                                    Color(0xFF0A130E)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Bubble level crosshair
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(color = mintBorderColor.copy(alpha = 0.6f), radius = 18.dp.toPx(), style = Stroke(1.dp.toPx()))
                        drawCircle(color = mintBorderColor.copy(alpha = 0.3f), radius = 8.dp.toPx(), style = Stroke(0.8.dp.toPx()))

                        // Dynamic bubble based on pitch and roll
                        val maxOffsetPx = 14.dp.toPx()
                        val bubbleX = (rollAngle.coerceIn(-30f, 30f) / 30f) * maxOffsetPx
                        val bubbleY = (-pitchAngle.coerceIn(-30f, 30f) / 30f) * maxOffsetPx

                        val bubbleCenter = Offset(c.x + bubbleX, c.y + bubbleY)
                        val bubbleColor = if (isDeviceFlat) emeraldPrimaryColor else goldAccentColor

                        drawCircle(
                            color = bubbleColor,
                            radius = 5.dp.toPx(),
                            center = bubbleCenter
                        )
                    }

                    // Metallic cap rim
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, goldAccentColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }

                // Top Fixed Phone Heading Marker
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .shadow(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Slider if Manual Simulation is On
            AnimatedVisibility(visible = isManualMode || !hasSensor) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MintBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AlDeenText(
                                text = "Manual Heading Simulator (دستی گھمائیں)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            )
                            Text(
                                text = "${manualHeading.roundToInt()}°",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                        }
                        Slider(
                            value = manualHeading,
                            onValueChange = { manualHeading = it },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Features & Telemetry Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MintBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Navigation Telemetry & Coordinates",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Holy Kaaba Coordinates", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Text(text = "21°25'21\" N, 39°49'34\" E", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Deviation from Qibla", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Text(
                            text = if (isFacingQibla) "0° (Aligned ✓)" else "${absDiff.roundToInt()}° away",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFacingQibla) EmeraldPrimary else TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Haptic Vibration Alert", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { hapticEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldContainer
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlDeenText(text = "Alignment Audio Chime (آواز)", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = { audioEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldContainer
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sensor Calibration & Health", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (sensorAccuracy) {
                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> EmeraldPrimary.copy(alpha = 0.15f)
                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> GoldAccent.copy(alpha = 0.15f)
                                else -> Color.Red.copy(alpha = 0.15f)
                            },
                            modifier = Modifier.clickable { showCalibrationDialog = true }
                        ) {
                            Text(
                                text = when (sensorAccuracy) {
                                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Accurate ✓"
                                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium ⚡"
                                    else -> "Calibrate ↻"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (sensorAccuracy) {
                                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> EmeraldPrimary
                                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> GoldAccent
                                        else -> Color.Red
                                    },
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Spirit Level Accuracy", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        AlDeenText(
                            text = if (isDeviceFlat) "Level: Flat ✓" else "Tilted (فون سیدھا کریں)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isDeviceFlat) EmeraldPrimary else GoldAccent
                            ),
                            targetScript = ScriptLanguage.URDU
                        )
                    }
                }
            }
        }
    }

    // Sensor Calibration Guide Dialog
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = {
                Text(
                    text = "Compass Sensor Calibration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                )
            },
            text = {
                Column {
                    Text(
                        text = "To ensure maximum compass accuracy and remove magnetic interference:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Wave your phone in a Figure-8 motion (∞) three to four times.\n" +
                                "2. Keep the phone away from laptops, metallic objects, and magnets.\n" +
                                "3. Lay the phone flat on your open palm for the most precise Qibla heading.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 20.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smooth filter with zero jump-spin active",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalibrationDialog = false }) {
                    AlDeenText("OK (سمجھ گئے)", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Draws the 3D high-precision metallic compass bezel, cardinal letters, and tick marks.
 */
private fun DrawScope.draw3DCompassBezel(
    facingQibla: Boolean,
    glowColor: Color,
    goldColor: Color
) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Outer 3D gold rim ring
    drawCircle(
        color = goldColor.copy(alpha = 0.5f),
        radius = radius - 3.dp.toPx(),
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Inner subtle glow ring
    drawCircle(
        color = glowColor.copy(alpha = 0.3f),
        radius = radius - 10.dp.toPx(),
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Precision Tick Marks (Every 5 degrees)
    for (deg in 0 until 360 step 5) {
        val angleRad = Math.toRadians(deg.toDouble()).toFloat()
        val isCardinal = deg % 90 == 0
        val isMajor = deg % 30 == 0
        val isMedium = deg % 15 == 0

        val tickLen = when {
            isCardinal -> 16.dp.toPx()
            isMajor -> 11.dp.toPx()
            isMedium -> 7.dp.toPx()
            else -> 4.dp.toPx()
        }

        val strokeW = when {
            isCardinal -> 2.5.dp.toPx()
            isMajor -> 1.8.dp.toPx()
            else -> 0.9.dp.toPx()
        }

        val strokeColor = when {
            deg == 0 -> Color(0xFFE53935) // North is Red
            isCardinal -> goldColor
            isMajor -> Color(0xFF81C784)
            else -> Color(0xFF4E715B)
        }


        val outerR = radius - 14.dp.toPx()
        val innerR = outerR - tickLen

        val outerX = center.x + outerR * sin(angleRad)
        val outerY = center.y - outerR * cos(angleRad)
        val innerX = center.x + innerR * sin(angleRad)
        val innerY = center.y - innerR * cos(angleRad)

        drawLine(
            color = strokeColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    // Inner decorative concentric ring
    drawCircle(
        color = Color(0xFF234431),
        radius = radius - 55.dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Draw Cardinal Text Markers (N, E, S, W)
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 14.sp.toPx()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val cardinals = listOf(
        Pair(0, "N" to android.graphics.Color.parseColor("#E53935")),
        Pair(90, "E" to android.graphics.Color.parseColor("#D4AF37")),
        Pair(180, "S" to android.graphics.Color.parseColor("#81C784")),
        Pair(270, "W" to android.graphics.Color.parseColor("#D4AF37"))
    )

    for ((deg, info) in cardinals) {
        val (label, colorInt) = info
        val angleRad = Math.toRadians(deg.toDouble()).toFloat()
        val textR = radius - 35.dp.toPx()
        val tx = center.x + textR * sin(angleRad)
        val ty = center.y - textR * cos(angleRad) + (textPaint.textSize / 3f)
        textPaint.color = colorInt
        drawContext.canvas.nativeCanvas.drawText(label, tx, ty, textPaint)
    }

    // Degree text numbers every 30°
    val degPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 9.sp.toPx()
        color = android.graphics.Color.parseColor("#8BA894")
    }
    for (deg in 30 until 360 step 30) {
        if (deg % 90 == 0) continue
        val angleRad = Math.toRadians(deg.toDouble()).toFloat()
        val textR = radius - 34.dp.toPx()
        val tx = center.x + textR * sin(angleRad)
        val ty = center.y - textR * cos(angleRad) + (degPaint.textSize / 3f)
        drawContext.canvas.nativeCanvas.drawText("$deg°", tx, ty, degPaint)
    }
}

/**
 * Draws the sculpted 3D two-tone beveled needle.
 */
private fun DrawScope.draw3DNeedle(
    isFacingQibla: Boolean,
    glowColor: Color,
    primaryColor: Color,
    goldColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val needleTopY = center.y - (size.height * 0.38f)
    val needleBottomY = center.y + (size.height * 0.28f)
    val needleWidthHalf = 7.dp.toPx()

    // 1. Top Half: Left illuminated facet (Emerald / Gold highlight)
    val topLeftPath = Path().apply {
        moveTo(center.x, needleTopY)
        lineTo(center.x - needleWidthHalf, center.y)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(
        path = topLeftPath,
        color = if (isFacingQibla) goldColor else primaryColor
    )


    // 2. Top Half: Right shaded facet (Darker emerald for 3D depth)
    val topRightPath = Path().apply {
        moveTo(center.x, needleTopY)
        lineTo(center.x + needleWidthHalf, center.y)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(
        path = topRightPath,
        color = if (isFacingQibla) Color(0xFFC59B27) else Color(0xFF0A4428)
    )

    // 3. Bottom Half: Left facet (Silver/white)
    val bottomLeftPath = Path().apply {
        moveTo(center.x, needleBottomY)
        lineTo(center.x - (needleWidthHalf * 0.7f), center.y)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(
        path = bottomLeftPath,
        color = Color(0xFFB0BEC5)
    )

    // 4. Bottom Half: Right shaded facet (Dark slate)
    val bottomRightPath = Path().apply {
        moveTo(center.x, needleBottomY)
        lineTo(center.x + (needleWidthHalf * 0.7f), center.y)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(
        path = bottomRightPath,
        color = Color(0xFF78909C)
    )
}
