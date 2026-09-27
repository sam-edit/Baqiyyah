package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyPrayerSchedule
import com.example.domain.model.JuristicSchool
import com.example.domain.model.PrayerType
import com.example.ui.theme.*
import java.time.LocalTime

@Composable
fun TodayPrayerTimesCard(
    schedule: DailyPrayerSchedule,
    activePrayer: PrayerType,
    formatTime: (LocalTime) -> String,
    juristicSchool: JuristicSchool = JuristicSchool.HANAFI,
    onToggleJuristicSchool: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val primaryPrayers = listOf(
        PrayerType.FAJR to schedule.fajr,
        PrayerType.DHUHR to schedule.dhuhr,
        PrayerType.ASR to schedule.asr,
        PrayerType.MAGHRIB to schedule.maghrib,
        PrayerType.ISHA to schedule.isha
    )

    val additionalPrayers = listOf(
        PrayerType.TAHAJJUD to schedule.tahajjud,
        PrayerType.ISHRAQ to schedule.ishraq,
        PrayerType.CHASHT to schedule.chasht,
        PrayerType.ZAWAL to schedule.zawal,
        PrayerType.JUMMAH to schedule.jummah
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_prayer_times_card"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            // Header: Title and Hijri/Daily context + Asr School Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today Prayer Times",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        onClick = onToggleJuristicSchool,
                        shape = AlDeenTokens.ShapePill,
                        color = EmeraldContainer,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("toggle_asr_school_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlDeenText(
                                text = "Asr: ${juristicSchool.displayName} (${if (juristicSchool == JuristicSchool.HANAFI) "حنفی" else "شافعی"}) • Tap to change",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                AlDeenText(
                    text = "أَوْقَاتُ الصَّلَاةِ",
                    style = AlDeenTypography.ArabicHeading.copy(
                        color = EmeraldPrimary,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Primary 5 Fard Prayers displayed in a single horizontal row of vertical blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                primaryPrayers.forEach { (prayer, time) ->
                    val asrSubtitle = if (prayer == PrayerType.ASR) {
                        if (juristicSchool == JuristicSchool.HANAFI) "Hanafi" else "Shafi'i"
                    } else null

                    PrayerBlockItem(
                        prayerType = prayer,
                        timeFormatted = formatTime(time),
                        isActive = activePrayer == prayer,
                        subtitle = asrSubtitle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Details button below the row of prayer blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    onClick = { isExpanded = !isExpanded },
                    shape = AlDeenTokens.ShapePill,
                    color = if (isExpanded) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isExpanded) EmeraldPrimary else MintBorder),
                    modifier = Modifier.testTag("today_prayer_detail_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Details" else "Details",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Hide Details" else "Show Details",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 3. Expandable additional prayers with the EXACT SAME vertical block interface
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(AlDeenTokens.AnimationDurationMedium)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(AlDeenTokens.AnimationDurationMedium)) + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MintBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Additional & Nafl Times",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Same row of vertical blocks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        additionalPrayers.forEach { (prayer, time) ->
                            PrayerBlockItem(
                                prayerType = prayer,
                                timeFormatted = formatTime(time),
                                isActive = activePrayer == prayer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vertical Prayer Block representing an individual prayer time
 * cleanly arranged for single-row multi-column display
 */
@Composable
private fun PrayerBlockItem(
    prayerType: PrayerType,
    timeFormatted: String,
    isActive: Boolean,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundModifier = if (isActive) {
        Modifier.background(ActivePrayerGradient)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    }

    val borderStroke = if (isActive) {
        BorderStroke(1.5.dp, EmeraldPrimary)
    } else {
        BorderStroke(1.dp, MintBorder)
    }

    Surface(
        modifier = modifier
            .clip(AlDeenTokens.ShapeSmallCard)
            .then(backgroundModifier)
            .testTag("prayer_block_${prayerType.name.lowercase()}"),
        shape = AlDeenTokens.ShapeSmallCard,
        color = Color.Transparent,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Prayer English Name
            Text(
                text = prayerType.displayName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive) EmeraldPrimary else TextPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Urdu / Arabic Name
            AlDeenText(
                text = prayerType.urduName,
                style = AlDeenTypography.UrduBody.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) EmeraldPrimary else TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                ),
                targetScript = ScriptLanguage.URDU,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Time Formatted
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) EmeraldPrimary else TextPrimary,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )

            if (subtitle != null) {
                Text(
                    text = "($subtitle)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isActive) EmeraldPrimary else TextSecondary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }

            // Active Badge
            Spacer(modifier = Modifier.height(4.dp))
            if (isActive) {
                Surface(
                    shape = AlDeenTokens.ShapePill,
                    color = EmeraldPrimary
                ) {
                    Text(
                        text = "NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(if (subtitle != null) 3.dp else 13.dp))
            }
        }
    }
}
