package com.example.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackState
import com.example.data.repository.SettingsRepository
import com.example.domain.model.Ayah
import com.example.domain.model.QuranReciter
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    surahNumber: Int? = null,
    scrollToAyah: Int? = null
) {
    val surah by viewModel.currentSurah.collectAsStateWithLifecycle()
    val ayahs by viewModel.currentAyahs.collectAsStateWithLifecycle()
    val audioState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val isOpenedFromPara by viewModel.isOpenedFromPara.collectAsStateWithLifecycle()
    val selectedReciter by viewModel.selectedReciter.collectAsStateWithLifecycle()
    val downloadedRecitersMap by viewModel.downloadedRecitersMap.collectAsStateWithLifecycle()
    val downloadingKeys by viewModel.downloadingKeys.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()
    val downloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val selectedTranslationId by viewModel.selectedTranslationId.collectAsStateWithLifecycle()
    val activeLanguage = viewModel.supportedTranslationLanguages.firstOrNull { it.id == selectedTranslationId }
    val bookmarkedRefs by viewModel.bookmarkedRefs.collectAsStateWithLifecycle()
    val translationRevision by viewModel.translationRevision.collectAsStateWithLifecycle()

    var showDownloadSheet by remember { mutableStateOf(false) }
    var showReciterSelectionSheet by remember { mutableStateOf(false) }
    var showTranslationSheet by remember { mutableStateOf(false) }
    var selectedForDownload by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteAllAudioDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val arabicFontSize by settingsRepository.quranArabicFontSize.collectAsStateWithLifecycle()
    val transFontSize by settingsRepository.quranTransFontSize.collectAsStateWithLifecycle()
    var showFontSizeSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(surahNumber) {
        if (surahNumber != null && viewModel.currentSurahNumber.value != surahNumber) {
            viewModel.openSurah(surahNumber, fromPara = false)
        }
    }

    LaunchedEffect(ayahs, scrollToAyah, surah) {
        if (scrollToAyah != null && ayahs.isNotEmpty()) {
            val index = ayahs.indexOfFirst { it.numberInSurah == scrollToAyah }
            if (index != -1) {
                // Account for the Bismillah header if present (Surah 9 has no Bismillah header item)
                val targetIndex = if (surah?.number != 9) index + 1 else index
                kotlinx.coroutines.delay(120)
                listState.scrollToItem(targetIndex)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    var reciterToDelete by remember { mutableStateOf<QuranReciter?>(null) }

    val surahNumber = surah?.number ?: 1

    LaunchedEffect(surahNumber, selectedTranslationId) {
        viewModel.ensureTranslationLoadedForSurah(surahNumber, selectedTranslationId)
    }

    val downloadedReciters = downloadedRecitersMap[surahNumber] ?: emptySet()
    val areAllDownloaded = surah != null && viewModel.areAllRecitersDownloaded(surahNumber)
    val hasAnyDownloaded = downloadedReciters.isNotEmpty()

    val isSurahPlaying = surah != null && audioState is AudioPlaybackState.Playing &&
            ((audioState as AudioPlaybackState.Playing).trackId == "surah_${surahNumber}_${selectedReciter.id}" ||
                    (audioState as AudioPlaybackState.Playing).trackId == "surah_${surahNumber}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AlDeenText(
                                text = surah?.let { "${it.englishName} (${it.urduName})" } ?: "Surah Reader",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                            // Tick mark when audio is downloaded
                            if (hasAnyDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Audio Downloaded",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Text(
                            text = surah?.let { "${it.revelationType} • ${it.numberOfAyahs} Verses" } ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Quran list",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Font Settings Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MintBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { showFontSizeSheet = true }
                                .testTag("reader_font_settings_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Font Settings",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Translate Toggle in Reader Top Bar (Circle Button with Large Icon)
                        Surface(
                            shape = CircleShape,
                            color = if (isTranslationEnabled) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, if (isTranslationEnabled) EmeraldPrimary else MintBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { showTranslationSheet = true }
                                .padding(end = 4.dp)
                                .testTag("reader_translation_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translation & Language Settings",
                                    tint = if (isTranslationEnabled) EmeraldPrimary else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Persistent Audio Bottom Overlay Bar (har time us surah mai rehta hai)
            if (surah != null) {
                val isCurrentReciterDownloaded = viewModel.isReciterDownloaded(surah!!.number, selectedReciter.id)
                SurahAudioBottomOverlay(
                    surah = surah!!,
                    selectedReciter = selectedReciter,
                    isPlaying = isSurahPlaying,
                    isCurrentReciterDownloaded = isCurrentReciterDownloaded,
                    areAllDownloaded = areAllDownloaded,
                    downloadedCount = downloadedReciters.size,
                    totalCount = viewModel.supportedReciters.size,
                    onPlayToggle = { viewModel.playSurahAudio(surah!!, selectedReciter) },
                    onOpenReciterSelector = { showReciterSelectionSheet = true },
                    onOpenDownloadMenu = { showDownloadSheet = true },
                    onDeleteAll = { showDeleteAllAudioDialog = true }
                )
            }
        },
        modifier = modifier.testTag("surah_reader_screen")
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingLarge)
        ) {
            // Bismillah Header Card (Surah 9 At-Tawbah does not have Bismillah)
            if (surah != null && surah!!.number != 9) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AlDeenTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AlDeenText(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = AlDeenTypography.QuranAyahText.copy(
                                    color = EmeraldPrimary,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }

            // Loading indicator while loading verses
            if (ayahs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading verses...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }

            // Ayahs list (Sada Quran mode when isTranslationEnabled == false, Word/Line translation when true)
            items(ayahs, key = { it.number }) { ayah ->
                val isAyahPlaying = audioState is AudioPlaybackState.Playing &&
                        (audioState as AudioPlaybackState.Playing).trackId == "ayah_${ayah.number}"
                val isBookmarked = surah?.let { bookmarkedRefs.contains("surah_${it.number}_ayah_${ayah.numberInSurah}") } ?: false
                val isTarget = (scrollToAyah != null && ayah.numberInSurah == scrollToAyah)
                val currentRevision = translationRevision
                val ayahTranslation = viewModel.getAyahTranslation(
                    surahNumber = surah?.number ?: 1,
                    ayahNumberInSurah = ayah.numberInSurah,
                    fallbackUrdu = ayah.urduTranslation,
                    fallbackEnglish = ayah.textTranslation
                )

                AyahCard(
                    ayah = ayah,
                    isPlaying = isAyahPlaying,
                    isBookmarked = isBookmarked,
                    isTranslationEnabled = isTranslationEnabled,
                    activeTranslation = ayahTranslation,
                    activeLanguageName = activeLanguage?.nativeName ?: "ترجمہ",
                    arabicFontSize = arabicFontSize,
                    transFontSize = transFontSize,
                    isHighlighted = isTarget,
                    onPlayClick = { viewModel.playAyahAudio(ayah) },
                    onBookmarkClick = { surah?.let { viewModel.toggleAyahBookmark(ayah, it) } },
                    onDownloadTranslation = { viewModel.downloadQuranTranslation(selectedTranslationId) }
                )
            }

            // Next Surah Button at bottom (ONLY shown when opened from Para section, removed for Surah section)
            if (isOpenedFromPara && surah != null && surah!!.number < 114) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.goToNextSurah() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("next_surah_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = AlDeenTokens.ShapePill
                    ) {
                        Text(
                            text = "Next: Surah ${surah!!.number + 1}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Audio Delete Warning Dialog (All Reciters)
    if (showDeleteAllAudioDialog && surah != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAllAudioDialog = false },
            title = {
                Text(
                    text = "Delete Audio Recitations?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all downloaded audio recitations for Surah ${surah!!.englishName}? You will need to download them again for offline playback.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllRecitersAudio(surah!!.number)
                        showDeleteAllAudioDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllAudioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Delete Warning Dialog (Single Reciter)
    if (reciterToDelete != null && surah != null) {
        val targetReciter = reciterToDelete!!
        AlertDialog(
            onDismissRequest = { reciterToDelete = null },
            title = {
                Text(
                    text = "Delete Reciter Audio?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the audio recitation by ${targetReciter.englishName} for Surah ${surah!!.englishName}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReciterAudio(surah!!.number, targetReciter.id)
                        reciterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reciterToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom Sheet 1: Download Recitations Menu
    if (showDownloadSheet && surah != null) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AlDeenText(
                            text = "Download Recitations (تلاوت ڈاؤن لوڈ)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${surah!!.englishName} • ${downloadedReciters.size}/${viewModel.supportedReciters.size} Downloaded",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = { showDownloadSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select All / Deselect All Action Row
                val nonDownloadedReciters = viewModel.supportedReciters.filter { !viewModel.isReciterDownloaded(surah!!.number, it.id) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select reciters to download:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    )

                    if (nonDownloadedReciters.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedForDownload = if (selectedForDownload.size == nonDownloadedReciters.size) {
                                    emptySet()
                                } else {
                                    nonDownloadedReciters.map { it.id }.toSet()
                                }
                            }
                        ) {
                            AlDeenText(
                                text = if (selectedForDownload.size == nonDownloadedReciters.size) "Deselect All" else "Select All (سب منتخب کریں)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MintBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Reciters List with Selection & Progress Tracking
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.supportedReciters, key = { it.id }) { reciter ->
                        val isDownloaded = viewModel.isReciterDownloaded(surah!!.number, reciter.id)
                        val isDownloading = viewModel.isReciterDownloading(surah!!.number, reciter.id)
                        val key = "${surah!!.number}_${reciter.id}"
                        val progress = downloadProgressMap[key] ?: 0f
                        val statusText = downloadStatusMap[key] ?: if (isDownloading) "Downloading..." else ""
                        val isChecked = selectedForDownload.contains(reciter.id)

                        Surface(
                            shape = AlDeenTokens.ShapeSmallCard,
                            color = if (isDownloaded) EmeraldContainer.copy(alpha = 0.35f) else Color.Transparent,
                            border = BorderStroke(
                                0.8.dp,
                                if (isDownloaded) EmeraldPrimary.copy(alpha = 0.5f)
                                else if (isChecked) EmeraldPrimary
                                else MintBorder.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isDownloaded && !isDownloading) {
                                    selectedForDownload = if (isChecked) {
                                        selectedForDownload - reciter.id
                                    } else {
                                        selectedForDownload + reciter.id
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Checkbox for selection (if not already downloaded)
                                        if (!isDownloaded && !isDownloading) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedForDownload = if (checked) {
                                                        selectedForDownload + reciter.id
                                                    } else {
                                                        selectedForDownload - reciter.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = reciter.englishName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                )
                                                if (isDownloaded) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Downloaded",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            AlDeenText(
                                                text = "${reciter.name} • ${reciter.style}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                    }

                                    // Right Action Button / Status
                                    if (isDownloaded) {
                                        Button(
                                            onClick = { reciterToDelete = reciter },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFDC2626), // Red
                                                contentColor = Color.White
                                            ),
                                            shape = AlDeenTokens.ShapePill,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    } else if (isDownloading) {
                                        Text(
                                            text = statusText.ifBlank { "Downloading..." },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }

                                // Active progress bar when downloading
                                if (isDownloading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = EmeraldPrimary,
                                        trackColor = MintBorder.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Dedicated Download Button
                val selectedRecitersToDownload = viewModel.supportedReciters.filter { selectedForDownload.contains(it.id) }
                Button(
                    onClick = {
                        if (selectedRecitersToDownload.isNotEmpty()) {
                            viewModel.downloadSelectedReciters(surah!!.number, selectedRecitersToDownload)
                            selectedForDownload = emptySet()
                        } else {
                            // If none selected, download all non-downloaded
                            viewModel.downloadAllReciters(surah!!.number)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("download_selected_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    AlDeenText(
                        text = if (selectedRecitersToDownload.isNotEmpty()) {
                            "Download Selected (${selectedRecitersToDownload.size}) • ڈاؤن لوڈ کریں"
                        } else {
                            "Download All Recitations • سب ڈاؤن لوڈ کریں"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }

    // Bottom Sheet 2: Reciter Selection Menu
    // User mandate: only selection here; clicking download icon immediately closes this sheet and opens the download sheet!
    if (showReciterSelectionSheet && surah != null) {
        ModalBottomSheet(
            onDismissRequest = { showReciterSelectionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AlDeenText(
                            text = "Select Reciter (قاری منتخب کریں)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Choose Qari for Surah recitation",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = { showReciterSelectionSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MintBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(viewModel.supportedReciters, key = { it.id }) { reciter ->
                        val isSelected = selectedReciter.id == reciter.id
                        val isDownloaded = viewModel.isReciterDownloaded(surah!!.number, reciter.id)
                        val isDownloading = viewModel.isReciterDownloading(surah!!.number, reciter.id)

                        Surface(
                            shape = AlDeenTokens.ShapeSmallCard,
                            color = if (isSelected) EmeraldContainer else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MintBorder.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedReciter(reciter)
                                    showReciterSelectionSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setSelectedReciter(reciter)
                                            showReciterSelectionSheet = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = reciter.englishName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = TextPrimary
                                            )
                                        )
                                        AlDeenText(
                                            text = "${reciter.name} (${reciter.style})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            ),
                                            targetScript = ScriptLanguage.ARABIC
                                        )
                                    }
                                }

                                // Status / Download Icon
                                // When user clicks download icon: closes selector and opens download sheet immediately!
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded Offline",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = EmeraldPrimary
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                // User directive: direct Windows close ho or download wali Windows open ho jaye
                                                showReciterSelectionSheet = false
                                                showDownloadSheet = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Open Download Screen",
                                                tint = EmeraldPrimary,
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
    }

    if (showTranslationSheet) {
        QuranTranslationBottomSheet(
            viewModel = viewModel,
            onDismiss = { showTranslationSheet = false }
        )
    }

    if (showFontSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFontSizeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            QuranFontSizeBottomSheet(
                arabicFontSize = arabicFontSize,
                transFontSize = transFontSize,
                onArabicFontSizeChange = { settingsRepository.setQuranArabicFontSize(it) },
                onTransFontSizeChange = { settingsRepository.setQuranTransFontSize(it) },
                onDismiss = { showFontSizeSheet = false },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

/**
 * Persistent Audio Bottom Overlay Bar (Bottom Menu in Surah Reader)
 */
@Composable
private fun SurahAudioBottomOverlay(
    surah: Surah,
    selectedReciter: QuranReciter,
    isPlaying: Boolean,
    isCurrentReciterDownloaded: Boolean,
    areAllDownloaded: Boolean,
    downloadedCount: Int,
    totalCount: Int,
    onPlayToggle: () -> Unit,
    onOpenReciterSelector: () -> Unit,
    onOpenDownloadMenu: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Play / Pause Button (Left Side) - Disabled until current reciter's audio is downloaded!
            Button(
                onClick = onPlayToggle,
                enabled = isCurrentReciterDownloaded,
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp)
                    .testTag("reader_play_pause_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = EmeraldPrimary.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = AlDeenTokens.ShapePill,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlaying) "Pause" else if (isCurrentReciterDownloaded) "Play" else "Play",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }

            // 2. Reciter Selection Button (Center)
            OutlinedButton(
                onClick = onOpenReciterSelector,
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .testTag("reader_reciter_select_button"),
                border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                shape = AlDeenTokens.ShapePill,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Select Reciter",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedReciter.englishName.split(" ").firstOrNull() ?: "Qari",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            // 3. Download / Delete Button (Right Side)
            // "button tab tak delete mai convert ni krna jab tak sab reciters ki audio download naa ho jaye"
            if (areAllDownloaded) {
                // When all reciters are downloaded -> Convert to DELETE button with RED color!
                Button(
                    onClick = onDeleteAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("reader_delete_all_audios_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626), // RED
                        contentColor = Color.White
                    ),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete All Audios",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                // Download button -> Opens download menu with all reciters & "Download All"
                OutlinedButton(
                    onClick = onOpenDownloadMenu,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("reader_download_menu_button"),
                    border = BorderStroke(1.2.dp, EmeraldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Audios",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (downloadedCount > 0) "$downloadedCount/$totalCount" else "Download",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = EmeraldPrimary
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyahCard(
    ayah: Ayah,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    isTranslationEnabled: Boolean,
    activeTranslation: String,
    activeLanguageName: String,
    arabicFontSize: Float = 24f,
    transFontSize: Float = 13f,
    isHighlighted: Boolean = false,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDownloadTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_item_${ayah.numberInSurah}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isHighlighted) 2.dp else AlDeenTokens.CardBorderWidth,
            if (isHighlighted) EmeraldPrimary else MintBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            // Header Row: Ayah Number + Play + Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah Number Badge & Highlight Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isHighlighted) EmeraldPrimary else EmeraldContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${ayah.numberInSurah}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighlighted) Color.White else OnEmeraldContainer,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                    if (isHighlighted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = GoldAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = "Searched Ayah",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Play
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (isPlaying) "Pause Ayah Audio" else "Play Ayah Audio",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Bookmark
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Ayah",
                            tint = if (isBookmarked) GoldAccent else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic text (Sada Quran display: Clean, elegant, readable)
            AlDeenText(
                text = ayah.textArabic,
                style = AlDeenTypography.QuranAyahText.copy(fontSize = arabicFontSize.sp),
                targetScript = ScriptLanguage.ARABIC,
                modifier = Modifier.fillMaxWidth()
            )

            // When isTranslationEnabled == true: show Word-by-Word translation AND full authentic translation
            if (isTranslationEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MintBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Word-by-Word Translation Grid/Flow (Right to Left / RTL alignment)
                if (ayah.words.isNotEmpty()) {
                    AlDeenText(
                        text = "لفظ بہ لفظ ترجمہ (Word by Word):",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        textAlign = TextAlign.Start
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ayah_word_by_word_${ayah.numberInSurah}"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ayah.words.filter { !it.isVerseEndMarker }.forEach { word ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(0.8.dp, MintBorder),
                                    modifier = Modifier.padding(1.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        val isUrdu = activeLanguageName.contains("اردو") || activeLanguageName.contains("Urdu")
                                        val isEnglish = activeLanguageName.contains("English")
                                        val wordMeaning = if (isUrdu) {
                                            word.urduTranslation.ifBlank { word.activeTranslation }
                                        } else if (isEnglish) {
                                            word.englishTranslation.ifBlank { word.activeTranslation }.ifBlank { word.urduTranslation }
                                        } else {
                                            word.activeTranslation.ifBlank { word.englishTranslation }.ifBlank { word.urduTranslation }
                                        }
                                        AlDeenText(
                                            text = word.textUthmani,
                                            style = AlDeenTypography.QuranAyahText.copy(fontSize = (arabicFontSize * 0.7f).coerceIn(16f, 28f).sp),
                                            color = TextPrimary,
                                            targetScript = ScriptLanguage.ARABIC,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (wordMeaning.isNotBlank()) {
                                            AlDeenText(
                                                text = wordMeaning,
                                                style = if (isUrdu) {
                                                    AlDeenTypography.UrduBody.copy(
                                                        fontSize = transFontSize.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldPrimary
                                                    )
                                                } else {
                                                    MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = transFontSize.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = EmeraldPrimary
                                                    )
                                                },
                                                targetScript = if (isUrdu) ScriptLanguage.URDU else if (isEnglish) ScriptLanguage.ENGLISH else null,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MintBorder.copy(alpha = 0.4f), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Translation Display in selected language
                val isUrdu = activeLanguageName.contains("اردو") || activeLanguageName.contains("Urdu")
                val displayText = activeTranslation
                if (displayText.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldContainer.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .border(BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.25f)), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        AlDeenText(
                            text = if (isUrdu) "بامحاورہ ترجمہ ($activeLanguageName)" else "Translation ($activeLanguageName)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AlDeenText(
                            text = displayText,
                            style = if (isUrdu) {
                                AlDeenTypography.UrduBody.copy(
                                    fontSize = transFontSize.sp,
                                    lineHeight = (transFontSize * 1.8f).sp,
                                    color = TextPrimary
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = transFontSize.sp,
                                    lineHeight = (transFontSize * 1.6f).sp,
                                    color = TextPrimary
                                )
                            },
                            targetScript = if (isUrdu) ScriptLanguage.URDU else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Translation for $activeLanguageName is not downloaded or unavailable.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onDownloadTranslation,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = AlDeenTokens.ShapePill,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download $activeLanguageName Translation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Line-wise English Translation (when selected language is not English)
                if (activeLanguageName != "English" && ayah.textTranslation.isNotBlank()) {
                    Text(
                        text = ayah.textTranslation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = transFontSize.sp,
                            lineHeight = (transFontSize * 1.5f).sp,
                            color = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
