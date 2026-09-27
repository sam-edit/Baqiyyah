package com.example.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@Composable
fun QuranFontSizeBottomSheet(
    arabicFontSize: Float,
    transFontSize: Float,
    onArabicFontSizeChange: (Float) -> Unit,
    onTransFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MintBorder),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Quran Font Size Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        AlDeenText(
                            text = "قرآن عربی اور ترجمہ کا سائز ایڈجسٹ کریں",
                            style = AlDeenTypography.UrduBody.copy(
                                color = EmeraldPrimary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AlDeenText(
                text = "Arabic Font Size (عربی فونٹ سائز): ${arabicFontSize.toInt()} sp",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = arabicFontSize,
                onValueChange = onArabicFontSizeChange,
                valueRange = 20f..36f,
                steps = 16,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldPrimary,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = MintBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            AlDeenText(
                text = "Translation & Word-by-Word Size (ترجمہ سائز): ${transFontSize.toInt()} sp",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = transFontSize,
                onValueChange = onTransFontSizeChange,
                valueRange = 10f..22f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldPrimary,
                    activeTrackColor = EmeraldPrimary,
                    inactiveTrackColor = MintBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = AlDeenTokens.ShapePill,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                AlDeenText(
                    text = "Done (محفوظ کریں)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }
    }
}
