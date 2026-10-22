package com.example.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RecentAudioSurah
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranRecentAudioBottomSheet(
    recentList: List<RecentAudioSurah>,
    currentPlayingSurahNumber: Int,
    isPlaying: Boolean,
    onSelectRecent: (RecentAudioSurah) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = EmeraldPrimary.copy(alpha = 0.5f)) },
        modifier = Modifier.testTag("recent_audio_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Recent Recitations",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        AlDeenText(
                            text = "حالیہ تلاوت شدہ سورتیں",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                if (recentList.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_recent_audio_btn")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Recent Recitations",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Play any Surah from the player to quickly access it here.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(recentList, key = { "${it.surahNumber}_${it.reciterId}" }) { item ->
                        val isItemCurrent = (item.surahNumber == currentPlayingSurahNumber)
                        val isItemActive = isItemCurrent && isPlaying

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectRecent(item) }
                                .testTag("recent_surah_item_${item.surahNumber}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isItemCurrent) EmeraldContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isItemCurrent) EmeraldPrimary.copy(alpha = 0.7f) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Surah Number circular badge
                                Surface(
                                    shape = CircleShape,
                                    color = if (isItemCurrent) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${item.surahNumber}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isItemCurrent) Color.White else TextPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.surahEnglishName.ifBlank { "Surah ${item.surahNumber}" },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isItemCurrent) EmeraldPrimary else TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.surahArabicName.isNotBlank()) {
                                            AlDeenText(
                                                text = item.surahArabicName,
                                                style = AlDeenTypography.ArabicSubheading.copy(
                                                    color = EmeraldPrimary,
                                                    fontSize = 15.sp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = item.reciterName.ifBlank { "Mishary Alafasy" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Play / Playing indicator action button
                                IconButton(
                                    onClick = { onSelectRecent(item) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isItemActive) EmeraldPrimary else EmeraldContainer,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isItemActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isItemActive) "Pause" else "Play",
                                        tint = if (isItemActive) Color.White else EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
