package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CalculationMethod
import com.example.ui.theme.*

/** Sky blue used for the "Tap to change" action in both light and dark themes. */
private val SkyBlueAction = Color(0xFF38BDF8)

/**
 * Small disclaimer line shown under the prayer dashboard:
 * (i) These calculations are according to {method}. Tap to change
 * The (i) icon is white in dark theme and black in light theme; "Tap to change" is sky blue in both.
 */
@Composable
fun CalculationMethodNote(
    method: CalculationMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val iconColor = if (isDark) Color.White else Color.Black
    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("calculation_method_note"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Disclaimer",
            tint = iconColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = buildAnnotatedString {
                append("These calculations are according to ${method.displayName}. ")
                withStyle(SpanStyle(color = SkyBlueAction, fontWeight = FontWeight.SemiBold)) {
                    append("Tap to change")
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontSize = 10.sp,
                lineHeight = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CalculationMethodDialog(
    selected: CalculationMethod,
    onSelect: (CalculationMethod) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            AlDeenText(
                text = "Calculation Method (طریقۂ حساب)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 17.sp
                )
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(CalculationMethod.values().toList()) { method ->
                    val isSelected = selected == method
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(method) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(method) },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
