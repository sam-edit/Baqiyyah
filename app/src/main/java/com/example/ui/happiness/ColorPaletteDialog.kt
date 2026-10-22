package com.example.ui.happiness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import kotlin.math.*

val PredefinedHappinessColors = listOf(
    Color(0xFFFFFFFF), // Pure White
    Color(0xFFFFD700), // Islamic Gold
    Color(0xFFFFF3CD), // Warm Amber Cream
    Color(0xFF34D399), // Emerald Glow
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFFDA4AF), // Soft Rose
    Color(0xFFFDBA74), // Coral Peach
    Color(0xFFC084FC), // Lavender / Purple
    Color(0xFFFDE047), // Sunshine Yellow
    Color(0xFF0F172A)  // Charcoal Obsidian
)

val PredefinedBackgroundColors = listOf(
    Color(0x99000000), // Semi-transparent Dark
    Color(0xCC000000), // Deep Dark Black
    Color(0x99FFFFFF), // Semi-transparent White
    Color(0xCCE0A944), // Translucent Gold
    Color(0xCC0B3D26), // Translucent Emerald
    Color(0xCC0B1A30), // Translucent Midnight Blue
    Color(0xCC3A1C3C), // Translucent Twilight
    Color(0xFFFFFFFF), // Solid White
    Color(0xFF111827)  // Solid Black
)

/**
 * Circular Color Wheel Picker (Circle HSV Palette)
 * Allows selecting any color by touching the circular wheel, with a brightness slider.
 */
@Composable
fun ColorPaletteDialog(
    initialColor: Color,
    title: String = "Choose Color (رنگ منتخب کریں)",
    allowTransparent: Boolean = false,
    onColorSelected: (Color?) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert initial color to HSV
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                (initialColor.alpha * 255).toInt(),
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) } // 0..360
    var saturation by remember { mutableFloatStateOf(if (initialHsv[1] == 0f) 0.85f else initialHsv[1]) } // 0..1
    var valueBrightness by remember { mutableFloatStateOf(if (initialHsv[2] == 0f) 0.95f else initialHsv[2]) } // 0..1
    var isTransparentSelected by remember { mutableStateOf(false) }

    val currentColor = remember(hue, saturation, valueBrightness, isTransparentSelected) {
        if (isTransparentSelected) {
            Color.Transparent
        } else {
            val hsv = floatArrayOf(hue, saturation, valueBrightness)
            val argb = android.graphics.Color.HSVToColor(hsv)
            Color(argb)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 17.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Color Preview Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Preview:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isTransparentSelected) Color.Transparent else currentColor
                            )
                            .border(1.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTransparentSelected) {
                            Text("None (شفاف)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (allowTransparent) {
                    OutlinedButton(
                        onClick = { isTransparentSelected = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isTransparentSelected) EmeraldContainer else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "No Background / Transparent (شفاف)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isTransparentSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ==========================================
                // Circular Color Wheel (Circle Picker)
                // ==========================================
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                fun updateFromPosition(pos: Offset, sizePx: Float) {
                                    isTransparentSelected = false
                                    val center = Offset(sizePx / 2f, sizePx / 2f)
                                    val radius = sizePx / 2f
                                    val dx = pos.x - center.x
                                    val dy = pos.y - center.y
                                    val dist = sqrt(dx * dx + dy * dy)

                                    // Compute angle in degrees: 0..360
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    hue = angle
                                    saturation = (dist / radius).coerceIn(0f, 1f)
                                }

                                detectTapGestures { offset ->
                                    updateFromPosition(offset, size.width.toFloat())
                                }
                            }
                            .pointerInput("drag") {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    isTransparentSelected = false
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    val dx = change.position.x - center.x
                                    val dy = change.position.y - center.y
                                    val dist = sqrt(dx * dx + dy * dy)

                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    hue = angle
                                    saturation = (dist / radius).coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // 1. Draw 360 degree hue sweep gradient
                        val hueColors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                        drawCircle(
                            brush = Brush.sweepGradient(hueColors, center = center),
                            radius = radius,
                            center = center
                        )

                        // 2. Draw white saturation radial gradient from center
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )

                        // 3. Draw black overlay according to valueBrightness
                        if (valueBrightness < 1f) {
                            drawCircle(
                                color = Color.Black.copy(alpha = (1f - valueBrightness) * 0.85f),
                                radius = radius,
                                center = center
                            )
                        }

                        // 4. Draw outer border
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.6f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 5. Draw cursor indicator for selected hue & saturation
                        if (!isTransparentSelected) {
                            val cursorRad = Math.toRadians(hue.toDouble())
                            val cursorDist = saturation * radius
                            val cursorX = center.x + (cursorDist * cos(cursorRad)).toFloat()
                            val cursorY = center.y + (cursorDist * sin(cursorRad)).toFloat()

                            drawCircle(
                                color = Color.White,
                                radius = 9.dp.toPx(),
                                center = Offset(cursorX, cursorY)
                            )
                            drawCircle(
                                color = currentColor,
                                radius = 7.dp.toPx(),
                                center = Offset(cursorX, cursorY)
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.7f),
                                radius = 9.dp.toPx(),
                                center = Offset(cursorX, cursorY),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // Brightness Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness (چمک):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("${(valueBrightness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = valueBrightness,
                        onValueChange = {
                            isTransparentSelected = false
                            valueBrightness = it
                        },
                        valueRange = 0.15f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTransparentSelected) {
                        onColorSelected(null)
                    } else {
                        onColorSelected(currentColor)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Apply (رنگ لگائیں)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
