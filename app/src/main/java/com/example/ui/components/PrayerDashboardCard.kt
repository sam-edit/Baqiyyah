package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun PrayerDashboardCard(
    prayerState: PrayerState,
    formattedCurrentTime: String,
    formattedCurrentStart: String,
    formattedUpcomingTime: String,
    onToggleQuietMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = prayerState.progress,
        label = "prayer_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_dashboard_card"),
        shape = AlDeenTokens.ShapeCard,
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationHigh),
        colors = CardDefaults.cardColors(containerColor = EmeraldDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DashboardGradient)
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header Row: Current/Next Prayer Heading + Urdu + Quiet Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Current Prayer",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                color = GoldAccent,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AlDeenText(
                                text = "موجودہ نماز",
                                style = AlDeenTypography.UrduBody.copy(
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Prayer Name in English & Urdu
                        Row(verticalAlignment = Alignment.Bottom) {
                            val displayName = prayerState.currentPrayer.displayName
                            val urduName = prayerState.currentPrayer.urduName
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AlDeenText(
                                text = "($urduName)",
                                style = AlDeenTypography.UrduBody.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 18.sp
                                ),
                                targetScript = ScriptLanguage.URDU,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    // Quiet Mode Action Control
                    Surface(
                        onClick = onToggleQuietMode,
                        shape = CircleShape,
                        color = if (prayerState.isQuietMode) Color(0xFFC0392B) else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(AlDeenTokens.TouchTargetMin)
                            .testTag("quiet_mode_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (prayerState.isQuietMode) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                contentDescription = if (prayerState.isQuietMode) "Quiet Mode Active (Muted)" else "Adhan Sound Active",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Started At block
                Surface(
                    shape = AlDeenTokens.ShapeSmallCard,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "Started at $formattedCurrentStart",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Negative Countdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = prayerState.negativeCountdownFormatted,
                        style = AlDeenTypography.CountdownDisplay,
                        modifier = Modifier.testTag("negative_countdown_display")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "UNTIL ${prayerState.upcomingPrayer.displayName.uppercase()} ADHAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thin Horizontal Progress Bar showing elapsed vs remaining interval
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(AlDeenTokens.ShapePill)
                        .testTag("prayer_interval_progress_bar"),
                    color = GoldAccent,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current to Upcoming Prayer Transition Row with Arrow (Left to Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AlDeenTokens.ShapeSmallCard)
                        .background(Color.Black.copy(alpha = 0.22f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Current Prayer Name & Start Time
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Now • ${prayerState.currentPrayer.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedCurrentTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    // Center: Arrow pointing from Current to Next Prayer
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Transition to Next Prayer",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right: Next Prayer Time & Name
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Next • ${prayerState.upcomingPrayer.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedUpcomingTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
