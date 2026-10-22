package com.example.ui.quran

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.audio.AudioPlaybackState
import com.example.audio.AudioRepeatMode
import com.example.domain.model.QuranReciter
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

fun formatAudioDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun QuranFloatingAudioPlayer(
    currentSurah: Surah,
    selectedReciter: QuranReciter,
    isDownloaded: Boolean,
    hasAnyAudioDownloaded: Boolean = isDownloaded,
    isDownloading: Boolean,
    downloadProgress: Float,
    audioPlaybackState: AudioPlaybackState,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    repeatMode: AudioRepeatMode,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onPreviousSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenRecentSheet: () -> Unit,
    onOpenSurahPickerSheet: () -> Unit,
    onOpenReciterSheet: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = audioPlaybackState is AudioPlaybackState.Playing
    val isLoading = audioPlaybackState is AudioPlaybackState.Loading

    // Local state for smooth dragging of slider
    var isDraggingSlider by remember { mutableStateOf(false) }
    var draggedPositionMs by remember { mutableStateOf(0L) }

    val effectivePosition = if (isDraggingSlider) draggedPositionMs else currentPositionMs
    val effectiveDuration = if (durationMs > 0L) durationMs else 1L
    val sliderValue = (effectivePosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("quran_floating_audio_player"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (!isExpanded) {
            // ================= COLLAPSED MINI PLAYER BAR =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small circular play/pause button (always playable live with progressive cache)
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(38.dp)
                            .background(EmeraldPrimary, CircleShape)
                            .testTag("mini_player_play_pause")
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            isPlaying -> {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${currentSurah.number}. ${currentSurah.englishName}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AlDeenText(
                                text = currentSurah.arabicName,
                                style = AlDeenTypography.ArabicSubheading.copy(
                                    color = EmeraldPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isDownloaded) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded Offline",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = "Live Streaming with Cache",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = "${selectedReciter.englishName} • ${formatAudioDuration(currentPositionMs)} / ${formatAudioDuration(durationMs)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Expand Icon Button
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "Expand Player",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Slim Progress Bar at bottom of mini player
                LinearProgressIndicator(
                    progress = { sliderValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = EmeraldPrimary,
                    trackColor = EmeraldContainer
                )
            }
        } else {
            // ================= FULL EXPANDED FLOATING PLAYER =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Row (Surah info, Reciter badge, minimize button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${currentSurah.number}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentSurah.englishName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AlDeenText(
                                    text = currentSurah.arabicName,
                                    style = AlDeenTypography.ArabicSubheading.copy(
                                        color = EmeraldPrimary,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = selectedReciter.englishName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleExpanded,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Minimize",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onClosePlayer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TOP SLIDER SECTION (As requested: "uper uska slider bhi chal raha ho or usko ham agy pichy bhi kr skyn")
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            isDraggingSlider = true
                            draggedPositionMs = (newVal * effectiveDuration).toLong()
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onSeekTo(draggedPositionMs)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("floating_audio_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = EmeraldContainer
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatAudioDuration(effectivePosition),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = formatAudioDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PLAYBACK CONTROLS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Toggle Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                onSetPlaybackSpeed(nextSpeed)
                            }
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = onSeekBackward,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous Surah
                    IconButton(
                        onClick = onPreviousSurah,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous Surah",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Central Play / Pause Button (Large FAB, always playable live with progressive cache)
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onPlayPauseToggle() }
                            .testTag("floating_player_play_pause_fab"),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                isLoading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                }
                                isPlaying -> {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Next Surah
                    IconButton(
                        onClick = onNextSurah,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next Surah",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = onSeekForward,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat Toggle Button
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                AudioRepeatMode.REPEAT_ONE -> Icons.Default.RepeatOne
                                AudioRepeatMode.REPEAT_ALL -> Icons.Default.Repeat
                                AudioRepeatMode.OFF -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != AudioRepeatMode.OFF) EmeraldPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Compact Download Banner (Always visible, sleek single row, reduced height/length)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, MintBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReciterSheet() }
                        .testTag("floating_player_download_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDownloaded) "Audio is Downloaded for offline Playback" else "Download Audio for offline Playback",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                        Text(
                            text = if (isDownloaded) "Manage" else "Download",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Divider line
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.8.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ================= THE 3 ACTION BUTTONS =================
                // (As explicitly requested by user:
                // "nichy 3 buttons ho like aik recent wala aik surah selection wala or usky sath qari select wala i mean reciter")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Recent (حالیہ)
                    FilledTonalButton(
                        onClick = onOpenRecentSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("floating_player_btn_recent"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Recent",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Button 2: Surah Selection (سورت)
                    FilledTonalButton(
                        onClick = onOpenSurahPickerSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("floating_player_btn_surah"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Surah",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Surah",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Button 3: Qari Select (قاری)
                    FilledTonalButton(
                        onClick = onOpenReciterSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("floating_player_btn_reciter"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Qari",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Qari",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Global host for the Floating Audio Player and Headphone FAB button across the 5 main sections:
 * Home, Quran, Hadeeth, Goals, More.
 */
@Composable
fun QuranGlobalFloatingPlayerHost(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val downloadedSurahs by viewModel.downloadedSurahs.collectAsStateWithLifecycle()
    val audioState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val selectedAudioSurahNumber by viewModel.selectedAudioSurahNumber.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val isFloatingAudioPlayerVisible by viewModel.isFloatingAudioPlayerVisible.collectAsStateWithLifecycle()
    val isFloatingAudioPlayerExpanded by viewModel.isFloatingAudioPlayerExpanded.collectAsStateWithLifecycle()
    val isRecentAudioSheetOpen by viewModel.isRecentAudioBottomSheetOpen.collectAsStateWithLifecycle()
    val isSurahPickerSheetOpen by viewModel.isSurahPickerBottomSheetOpen.collectAsStateWithLifecycle()
    val isReciterAudioSheetOpen by viewModel.isReciterAudioBottomSheetOpen.collectAsStateWithLifecycle()
    val recentAudioSurahs by viewModel.recentAudioSurahs.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.audioCurrentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.audioPlaybackSpeed.collectAsStateWithLifecycle()
    val repeatMode by viewModel.audioRepeatMode.collectAsStateWithLifecycle()
    val downloadingKeys by viewModel.downloadingKeys.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()
    val downloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()
    val downloadedRecitersMap by viewModel.downloadedRecitersMap.collectAsStateWithLifecycle()

    val activeAudioSurah = remember(selectedAudioSurahNumber, surahs) {
        surahs.find { it.number == selectedAudioSurahNumber } ?: surahs.firstOrNull() ?: Surah(
            number = 1,
            englishName = "Al-Fatihah",
            urduName = "الفاتحہ",
            arabicName = "سُورَةُ الْفَاتِحَةِ",
            englishTranslation = "The Opening",
            revelationType = "Meccan",
            numberOfAyahs = 7,
            rukuCount = 1
        )
    }
    val isAudioDownloaded = viewModel.isReciterDownloaded(activeAudioSurah.number, selectedReciter.id)
    val hasAnyAudioDownloaded = downloadedSurahs.contains(activeAudioSurah.number) || (downloadedRecitersMap[activeAudioSurah.number]?.isNotEmpty() == true)
    val downloadKey = "${activeAudioSurah.number}_${selectedReciter.id}"
    val isAudioDownloading = downloadingKeys.contains(downloadKey)
    val audioDownloadProgress = downloadProgressMap[downloadKey] ?: 0f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Audio Player pinned to Bottom
        AnimatedVisibility(
            visible = isFloatingAudioPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            QuranFloatingAudioPlayer(
                currentSurah = activeAudioSurah,
                selectedReciter = selectedReciter,
                isDownloaded = isAudioDownloaded,
                hasAnyAudioDownloaded = hasAnyAudioDownloaded,
                isDownloading = isAudioDownloading,
                downloadProgress = audioDownloadProgress,
                audioPlaybackState = audioState,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                repeatMode = repeatMode,
                isExpanded = isFloatingAudioPlayerExpanded,
                onToggleExpanded = { viewModel.toggleFloatingAudioPlayerExpanded() },
                onPlayPauseToggle = {
                    viewModel.toggleAudioPlayPause(activeAudioSurah.number, selectedReciter)
                },
                onPreviousSurah = { viewModel.playPreviousAudioSurah() },
                onNextSurah = { viewModel.playNextAudioSurah() },
                onSeekTo = { pos -> viewModel.seekAudioTo(pos) },
                onSeekForward = { viewModel.seekAudioForward() },
                onSeekBackward = { viewModel.seekAudioBackward() },
                onSetPlaybackSpeed = { spd -> viewModel.setPlaybackSpeed(spd) },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onOpenRecentSheet = { viewModel.setRecentAudioBottomSheetOpen(true) },
                onOpenSurahPickerSheet = { viewModel.setSurahPickerBottomSheetOpen(true) },
                onOpenReciterSheet = { viewModel.setReciterAudioBottomSheetOpen(true) },
                onClosePlayer = { viewModel.setFloatingAudioPlayerVisible(false) }
            )
        }

        // Draggable, edge-docking & peeking Floating Audio Button with edge-glow feedback
        if (!isFloatingAudioPlayerVisible) {
            DraggableFloatingAudioButton(
                onClick = { viewModel.setFloatingAudioPlayerVisible(true) }
            )
        }
    }

    // 1. Recent Audio BottomSheet
    if (isRecentAudioSheetOpen) {
        QuranRecentAudioBottomSheet(
            recentList = recentAudioSurahs,
            currentPlayingSurahNumber = activeAudioSurah.number,
            isPlaying = audioState is AudioPlaybackState.Playing,
            onSelectRecent = { recentItem ->
                val surah = surahs.find { it.number == recentItem.surahNumber }
                val reciter = viewModel.supportedReciters.find { it.id == recentItem.reciterId } ?: selectedReciter
                if (surah != null) {
                    viewModel.setSelectedReciter(reciter)
                    viewModel.playSurahAudio(surah, reciter)
                    viewModel.setFloatingAudioPlayerVisible(true)
                }
                viewModel.setRecentAudioBottomSheetOpen(false)
            },
            onClearAll = { viewModel.clearRecentAudios() },
            onDismiss = { viewModel.setRecentAudioBottomSheetOpen(false) }
        )
    }

    // 2. Surah Selection BottomSheet
    if (isSurahPickerSheetOpen) {
        QuranSurahPickerBottomSheet(
            surahs = surahs,
            selectedSurahNumber = activeAudioSurah.number,
            downloadedSurahs = downloadedSurahs,
            currentReciterDownloadedMap = downloadedRecitersMap,
            currentReciterId = selectedReciter.id,
            onSelectSurah = { surah ->
                viewModel.selectSurahForAudio(surah.number)
                viewModel.playSurahAudio(surah, selectedReciter)
                viewModel.setFloatingAudioPlayerVisible(true)
                viewModel.setSurahPickerBottomSheetOpen(false)
            },
            onDismiss = { viewModel.setSurahPickerBottomSheetOpen(false) }
        )
    }

    // 3. Qari (Reciter) Selection & Download Recordings BottomSheet
    if (isReciterAudioSheetOpen) {
        QuranReciterAudioBottomSheet(
            surah = activeAudioSurah,
            reciters = viewModel.supportedReciters,
            selectedReciter = selectedReciter,
            downloadedRecitersMap = downloadedRecitersMap,
            downloadingKeys = downloadingKeys,
            downloadProgressMap = downloadProgressMap,
            downloadStatusMap = downloadStatusMap,
            onSelectReciter = { reciter ->
                viewModel.setSelectedReciter(reciter)
                viewModel.playSurahAudio(activeAudioSurah, reciter)
                viewModel.setReciterAudioBottomSheetOpen(false)
            },
            onDownloadReciter = { reciter ->
                viewModel.downloadReciterAudio(activeAudioSurah.number, reciter)
            },
            onDeleteReciterAudio = { reciter ->
                viewModel.deleteReciterAudio(activeAudioSurah.number, reciter.id)
            },
            onDownloadAllReciters = {
                viewModel.downloadAllReciters(activeAudioSurah.number)
            },
            onDismiss = { viewModel.setReciterAudioBottomSheetOpen(false) }
        )
    }
}

enum class AudioDockSide { LEFT, RIGHT }

object FloatingAudioButtonHolder {
    var dockSide: AudioDockSide = AudioDockSide.RIGHT
    var isTucked: Boolean = false // Default on app open: OUT (bahir)
    var savedNormalizedY: Float = 0.88f // Kept lower near the bottom navigation bar

    fun resetToDefault() {
        dockSide = AudioDockSide.RIGHT
        isTucked = false
        savedNormalizedY = 0.88f
    }
}

/**
 * Draggable, dockable Floating Audio Button with edge-glowing target indicators
 * and auto-tucking / peeking behavior against the left or right screen edge.
 */
@Composable
fun DraggableFloatingAudioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    var dockSide by remember { mutableStateOf(FloatingAudioButtonHolder.dockSide) }
    var isTucked by remember { mutableStateOf(FloatingAudioButtonHolder.isTucked) }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(220),
        label = "edge_glow_alpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        val buttonSizeDp = 56.dp
        val buttonSizePx = with(density) { buttonSizeDp.toPx() }
        // 2/3 tucked inside the edge, 1/3 (approx 18-19dp) remains visible on screen
        val tuckedOffsetPx = buttonSizePx * (2f / 3f)
        val edgeMarginPx = with(density) { 16.dp.toPx() }
        val topBoundPx = with(density) { 60.dp.toPx() }
        // Positioned lower: sitting closely above the bottom navigation bar (~24dp above nav bar)
        val bottomNavMarginPx = with(density) { 24.dp.toPx() }
        val bottomBoundPx = (parentHeightPx - bottomNavMarginPx - buttonSizePx).coerceAtLeast(topBoundPx)
        val defaultY = if (FloatingAudioButtonHolder.savedNormalizedY > 0f && parentHeightPx > 0f) {
            (parentHeightPx * FloatingAudioButtonHolder.savedNormalizedY).coerceIn(topBoundPx, bottomBoundPx)
        } else {
            (parentHeightPx - bottomNavMarginPx - buttonSizePx).coerceIn(topBoundPx, bottomBoundPx)
        }

        val animX = remember {
            val initialX = if (dockSide == AudioDockSide.LEFT) {
                if (isTucked) -tuckedOffsetPx else edgeMarginPx
            } else {
                if (parentWidthPx > 0) {
                    if (isTucked) parentWidthPx - buttonSizePx + tuckedOffsetPx else parentWidthPx - buttonSizePx - edgeMarginPx
                } else 0f
            }
            Animatable(initialX)
        }
        val animY = remember {
            Animatable(if (parentHeightPx > 0) defaultY else 450f)
        }

        var initialized by remember { mutableStateOf(false) }

        LaunchedEffect(parentWidthPx, parentHeightPx) {
            if (parentWidthPx > 0 && parentHeightPx > 0 && !initialized) {
                initialized = true
                val startX = if (dockSide == AudioDockSide.LEFT) {
                    if (isTucked) -tuckedOffsetPx else edgeMarginPx
                } else {
                    if (isTucked) parentWidthPx - buttonSizePx + tuckedOffsetPx else parentWidthPx - buttonSizePx - edgeMarginPx
                }
                val startY = if (animY.value <= 0f || animY.value == 450f) defaultY else animY.value.coerceIn(topBoundPx, bottomBoundPx)
                animX.snapTo(startX)
                animY.snapTo(startY)
            }
        }

        // --- 1. Left and Right Edge Glowing Aura when dragging ---
        if (glowAlpha > 0.01f) {
            // Left Glow Target Area (full height)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                EmeraldPrimary.copy(alpha = 0.50f * glowAlpha),
                                EmeraldPrimary.copy(alpha = 0.18f * glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    color = GoldAccent.copy(alpha = 0.85f * glowAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(6.dp)
                        .fillMaxHeight(0.35f)
                ) {}
            }

            // Right Glow Target Area (full height)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                EmeraldPrimary.copy(alpha = 0.18f * glowAlpha),
                                EmeraldPrimary.copy(alpha = 0.50f * glowAlpha)
                            )
                        )
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    color = GoldAccent.copy(alpha = 0.85f * glowAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(6.dp)
                        .fillMaxHeight(0.35f)
                ) {}
            }
        }

        // --- 2. Draggable Floating Headphone Bubble ---
        var totalDragDistance by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animX.value.roundToInt(),
                        y = animY.value.roundToInt()
                    )
                }
                .pointerInput(parentWidthPx, parentHeightPx) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            totalDragDistance = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragDistance += kotlin.math.hypot(dragAmount.x, dragAmount.y)
                            scope.launch {
                                animX.snapTo(animX.value + dragAmount.x)
                                val newY = (animY.value + dragAmount.y).coerceIn(topBoundPx, bottomBoundPx)
                                animY.snapTo(newY)
                                if (parentHeightPx > 0f) {
                                    FloatingAudioButtonHolder.savedNormalizedY = newY / parentHeightPx
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (totalDragDistance < 12f) {
                                // Tap gesture detected
                                if (isTucked) {
                                    // CRITICAL: When tucked, clicking only brings it out to full view (no onClick action)
                                    isTucked = false
                                    FloatingAudioButtonHolder.isTucked = false
                                    scope.launch {
                                        val untuckedX = if (dockSide == AudioDockSide.LEFT) {
                                            edgeMarginPx
                                        } else {
                                            parentWidthPx - buttonSizePx - edgeMarginPx
                                        }
                                        animX.animateTo(
                                            targetValue = untuckedX,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                } else {
                                    // Only when already untucked/out does it trigger the audio player dialog
                                    onClick()
                                }
                            } else {
                                // User dragged it - determine target side (left or right)
                                val currentCenterX = animX.value + (buttonSizePx / 2f)
                                val targetSide = if (currentCenterX < parentWidthPx / 2f) AudioDockSide.LEFT else AudioDockSide.RIGHT
                                dockSide = targetSide
                                FloatingAudioButtonHolder.dockSide = targetSide

                                // Check if user dragged near or off either screen edge to tuck it
                                val isDraggedTowardsEdge = if (targetSide == AudioDockSide.LEFT) {
                                    animX.value <= edgeMarginPx * 1.5f
                                } else {
                                    animX.value >= parentWidthPx - buttonSizePx - edgeMarginPx * 1.5f
                                }

                                scope.launch {
                                    if (isDraggedTowardsEdge) {
                                        // Tuck 2/3 inside the edge, anywhere along the Y axis
                                        isTucked = true
                                        FloatingAudioButtonHolder.isTucked = true
                                        val tuckedX = if (targetSide == AudioDockSide.LEFT) {
                                            -tuckedOffsetPx
                                        } else {
                                            parentWidthPx - buttonSizePx + tuckedOffsetPx
                                        }
                                        animX.animateTo(
                                            targetValue = tuckedX,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    } else {
                                        // Snap to edge margin at the current height and keep it outside
                                        isTucked = false
                                        FloatingAudioButtonHolder.isTucked = false
                                        val snappedX = if (targetSide == AudioDockSide.LEFT) {
                                            edgeMarginPx
                                        } else {
                                            parentWidthPx - buttonSizePx - edgeMarginPx
                                        }
                                        animX.animateTo(
                                            targetValue = snappedX,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                val currentCenterX = animX.value + (buttonSizePx / 2f)
                                val targetSide = if (currentCenterX < parentWidthPx / 2f) AudioDockSide.LEFT else AudioDockSide.RIGHT
                                dockSide = targetSide
                                FloatingAudioButtonHolder.dockSide = targetSide
                                val snappedX = if (targetSide == AudioDockSide.LEFT) edgeMarginPx else parentWidthPx - buttonSizePx - edgeMarginPx
                                animX.animateTo(snappedX, spring())
                            }
                        }
                    )
                }
        ) {
            Surface(
                onClick = {
                    if (isTucked) {
                        // CRITICAL: When tucked, tap strictly pops it out to 100% visible, does NOT trigger action
                        isTucked = false
                        FloatingAudioButtonHolder.isTucked = false
                        scope.launch {
                            val untuckedX = if (dockSide == AudioDockSide.LEFT) {
                                edgeMarginPx
                            } else {
                                parentWidthPx - buttonSizePx - edgeMarginPx
                            }
                            animX.animateTo(
                                targetValue = untuckedX,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    } else {
                        // Already untucked - regular click opens the floating player / sheet
                        onClick()
                    }
                },
                shape = CircleShape,
                color = EmeraldPrimary,
                contentColor = Color.White,
                shadowElevation = if (isDragging) 14.dp else if (isTucked) 3.dp else 8.dp,
                border = null, // Completely removed outline border as requested
                modifier = Modifier
                    .size(buttonSizeDp)
                    .testTag("reopen_floating_audio_player")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (isTucked) {
                        // When 2/3 tucked, align the visible 1/3 headphone icon so it's clearly peeking out
                        if (dockSide == AudioDockSide.LEFT) Alignment.CenterEnd else Alignment.CenterStart
                    } else Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Player",
                        modifier = Modifier
                            .padding(horizontal = if (isTucked) 4.dp else 0.dp)
                            .size(if (isTucked) 20.dp else 26.dp)
                    )

                    // Small golden status dot indicating it's active & can be tapped to restore
                    if (isTucked) {
                        Box(
                            modifier = Modifier
                                .align(if (dockSide == AudioDockSide.LEFT) Alignment.TopEnd else Alignment.TopStart)
                                .padding(
                                    top = 6.dp,
                                    start = if (dockSide == AudioDockSide.RIGHT) 5.dp else 0.dp,
                                    end = if (dockSide == AudioDockSide.LEFT) 5.dp else 0.dp
                                )
                                .size(6.dp)
                                .background(GoldAccent, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

