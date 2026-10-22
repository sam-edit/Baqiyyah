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
import com.example.domain.model.QuranReciter
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReciterAudioBottomSheet(
    surah: Surah,
    reciters: List<QuranReciter>,
    selectedReciter: QuranReciter,
    downloadedRecitersMap: Map<Int, Set<String>>,
    downloadingKeys: Set<String>,
    downloadProgressMap: Map<String, Float>,
    downloadStatusMap: Map<String, String>,
    onSelectReciter: (QuranReciter) -> Unit,
    onDownloadReciter: (QuranReciter) -> Unit,
    onDeleteReciterAudio: (QuranReciter) -> Unit,
    onDownloadAllReciters: () -> Unit,
    onDismiss: () -> Unit
) {
    val downloadedForThisSurah = downloadedRecitersMap[surah.number] ?: emptySet()
    val areAllDownloaded = reciters.all { reciter ->
        downloadedForThisSurah.contains(reciter.id) ||
        (reciter.id == "ar.alafasy" && downloadedForThisSurah.contains("ar.alafasy"))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = EmeraldPrimary.copy(alpha = 0.5f)) },
        modifier = Modifier.testTag("reciter_audio_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
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
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Select Reciter & Audio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                if (!areAllDownloaded) {
                    FilledTonalButton(
                        onClick = onDownloadAllReciters,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldContainer,
                            contentColor = EmeraldPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("download_all_reciters_btn")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download All",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Surah Context Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = EmeraldPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Surah ${surah.number}: ${surah.englishName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = "${surah.numberOfAyahs} Ayahs • ${surah.revelationType}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    AlDeenText(
                        text = surah.arabicName,
                        style = AlDeenTypography.ArabicSubheading.copy(
                            color = EmeraldPrimary,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reciters List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(reciters, key = { it.id }) { reciter ->
                    val isSelected = (reciter.id == selectedReciter.id)
                    val isDownloaded = downloadedForThisSurah.contains(reciter.id) ||
                            (reciter.id == "ar.alafasy" && downloadedForThisSurah.contains("ar.alafasy"))
                    val downloadKey = "${surah.number}_${reciter.id}"
                    val isDownloading = downloadingKeys.contains(downloadKey)
                    val progress = downloadProgressMap[downloadKey] ?: 0f
                    val statusText = downloadStatusMap[downloadKey] ?: ""

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectReciter(reciter)
                            }
                            .testTag("reciter_item_${reciter.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) EmeraldContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Microphone avatar
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = reciter.englishName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) EmeraldPrimary else TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = reciter.style,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    AlDeenText(
                                        text = reciter.name,
                                        style = AlDeenTypography.ArabicSubheading.copy(
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Status action (Downloaded badge, Download button, or Progress)
                                when {
                                    isDownloading -> {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(28.dp),
                                            color = EmeraldPrimary,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                    isDownloaded -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldContainer
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Downloaded",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isSelected) "Active" else "Ready",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = EmeraldPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }

                                            // Delete icon to manage storage
                                            IconButton(
                                                onClick = { onDeleteReciterAudio(reciter) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Recording",
                                                    tint = Color(0xFFE57373),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // Not downloaded: Show download icon
                                        IconButton(
                                            onClick = { onDownloadReciter(reciter) },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(EmeraldContainer, CircleShape)
                                                .testTag("download_reciter_${reciter.id}_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Download Recording",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // If downloading, show status percentage line
                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (statusText.isNotBlank()) statusText else "Downloading...",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = EmeraldContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
