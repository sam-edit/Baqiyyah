package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PrayerState
import com.example.ui.theme.*

/**
 * Compact Prayer Dashboard for secondary main tabs (Quran, Hadith, Goals, More).
 * Styled to match the homepage PrayerDashboardCard in emerald gradient & gold accents,
 * but in a clean compact half-height banner.
 * Layout:
 * Left side: "Now • [Current Prayer]" and directly below it the Prayer Time.
 * Center: An arrow pointing from current prayer towards next prayer.
 * Right side: Next prayer time, and directly below it the Next Prayer name.
 */
@Composable
fun CompactPrayerDashboardCard(
    prayerState: PrayerState,
    formattedCurrentTime: String,
    formattedUpcomingTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_prayer_dashboard"),
        shape = AlDeenTokens.ShapeCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DashboardGradient)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: "Now" and directly below it the current prayer time
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Now • ${prayerState.currentPrayer.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedCurrentTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                }

                // Center: Elegant arrow indicating flow from current to next namaz
                Surface(
                    shape = AlDeenTokens.ShapePill,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Prayer Transition",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right side: next prayer name on top, and directly below it the next prayer time (same as left side)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Next • ${prayerState.upcomingPrayer.displayName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedUpcomingTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                }
            }
        }
    }
}

