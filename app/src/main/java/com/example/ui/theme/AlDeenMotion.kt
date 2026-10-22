package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max

/**
 * Centralized motion tokens, spring specs, and interaction modifiers
 * for smooth, responsive, modern UI animations across Baqiyyah.
 */
object AlDeenMotion {
    // Standard Material 3 Durations (ms)
    const val DurationFast = 180
    const val DurationQuick = 240
    const val DurationNormal = 320
    const val DurationEmphasized = 400

    // Material 3 Emphasized Easings
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Standard Springs
    val DefaultSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    val ButtonSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 450f
    )

    val CardSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 380f
    )

    val SnappySpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 650f
    )

    val GentleSpring = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = 300f
    )

    val BouncySpring = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 400f
    )
}

/**
 * Non-intrusive press scale feedback for buttons, cards, and interactive elements.
 * Uses pointerInput without consuming gestures, ensuring standard clicks,
 * ripples, and parent vertical scrolling remain 100% functional.
 */
fun Modifier.pressScaleEffect(
    targetScale: Float = 0.96f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = AlDeenMotion.ButtonSpring,
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(targetScale, enabled) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Slightly stronger press scale for primary action buttons, FABs, and pills.
 */
fun Modifier.buttonPressScale(enabled: Boolean = true): Modifier =
    pressScaleEffect(targetScale = 0.95f, enabled = enabled)

/**
 * Subtle press scale for cards, list items, and tiles (around 0.975).
 */
fun Modifier.cardPressScale(enabled: Boolean = true): Modifier =
    pressScaleEffect(targetScale = 0.975f, enabled = enabled)

/**
 * Circular reveal shape expanding or contracting from a given origin fraction.
 */
class CircularRevealShape(
    private val progress: Float,
    private val originFraction: Offset = Offset(0.12f, 0.045f)
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        if (progress <= 0.001f) return Outline.Generic(path)
        if (progress >= 0.999f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))

        val centerX = originFraction.x * size.width
        val centerY = originFraction.y * size.height

        val maxRadius = hypot(
            max(centerX, size.width - centerX),
            max(centerY, size.height - centerY)
        )

        val currentRadius = maxRadius * progress
        path.addOval(
            Rect(
                left = centerX - currentRadius,
                top = centerY - currentRadius,
                right = centerX + currentRadius,
                bottom = centerY + currentRadius
            )
        )
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CircularRevealShape) return false
        return progress == other.progress && originFraction == other.originFraction
    }

    override fun hashCode(): Int {
        return 31 * progress.hashCode() + originFraction.hashCode()
    }
}

/**
 * Animated icon that smoothly morphs between a 3-bar hamburger menu and an 'X' close icon.
 */
@Composable
fun AnimatedMorphMenuIcon(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = EmeraldPrimary,
    size: Dp = AlDeenTokens.IconSizeMedium
) {
    val progress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "menu_morph_progress"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(AlDeenTokens.TouchTargetMin)
            .pressScaleEffect(0.9f)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 2.4.dp.toPx()
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val halfWidth = canvasWidth / 2f
            val halfHeight = canvasHeight / 2f

            val startX = 0.15f * canvasWidth
            val endX = 0.85f * canvasWidth

            // Interpolate top line Y from 0.25 to 0.50 (halfHeight)
            val topY = (0.25f + (0.50f - 0.25f) * progress) * canvasHeight
            val rotTop = 45f * progress

            drawContext.canvas.save()
            drawContext.transform.rotate(rotTop, pivot = Offset(halfWidth, topY))
            drawLine(
                color = tint,
                start = Offset(startX, topY),
                end = Offset(endX, topY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawContext.canvas.restore()

            // Middle line: fades out smoothly and shrinks slightly
            val middleAlpha = (1f - progress * 1.8f).coerceIn(0f, 1f)
            if (middleAlpha > 0f) {
                val middleIndent = (endX - startX) * 0.25f * progress
                drawLine(
                    color = tint.copy(alpha = tint.alpha * middleAlpha),
                    start = Offset(startX + middleIndent, halfHeight),
                    end = Offset(endX - middleIndent, halfHeight),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Interpolate bottom line Y from 0.75 to 0.50 (halfHeight)
            val bottomY = (0.75f - (0.75f - 0.50f) * progress) * canvasHeight
            val rotBottom = -45f * progress

            drawContext.canvas.save()
            drawContext.transform.rotate(rotBottom, pivot = Offset(halfWidth, bottomY))
            drawLine(
                color = tint,
                start = Offset(startX, bottomY),
                end = Offset(endX, bottomY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawContext.canvas.restore()
        }
    }
}

/**
 * Ambient pulsing breathing animation for active dots, live badges, and recording/playback.
 */
fun Modifier.pulseEffect(
    enabled: Boolean = true,
    minScale: Float = 0.85f,
    maxScale: Float = 1.18f,
    durationMillis: Int = 1100
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/**
 * Staggered cascade entrance modifier for cards in a column or list.
 * Remembers entrance state to prevent flickering on internal recompositions or scroll.
 */
fun Modifier.staggeredSlideFade(
    index: Int,
    durationMillis: Int = 320,
    staggerDelayMillis: Int = 40
): Modifier = composed {
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(hasEntered) }

    LaunchedEffect(Unit) {
        visible = true
        hasEntered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = (index * staggerDelayMillis).coerceAtMost(280),
            easing = AlDeenMotion.EmphasizedDecelerateEasing
        ),
        label = "stagger_alpha_$index"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 22f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "stagger_transY_$index"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}

/**
 * High-performance Shimmer effect modifier for skeleton loading cards and states.
 */
fun Modifier.shimmerEffect(
    enabled: Boolean = true,
    baseColor: Color = Color.Unspecified,
    highlightColor: Color = Color.Unspecified,
    durationMillis: Int = 1200
): Modifier = composed {
    if (!enabled) return@composed this

    val isDark = isSystemInDarkTheme()
    val actualBase = if (baseColor != Color.Unspecified) baseColor else if (isDark) Color(0xFF16251E) else Color(0xFFE2EFE7)
    val actualHighlight = if (highlightColor != Color.Unspecified) highlightColor else if (isDark) Color(0xFF263D31) else Color(0xFFF3FAF6)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(actualBase, actualHighlight, actualBase),
        start = Offset(translateAnim - 350f, translateAnim - 350f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

/**
 * Micro-interaction: rotates 360 degrees smoothly whenever trigger state changes.
 */
fun Modifier.rotateOnTrigger(
    trigger: Any?,
    durationMillis: Int = 450
): Modifier = composed {
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(trigger) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            rotationDegrees += 360f
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(durationMillis = durationMillis, easing = AlDeenMotion.EmphasizedDecelerateEasing),
        label = "rotate_on_trigger"
    )

    this.graphicsLayer {
        rotationZ = animatedRotation
    }
}

/**
 * Dancing 3-bar or 4-bar equalizer indicator for active audio recitations.
 */
@Composable
fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = EmeraldPrimary,
    barCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_transition")

    val heights = List(barCount) { i ->
        val duration = 400 + i * 140
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eq_bar_$i"
            ).value
        } else {
            0.3f
        }
    }

    Row(
        modifier = modifier
            .height(18.dp)
            .width((barCount * 4 + (barCount - 1) * 2).dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .background(color, shape = AlDeenTokens.ShapePill)
            )
        }
    }
}
