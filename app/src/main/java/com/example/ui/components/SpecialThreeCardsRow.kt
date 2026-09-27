package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyPrayerSchedule
import com.example.ui.theme.*
import java.time.LocalTime

@Composable
fun SpecialThreeCardsRow(
    schedule: DailyPrayerSchedule,
    formatTime: (LocalTime) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("special_three_cards_row"),
        horizontalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium)
    ) {
        // 1. Suhur End Card
        SpecialSubCard(
            title = "Suhur End",
            urduTitle = "سحری اختتام",
            timeFormatted = formatTime(schedule.suhurEnd),
            accentColor = BluePrayer,
            backgroundColor = BluePrayerLight,
            modifier = Modifier.weight(1f)
        )

        // 2. Iftar Card
        SpecialSubCard(
            title = "Iftar",
            urduTitle = "افطار",
            timeFormatted = formatTime(schedule.iftar),
            accentColor = AmberHighlight,
            backgroundColor = GoldLight,
            modifier = Modifier.weight(1f)
        )

        // 3. Tahajjud Card
        SpecialSubCard(
            title = "Tahajjud",
            urduTitle = "تہجد",
            timeFormatted = formatTime(schedule.tahajjud),
            accentColor = EmeraldPrimary,
            backgroundColor = EmeraldContainer.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SpecialSubCard(
    title: String,
    urduTitle: String,
    timeFormatted: String,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = AlDeenTokens.ShapeSmallCard,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationLow),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 11.sp
                )
            )

            AlDeenText(
                text = urduTitle,
                style = AlDeenTypography.UrduBody.copy(
                    fontWeight = FontWeight.Medium,
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )
        }
    }
}
