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
fun ForbiddenSalatTimesCard(
    schedule: DailyPrayerSchedule,
    formatTime: (LocalTime) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("forbidden_salat_times_card"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, RedForbidden.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForbiddenCardGradient)
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            // Card Title + Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Forbidden Salat Times",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RedForbidden,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Praying voluntary (Nafl) Salat is prohibited during these intervals",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exactly 1 row with 3 child blocks: Sunrise, Zawal, Sunset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingSmall)
            ) {
                ForbiddenChildBlock(
                    title = "Sunrise",
                    urduTitle = "طلوع آفتاب",
                    timeFormatted = formatTime(schedule.sunrise),
                    modifier = Modifier.weight(1f)
                )

                ForbiddenChildBlock(
                    title = "Zawal",
                    urduTitle = "نصف النہار",
                    timeFormatted = formatTime(schedule.zawal),
                    modifier = Modifier.weight(1f)
                )

                ForbiddenChildBlock(
                    title = "Sunset",
                    urduTitle = "غروب آفتاب",
                    timeFormatted = formatTime(schedule.sunset),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ForbiddenChildBlock(
    title: String,
    urduTitle: String,
    timeFormatted: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AlDeenTokens.ShapeSmallCard,
        color = RedForbiddenLight,
        border = BorderStroke(1.dp, RedForbidden.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = RedForbidden,
                    fontSize = 11.sp
                )
            )

            AlDeenText(
                text = urduTitle,
                style = AlDeenTypography.UrduBody.copy(
                    fontWeight = FontWeight.Medium,
                    color = RedForbidden.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )
        }
    }
}
