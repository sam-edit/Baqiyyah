package com.example.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackState
import com.example.audio.QuranAudioPlayer
import com.example.data.hadith.HadithDownloadManager
import com.example.data.hadith.HadithDownloadState
import com.example.data.quran.QuranTranslationManager
import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.BookmarksRepository
import com.example.data.repository.HadithRepository
import com.example.data.repository.QuranRepository
import com.example.domain.model.Ayah
import com.example.domain.model.Hadith
import com.example.domain.model.HadithBook
import com.example.domain.model.HadithSearchMode
import com.example.domain.model.QuranSearchMode
import com.example.domain.model.SearchLanguage
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.hadith.HadithBookCard
import com.example.ui.hadith.HadithViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SearchSection {
    QURAN, HADITH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    hadithViewModel: HadithViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
    onOpenHadithBook: (String) -> Unit,
    onNavigateToHadithSection: () -> Unit,
    onNavigateToQuranSection: () -> Unit = {},
    onOpenAyah: (Int, Int) -> Unit = { _, _ -> },
    onOpenHadith: (String, Int, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val quranRepo = remember { QuranRepository(context) }
    val translationManager = remember { QuranTranslationManager.getInstance(context) }
    val database = remember { com.example.data.local.AlDeenDatabase.getInstance(context) }
    val bookmarksRepo = remember { BookmarksRepository(database.bookmarkDao()) }
    val audioPlayer = remember { QuranAudioPlayer(context) }

    val audioState by audioPlayer.playbackState.collectAsStateWithLifecycle()
    val downloadedQuranLanguages by translationManager.downloadedLanguageCodes.collectAsStateWithLifecycle()
    val allBookmarks by bookmarksRepo.allBookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarkedRefs = remember(allBookmarks) { allBookmarks.map { it.referenceId }.toSet() }

    // Section Selection
    var selectedSection by remember { mutableStateOf(SearchSection.QURAN) }

    // Quran Search State
    var quranSearchQuery by remember { mutableStateOf("") }
    var quranMode by remember { mutableStateOf(QuranSearchMode.BY_WORD) }
    var quranLanguage by remember { mutableStateOf(SearchLanguage.URDU) }
    var showQuranSearchByDropdown by remember { mutableStateOf(false) }
    var showQuranLanguageDropdown by remember { mutableStateOf(false) }
    var isQuranSearching by remember { mutableStateOf(false) }
    var quranResults by remember { mutableStateOf<List<Pair<Surah, Ayah>>>(emptyList()) }
    var quranSearchJob by remember { mutableStateOf<Job?>(null) }

    // Hadith Search State (Main Menu Hadith Features)
    val hadithSearchQuery by hadithViewModel.searchQuery.collectAsStateWithLifecycle()
    val hadithMode by hadithViewModel.hadithMode.collectAsStateWithLifecycle()
    val hadithSearchLanguage by hadithViewModel.searchLanguage.collectAsStateWithLifecycle()
    val selectedBookFilter by hadithViewModel.selectedBookFilter.collectAsStateWithLifecycle()
    val hadithSearchResults by hadithViewModel.searchResults.collectAsStateWithLifecycle()
    val isHadithSearching by hadithViewModel.isSearching.collectAsStateWithLifecycle()
    val hadithSearchProgress by hadithViewModel.searchProgress.collectAsStateWithLifecycle()
    val downloadStatusMap by hadithViewModel.downloadStatusMap.collectAsStateWithLifecycle()
    val langStatusMap by hadithViewModel.languageDownloadStatusMap.collectAsStateWithLifecycle()

    var showBookFilterDropdown by remember { mutableStateOf(false) }
    var showHadithSearchByDropdown by remember { mutableStateOf(false) }
    var showHadithLanguageDropdown by remember { mutableStateOf(false) }
    var bookPromptForDownload by remember { mutableStateOf<HadithBook?>(null) }
    var bookToDelete by remember { mutableStateOf<HadithBook?>(null) }

    // Search Quran Execution
    fun executeQuranSearch(query: String) {
        quranSearchJob?.cancel()
        if (query.isBlank()) {
            quranResults = emptyList()
            isQuranSearching = false
            return
        }

        isQuranSearching = true
        quranSearchJob = coroutineScope.launch {
            delay(150)
            val results = withContext(Dispatchers.IO) {
                quranRepo.searchAyahsOffline(
                    context = context,
                    query = query,
                    searchByAyah = (quranMode == QuranSearchMode.BY_AYAH),
                    langCode = quranLanguage.code,
                    translationManager = translationManager
                )
            }
            quranResults = results
            isQuranSearching = false
        }
    }

    LaunchedEffect(quranSearchQuery, quranMode, quranLanguage) {
        executeQuranSearch(quranSearchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search Quran & Hadith",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("universal_search_screen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Section Tabs: Holy Quran vs Hadith
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(AlDeenTokens.ShapePill)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isQuran = selectedSection == SearchSection.QURAN
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AlDeenTokens.ShapePill)
                        .clickable { selectedSection = SearchSection.QURAN }
                        .testTag("search_section_quran"),
                    color = if (isQuran) EmeraldPrimary else Color.Transparent,
                    shape = AlDeenTokens.ShapePill
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (isQuran) Color.White else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Holy Quran",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isQuran) Color.White else TextSecondary
                            )
                        )
                    }
                }

                val isHadith = selectedSection == SearchSection.HADITH
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(AlDeenTokens.ShapePill)
                        .clickable { selectedSection = SearchSection.HADITH }
                        .testTag("search_section_hadith"),
                    color = if (isHadith) EmeraldPrimary else Color.Transparent,
                    shape = AlDeenTokens.ShapePill
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = if (isHadith) Color.White else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hadith",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isHadith) Color.White else TextSecondary
                            )
                        )
                    }
                }
            }

            // 2. Section Specific Content
            if (selectedSection == SearchSection.QURAN) {
                // Quran Search Field
                OutlinedTextField(
                    value = quranSearchQuery,
                    onValueChange = { quranSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("universal_search_input"),
                    placeholder = {
                        AlDeenText(
                            text = quranMode.hint,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = EmeraldPrimary
                        )
                    },
                    trailingIcon = {
                        if (quranSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { quranSearchQuery = "" },
                                modifier = Modifier.testTag("search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = AlDeenTokens.ShapePill,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MintBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search By and Language Selectors Side-by-Side for Quran
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: Search By Selector
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showQuranSearchByDropdown = true }
                                .testTag("search_by_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Search By",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                                    )
                                    Text(
                                        text = quranMode.label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Search By",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showQuranSearchByDropdown,
                            onDismissRequest = { showQuranSearchByDropdown = false }
                        ) {
                            QuranSearchMode.entries.forEach { mode ->
                                val selected = quranMode == mode
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = mode.label,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) EmeraldPrimary else TextPrimary
                                            )
                                            if (selected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        quranMode = mode
                                        showQuranSearchByDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Right: Language Selector
                    val isLanguageDisabled = (quranMode == QuranSearchMode.BY_AYAH)

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLanguageDisabled) 0.3f else 0.6f),
                            border = BorderStroke(1.dp, if (isLanguageDisabled) MintBorder.copy(alpha = 0.3f) else MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLanguageDisabled) { showQuranLanguageDropdown = true }
                                .testTag("search_language_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Language",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    AlDeenText(
                                        text = if (isLanguageDisabled) "Not Applicable" else "${quranLanguage.englishName} (${quranLanguage.nativeName})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.5f) else EmeraldPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Language",
                                    tint = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.4f) else EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showQuranLanguageDropdown,
                            onDismissRequest = { showQuranLanguageDropdown = false }
                        ) {
                            SearchLanguage.quranLanguages.forEach { lang ->
                                val selected = quranLanguage == lang
                                val isDownloaded = downloadedQuranLanguages.contains(lang.code) || translationManager.isLanguageCodeDownloaded(lang.code)

                                if (isDownloaded) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                AlDeenText(
                                                    text = "${lang.englishName} (${lang.nativeName})",
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selected) EmeraldPrimary else TextPrimary
                                                )
                                                if (selected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            quranLanguage = lang
                                            showQuranLanguageDropdown = false
                                        }
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        AlDeenText(
                                            text = "${lang.englishName} (${lang.nativeName})",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextSecondary.copy(alpha = 0.6f)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                showQuranLanguageDropdown = false
                                                coroutineScope.launch {
                                                    val trans = translationManager.supportedLanguages.firstOrNull { it.languageCode == lang.code }
                                                    if (trans != null) {
                                                        translationManager.downloadTranslation(trans.id)
                                                        Toast.makeText(context, "Downloading ${trans.englishName} Quran Translation...", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            shape = AlDeenTokens.ShapePill,
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    thickness = 0.8.dp,
                    color = MintBorder.copy(alpha = 0.6f)
                )

                // Results Header & Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (quranSearchQuery.isBlank()) "Offline Quran (All 114 Surahs & 6,236 Ayahs)"
                        else "Results: ${quranResults.size} Ayahs found",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (quranResults.isNotEmpty()) EmeraldPrimary else TextSecondary
                        )
                    )

                    if (isQuranSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                    }
                }

                // Quran Results List
                if (quranSearchQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type an Ayah number (e.g. 255 or 2:255) or keywords in ${quranLanguage.englishName}, Arabic, Urdu... Results update live as you type.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (quranResults.isEmpty() && !isQuranSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Quran results found for \"$quranSearchQuery\"",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary, fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("quran_search_results_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = quranResults,
                            key = { "${it.first.number}_${it.second.numberInSurah}" }
                        ) { (surah, ayah) ->
                            val refKey = "quran_${surah.number}_${ayah.numberInSurah}"
                            val isBookmarked = bookmarkedRefs.contains(refKey)
                            val trackKey = "ayah_${ayah.number}"
                            val isPlaying = (audioState as? AudioPlaybackState.Playing)?.trackId == trackKey

                            QuranSearchResultCard(
                                surah = surah,
                                ayah = ayah,
                                selectedLanguage = quranLanguage,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlaying,
                                isNumberSearch = (quranMode == QuranSearchMode.BY_AYAH),
                                onReadInSurah = { onOpenAyah(surah.number, ayah.numberInSurah) },
                                onPlayAudio = {
                                    val url = ayah.audioUrl ?: "https://cdn.islamic.network/quran/audio/128/ar.alafasy/${ayah.number}.mp3"
                                    audioPlayer.play(url, trackKey)
                                },
                                onToggleBookmark = {
                                    coroutineScope.launch {
                                        bookmarksRepo.toggleBookmark(
                                            com.example.domain.model.BookmarkItem(
                                                type = com.example.domain.model.BookmarkType.QURAN_AYAH,
                                                referenceId = refKey,
                                                title = "Surah ${surah.englishName} [${surah.number}:${ayah.numberInSurah}]",
                                                subtitle = surah.urduName,
                                                arabicText = ayah.textArabic,
                                                translation = ayah.textTranslation
                                            )
                                        )
                                    }
                                },
                                onCopy = {
                                    val textToCopy = "${ayah.textArabic}\n\n${ayah.textTranslation}\n[Surah ${surah.englishName} ${surah.number}:${ayah.numberInSurah}]"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Ayah", textToCopy))
                                    Toast.makeText(context, "Ayah copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // HADITH SECTION (EXACT MAIN MENU FEATURES)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // 1. Book Selection Option (All Books + Individual Books) Above the Two Filters
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBookFilterDropdown = true }
                                .testTag("hadith_book_filter_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        AlDeenText(
                                            text = "Book Selection (کتاب کا انتخاب)",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                                        )
                                        AlDeenText(
                                            text = selectedBookFilter?.name ?: "All Books (تمام کتب)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (selectedBookFilter != null) {
                                    val selBook = selectedBookFilter!!
                                    val bookState = downloadStatusMap[selBook.slug]
                                        ?: if (hadithViewModel.isBookDownloaded(selBook.slug)) HadithDownloadState.Downloaded else HadithDownloadState.NotDownloaded
                                    val isBookDownloaded = bookState is HadithDownloadState.Downloaded || hadithViewModel.isBookDownloaded(selBook.slug)
                                    val isBookDownloading = bookState is HadithDownloadState.Downloading
                                    val downloadProgress = (bookState as? HadithDownloadState.Downloading)?.progress ?: 0f

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        if (isBookDownloaded) {
                                            Surface(
                                                shape = AlDeenTokens.ShapePill,
                                                color = EmeraldContainer,
                                                border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Downloaded",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Downloaded",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = EmeraldPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        } else if (isBookDownloading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(AlDeenTokens.ShapePill)
                                                    .background(EmeraldContainer.copy(alpha = 0.5f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 2.dp,
                                                    color = EmeraldPrimary
                                                )
                                                Text(
                                                    text = "${(downloadProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = EmeraldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = { hadithViewModel.downloadBook(selBook.slug) },
                                                shape = AlDeenTokens.ShapePill,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = EmeraldContainer,
                                                    contentColor = EmeraldPrimary
                                                ),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download Book",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Download",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Book",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showBookFilterDropdown,
                            onDismissRequest = { showBookFilterDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            AlDeenText(
                                                text = "All Books (تمام کتبِ احادیث)",
                                                fontWeight = if (selectedBookFilter == null) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedBookFilter == null) EmeraldPrimary else TextPrimary
                                            )
                                            Text(
                                                text = "Search across all Kutub al-Sittah collections",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                            )
                                        }
                                        if (selectedBookFilter == null) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    hadithViewModel.setSelectedBookFilter(null)
                                    showBookFilterDropdown = false
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            hadithViewModel.books.forEach { book ->
                                val selected = selectedBookFilter?.slug == book.slug
                                val bookState = downloadStatusMap[book.slug]
                                    ?: if (hadithViewModel.isBookDownloaded(book.slug)) HadithDownloadState.Downloaded else HadithDownloadState.NotDownloaded
                                val isBookDownloaded = bookState is HadithDownloadState.Downloaded || hadithViewModel.isBookDownloaded(book.slug)
                                val isBookDownloading = bookState is HadithDownloadState.Downloading
                                val downloadProgress = (bookState as? HadithDownloadState.Downloading)?.progress ?: 0f

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = book.englishName,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selected) EmeraldPrimary else TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    AlDeenText(
                                                        text = book.arabicName,
                                                        style = AlDeenTypography.ArabicBody.copy(
                                                            color = if (selected) EmeraldPrimary else TextSecondary,
                                                            fontSize = 13.sp
                                                        ),
                                                        targetScript = ScriptLanguage.ARABIC
                                                    )
                                                }
                                                Text(
                                                    text = "${book.totalHadithCount} Ahadith",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isBookDownloaded) {
                                                    Surface(
                                                        shape = AlDeenTokens.ShapePill,
                                                        color = EmeraldContainer
                                                    ) {
                                                        Text(
                                                            text = "Downloaded",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = EmeraldPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isBookDownloading) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        modifier = Modifier
                                                            .clip(AlDeenTokens.ShapePill)
                                                            .background(EmeraldContainer.copy(alpha = 0.5f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(10.dp),
                                                            strokeWidth = 1.5.dp,
                                                            color = EmeraldPrimary
                                                        )
                                                        Text(
                                                            text = "${(downloadProgress * 100).toInt()}%",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = EmeraldPrimary,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    FilledTonalButton(
                                                        onClick = { hadithViewModel.downloadBook(book.slug) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                                                        shape = AlDeenTokens.ShapePill,
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = EmeraldContainer,
                                                            contentColor = EmeraldPrimary
                                                        ),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(11.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Download", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        hadithViewModel.setSelectedBookFilter(book)
                                        showBookFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Search By and Language Selectors Side-by-Side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: Search By Selector
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MintBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHadithSearchByDropdown = true }
                                    .testTag("hadith_search_by_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Search By",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                                        )
                                        Text(
                                            text = hadithMode.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Search By",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showHadithSearchByDropdown,
                                onDismissRequest = { showHadithSearchByDropdown = false }
                            ) {
                                HadithSearchMode.entries.forEach { mode ->
                                    val selected = hadithMode == mode
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = mode.label,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selected) EmeraldPrimary else TextPrimary
                                                )
                                                if (selected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            hadithViewModel.setHadithMode(mode)
                                            showHadithSearchByDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Right: Language Selector
                        val isLanguageDisabled = (hadithMode == HadithSearchMode.BY_NUMBER)
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLanguageDisabled) 0.3f else 0.6f),
                                border = BorderStroke(1.dp, if (isLanguageDisabled) MintBorder.copy(alpha = 0.3f) else MintBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLanguageDisabled) { showHadithLanguageDropdown = true }
                                    .testTag("hadith_language_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedBookFilter != null) "Book Language" else "Language",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
                                                fontSize = 10.5.sp
                                            )
                                        )
                                        AlDeenText(
                                            text = if (isLanguageDisabled) "Not Applicable" else "${hadithSearchLanguage.englishName} (${hadithSearchLanguage.nativeName})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.5f) else EmeraldPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Language",
                                        tint = if (isLanguageDisabled) TextSecondary.copy(alpha = 0.4f) else EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showHadithLanguageDropdown,
                                onDismissRequest = { showHadithLanguageDropdown = false }
                            ) {
                                 val targetBookSlug = selectedBookFilter?.slug ?: "bukhari"
                                SearchLanguage.hadithLanguages.forEach { lang ->
                                    val selected = hadithSearchLanguage == lang
                                    val normLang = lang.code.lowercase().trim()

                                    val isDownloaded = if (selectedBookFilter != null) {
                                        hadithViewModel.downloadManager.isLanguageDownloaded(selectedBookFilter!!.slug, normLang)
                                    } else {
                                        hadithViewModel.downloadManager.isLanguageDownloadedAnyBook(normLang)
                                    }

                                    val downloadKey = "${targetBookSlug}_$normLang"
                                    val langDownloadState = langStatusMap[downloadKey]
                                        ?: if (isDownloaded) HadithDownloadState.Downloaded else HadithDownloadState.NotDownloaded
                                    val isDownloading = langDownloadState is HadithDownloadState.Downloading
                                    val downloadProgress = (langDownloadState as? HadithDownloadState.Downloading)?.progress ?: 0f

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = lang.englishName,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selected) EmeraldPrimary else TextPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        AlDeenText(
                                                            text = "(${lang.nativeName})",
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = if (selected) EmeraldPrimary.copy(alpha = 0.85f) else TextSecondary,
                                                                fontSize = 12.sp
                                                            )
                                                        )
                                                    }
                                                    Text(
                                                        text = when {
                                                            isDownloaded -> if (selectedBookFilter != null) "Downloaded for ${selectedBookFilter!!.name}" else "Downloaded"
                                                            isDownloading -> "Downloading ${(downloadProgress * 100).toInt()}%..."
                                                            else -> if (selectedBookFilter != null) "Not downloaded for ${selectedBookFilter!!.name}" else "Not downloaded"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = when {
                                                                isDownloaded -> EmeraldPrimary
                                                                isDownloading -> Color(0xFFE65100)
                                                                else -> TextSecondary.copy(alpha = 0.7f)
                                                            },
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }

                                                if (isDownloaded) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Surface(
                                                            shape = AlDeenTokens.ShapePill,
                                                            color = EmeraldContainer
                                                        ) {
                                                            Text(
                                                                text = "Ready",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    color = EmeraldPrimary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.5.sp
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        if (selected) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "Active",
                                                                tint = EmeraldPrimary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                } else if (isDownloading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = EmeraldPrimary
                                                    )
                                                } else {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            hadithViewModel.downloadLanguageAndSync(targetBookSlug, lang.code)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        shape = AlDeenTokens.ShapePill,
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = EmeraldContainer,
                                                            contentColor = EmeraldPrimary
                                                        ),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = "Download Language",
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "Download",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.5.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (isDownloaded) {
                                                hadithViewModel.selectLanguageAndSync(lang)
                                                showHadithLanguageDropdown = false
                                            } else if (!isDownloading) {
                                                hadithViewModel.downloadLanguageAndSync(targetBookSlug, lang.code)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Search Bar
                    OutlinedTextField(
                        value = hadithSearchQuery,
                        onValueChange = { hadithViewModel.setSearchQuery(it) },
                        placeholder = { AlDeenText(text = hadithMode.hint) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (isHadithSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldPrimary
                                    )
                                }
                                if (hadithSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { hadithViewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = AlDeenTokens.ShapePill,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MintBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hadith_search_field")
                    )

                    // Download required banner if a specific book is selected and not downloaded
                    if (selectedBookFilter != null) {
                        val selBook = selectedBookFilter!!
                        val isSelDownloaded = hadithViewModel.isBookDownloaded(selBook.slug)
                        val selState = downloadStatusMap[selBook.slug]
                        val isSelDownloading = selState is HadithDownloadState.Downloading

                        if (!isSelDownloaded && !isSelDownloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "${selBook.name} Not Downloaded",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = "Download this book to enable complete offline searching and reading.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                    Button(
                                        onClick = { hadithViewModel.downloadBook(selBook.slug) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = AlDeenTokens.ShapePill,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. Body: Search Results or 2-column Book Collection Grid
                    if (hadithSearchQuery.isNotBlank()) {
                        if (isHadithSearching && hadithSearchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = EmeraldPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AlDeenText(
                                        text = "Searching... $hadithSearchProgress%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("hadith_search_results_list"),
                                verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                                contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
                            ) {
                                if (isHadithSearching) {
                                    item {
                                        LinearProgressIndicator(
                                            progress = { (hadithSearchProgress.coerceIn(1, 100)) / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            color = EmeraldPrimary,
                                            trackColor = MintSurface
                                        )
                                    }
                                }
                                if (hadithSearchResults.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = AlDeenTokens.SpacingLarge),
                                            shape = AlDeenTokens.ShapeCard,
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MintBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SearchOff,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "No Ahadith found matching \"$hadithSearchQuery\"",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Try searching by a different keyword or Hadith number",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = if (isHadithSearching) "Searching across books... Found ${hadithSearchResults.size} Ahadith" else "Found ${hadithSearchResults.size} Ahadith in ${hadithSearchLanguage.englishName}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }

                                    items(
                                        items = hadithSearchResults,
                                        key = { "${it.bookSlug}_${it.hadithNumber}_${it.id}" }
                                    ) { hadith ->
                                        val refKey = "hadith_${hadith.bookSlug}_${hadith.hadithNumber}"
                                        val isBookmarked = bookmarkedRefs.contains(refKey)
                                        HadithSearchResultCard(
                                            hadith = hadith,
                                            selectedLanguage = hadithSearchLanguage,
                                            isBookmarked = isBookmarked,
                                            isNumberSearch = (hadithMode == HadithSearchMode.BY_NUMBER),
                                            onReadBook = {
                                                onOpenHadith(hadith.bookSlug, hadith.chapterNumber, hadith.hadithNumber)
                                            },
                                            onToggleBookmark = { hadithViewModel.toggleHadithBookmark(hadith) },
                                            onCopy = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText(
                                                    "Hadith",
                                                    "${hadith.arabicText}\n\n${hadith.activeTranslation.ifBlank { hadith.urduTranslation }}\n[${hadith.bookName} #${hadith.hadithNumber}]"
                                                )
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Hadith copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // When query is blank: do not show books, only a helpful prompt for searching Ahadith
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (selectedBookFilter != null) "Search in ${selectedBookFilter?.englishName}" else "Search Ahadith",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (hadithMode) {
                                        HadithSearchMode.BY_NUMBER -> "Enter Hadith number (e.g. 1, 42, 100) to find matching traditions."
                                        HadithSearchMode.BY_WORD -> "Search keywords or topics in ${hadithSearchLanguage.englishName}, Arabic, or Urdu."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Download Required Alert Dialog
    bookPromptForDownload?.let { book ->
        AlertDialog(
            onDismissRequest = { bookPromptForDownload = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Download Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "To read \"${book.englishName}\", you need to download the book first. Would you like to download it now for complete offline access?\n\nContains ${book.totalHadithCount} Ahadith across ${book.totalChaptersCount} Chapters.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hadithViewModel.downloadBook(book.slug)
                        bookPromptForDownload = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    modifier = Modifier.testTag("confirm_download_dialog_btn")
                ) {
                    Text("Download Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPromptForDownload = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = AlDeenTokens.ShapeCard
        )
    }

    // Delete Warning Alert Dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Hadith Book?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${book.englishName}\"? All downloaded chapters and offline ahadith for this book will be removed from your device.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        hadithViewModel.deleteBook(book.slug)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = AlDeenTokens.ShapePill,
                    modifier = Modifier.testTag("confirm_delete_book_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = AlDeenTokens.ShapeCard
        )
    }
}

@Composable
fun QuranSearchResultCard(
    surah: Surah,
    ayah: Ayah,
    selectedLanguage: SearchLanguage,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    isNumberSearch: Boolean = false,
    onReadInSurah: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onReadInSurah() }
            .testTag("quran_result_${surah.number}_${ayah.numberInSurah}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Surah Name, Ayah Tag, Bookmark & Read button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${ayah.numberInSurah}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Surah ${surah.englishName} [${surah.number}:${ayah.numberInSurah}]",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        AlDeenText(
                            text = "${surah.urduName} • ${surah.revelationType}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) EmeraldPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text
            AlDeenText(
                text = ayah.textArabic,
                style = AlDeenTypography.QuranAyahText.copy(
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    color = TextPrimary
                ),
                targetScript = ScriptLanguage.ARABIC,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Translation text in selected language
            AlDeenText(
                text = ayah.textTranslation,
                style = if (selectedLanguage == SearchLanguage.URDU) {
                    AlDeenTypography.UrduBody.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = TextSecondary
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = TextSecondary
                    )
                },
                targetScript = if (selectedLanguage == SearchLanguage.URDU) ScriptLanguage.URDU else null,
                textAlign = if (selectedLanguage == SearchLanguage.URDU) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Audio Button
                FilledTonalButton(
                    onClick = onPlayAudio,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPlaying) EmeraldPrimary else EmeraldPrimary.copy(alpha = 0.1f),
                        contentColor = if (isPlaying) Color.White else EmeraldPrimary
                    ),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Audio",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Playing" else "Recitation",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Read in Surah Action
                Button(
                    onClick = onReadInSurah,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Read in Surah",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HadithSearchResultCard(
    hadith: Hadith,
    selectedLanguage: SearchLanguage,
    isBookmarked: Boolean,
    isNumberSearch: Boolean = false,
    onReadBook: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onReadBook() }
            .testTag("hadith_result_${hadith.bookSlug}_${hadith.hadithNumber}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Book Name, Hadith Number, Bookmark & Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${hadith.bookName} #${hadith.hadithNumber}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        AlDeenText(
                            text = hadith.chapterNameUrdu.ifBlank { "Chapter ${hadith.chapterNumber}" },
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) EmeraldPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text of Hadith
            AlDeenText(
                text = hadith.arabicText,
                style = AlDeenTypography.ArabicBody.copy(
                    fontSize = 17.sp,
                    lineHeight = 30.sp,
                    color = TextPrimary
                ),
                targetScript = ScriptLanguage.ARABIC,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation text if available
            if (hadith.activeTranslation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AlDeenText(
                    text = hadith.activeTranslation,
                    style = if (selectedLanguage == SearchLanguage.URDU) {
                        AlDeenTypography.UrduBody.copy(
                            fontSize = 14.5.sp,
                            lineHeight = 23.sp,
                            color = TextSecondary
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            color = TextSecondary
                        )
                    },
                    targetScript = if (selectedLanguage == SearchLanguage.URDU) ScriptLanguage.URDU else null,
                    textAlign = if (selectedLanguage == SearchLanguage.URDU) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Read Hadith
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onReadBook,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Open Hadith",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HadithDownloadedBooksSummary(
    allBooks: List<HadithBook>,
    downloadedBooks: List<HadithBook>,
    onDownloadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Downloaded Books: ${downloadedBooks.size} / ${allBooks.size}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = if (downloadedBooks.isEmpty()) "Built-in samples available. Download full books below."
                               else "Full offline search enabled for downloaded books.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDownloadMore,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download More", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // List of books with status
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allBooks.forEach { book ->
                    val isDownloaded = downloadedBooks.any { it.slug == book.slug }
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = if (isDownloaded) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, if (isDownloaded) EmeraldPrimary.copy(alpha = 0.5f) else MintBorder.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (isDownloaded) EmeraldPrimary else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            AlDeenText(
                                text = book.urduName.ifBlank { book.englishName },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isDownloaded) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDownloaded) EmeraldPrimary else TextSecondary
                                ),
                                targetScript = ScriptLanguage.URDU
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoSearchResultsView(
    query: String,
    sectionName: String,
    downloadedBooksCount: Int = 1,
    onGoToHadith: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No $sectionName results found for \"$query\"",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (sectionName == "Quran")
                "Try searching with different keywords, surah references (e.g. 2:255), or switch between Arabic, English, Urdu, Hindi, or Bengali."
            else
                "Try searching by Hadith number (e.g. 1) or word. Ensure the relevant Hadith books are downloaded on your device.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )

        if (sectionName == "Hadith" && downloadedBooksCount == 0 && onGoToHadith != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onGoToHadith,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = AlDeenTokens.ShapePill
            ) {
                Text("Download Hadith Books")
            }
        }
    }
}
