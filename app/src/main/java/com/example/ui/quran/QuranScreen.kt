package com.example.ui.quran

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackState
import com.example.domain.model.Ayah
import com.example.domain.model.JuzInfo
import com.example.domain.model.QuranSearchMode
import com.example.domain.model.SearchLanguage
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    viewModel: QuranViewModel,
    prayerState: com.example.domain.model.PrayerState,
    location: com.example.domain.model.UserLocation,
    formattedCurrentTime: String,
    formattedUpcomingTime: String,
    onOpenDrawer: () -> Unit,
    onLocationClick: () -> Unit,
    onSurahSelected: (Int) -> Unit,
    onAyahSelected: (Int, Int) -> Unit = { surahNum, _ -> onSurahSelected(surahNum) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val activeJuzForSheet by viewModel.activeJuzForSheet.collectAsStateWithLifecycle()
    val audioState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val downloadedSurahs by viewModel.downloadedSurahs.collectAsStateWithLifecycle()
    val ayahSearchResults by viewModel.ayahSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchProgress by viewModel.searchProgress.collectAsStateWithLifecycle()
    val isAlQuranOpened by viewModel.isAlQuranOpened.collectAsStateWithLifecycle()
    val quranSearchMode by viewModel.quranSearchMode.collectAsStateWithLifecycle()
    val quranSearchLanguage by viewModel.quranSearchLanguage.collectAsStateWithLifecycle()
    val downloadedQuranLanguages by viewModel.translationManager.downloadedLanguageCodes.collectAsStateWithLifecycle()
    var showSearchByDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Back handler: return to Quran section block when Al Quran is opened
    BackHandler(enabled = isAlQuranOpened) {
        viewModel.setAlQuranOpened(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("quran_screen_root")
    ) {
        if (!isAlQuranOpened) {
            // Main Quran Section View with consistent Header, Compact Dashboard, and "Al Quran" Block
            com.example.ui.components.AlDeenHeader(
                location = location,
                onMenuClick = onOpenDrawer,
                onLocationClick = onLocationClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AlDeenTokens.SpacingLarge)
            ) {
                // Compact Prayer Dashboard
                com.example.ui.components.CompactPrayerDashboardCard(
                    prayerState = prayerState,
                    formattedCurrentTime = formattedCurrentTime,
                    formattedUpcomingTime = formattedUpcomingTime
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Holy Quran",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "The Noble Quran",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    AlDeenText(
                        text = "الْقُرْآنُ الْكَرِيمُ",
                        style = AlDeenTypography.ArabicHeading.copy(
                            color = EmeraldPrimary,
                            fontSize = 22.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The "Al Quran" Block
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setAlQuranOpened(true) }
                        .testTag("al_quran_block"),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = "Al Quran",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = AlDeenTokens.ShapePill,
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "114 Surahs • 30 Paras",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = OnEmeraldContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AlDeenText(
                            text = "Al Quran (القرآن الكريم)",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Explore all 114 Surahs and 30 Paras (Juz) with authentic translations, word-by-word meanings, and offline audio recitations.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Highlights
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = AlDeenTokens.ShapeSmallCard,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("114", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Surahs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = AlDeenTokens.ShapeSmallCard,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("30", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Paras", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = AlDeenTokens.ShapeSmallCard,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("6,236", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Ayahs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setAlQuranOpened(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = AlDeenTokens.ShapePill,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Al Quran", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Inside "Al Quran" Directory
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AlDeenTokens.SpacingLarge, vertical = AlDeenTokens.SpacingMedium)
            ) {
                // Top Bar with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setAlQuranOpened(false) },
                        modifier = Modifier.testTag("al_quran_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Quran Section",
                            tint = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        AlDeenText(
                            text = "Al Quran (الْقُرْآنُ الْكَرِيمُ)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = "Browse by Surah or Para (Juz)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        AlDeenText(
                            text = quranSearchMode.hint,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.testTag("quran_search_clear_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
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
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_search_field")
                )

                // Search By and Language Selectors Side-by-Side for Quran
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                                .clickable { showSearchByDropdown = true }
                                .testTag("quran_search_by_dropdown_trigger")
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
                                        text = quranSearchMode.label,
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
                            expanded = showSearchByDropdown,
                            onDismissRequest = { showSearchByDropdown = false }
                        ) {
                            QuranSearchMode.entries.forEach { mode ->
                                val selected = quranSearchMode == mode
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
                                        viewModel.setQuranSearchMode(mode)
                                        showSearchByDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Right: Language Selector
                    val isLanguageDisabled = (quranSearchMode == QuranSearchMode.BY_AYAH)

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLanguageDisabled) 0.3f else 0.6f),
                            border = BorderStroke(1.dp, if (isLanguageDisabled) MintBorder.copy(alpha = 0.3f) else MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLanguageDisabled) { showLanguageDropdown = true }
                                .testTag("quran_search_language_dropdown_trigger")
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
                                        text = if (isLanguageDisabled) "Not Applicable" else "${quranSearchLanguage.englishName} (${quranSearchLanguage.nativeName})",
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
                            expanded = showLanguageDropdown,
                            onDismissRequest = { showLanguageDropdown = false }
                        ) {
                            SearchLanguage.quranLanguages.forEach { lang ->
                                val selected = quranSearchLanguage == lang
                                val isDownloaded = downloadedQuranLanguages.contains(lang.code) || viewModel.translationManager.isLanguageCodeDownloaded(lang.code)

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
                                            viewModel.setQuranSearchLanguage(lang)
                                            showLanguageDropdown = false
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
                                                showLanguageDropdown = false
                                                coroutineScope.launch {
                                                    val trans = viewModel.translationManager.supportedLanguages.firstOrNull { it.languageCode == lang.code }
                                                    if (trans != null) {
                                                        viewModel.translationManager.downloadTranslation(trans.id)
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

                if (searchQuery.isBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Tabs: Surah, Juz
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = EmeraldPrimary,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTab),
                                color = EmeraldPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            text = {
                                Text(
                                    "Surah (${surahs.size})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            modifier = Modifier.testTag("quran_tab_surah")
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            text = {
                                Text(
                                    "Juz / Para (30)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            modifier = Modifier.testTag("quran_tab_juz")
                        )
                    }
                }
            }

            // Content: Live Search Results if searching, else Tab Content
            if (searchQuery.isNotBlank()) {
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            AlDeenText(
                                text = "Searching... $searchProgress%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AlDeenTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingSmall),
                    contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
                ) {
                    if (!isSearching && ayahSearchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Quran verses found for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (ayahSearchResults.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(0.8.dp, MintBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ayahSearchResults.size} verses found",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = "Tap verse to read in Surah",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        items(
                            items = ayahSearchResults,
                            key = { "${it.first.number}_${it.second.numberInSurah}" }
                        ) { (surah, ayah) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.openSurah(surah.number, fromPara = false)
                                        onAyahSelected(surah.number, ayah.numberInSurah)
                                    }
                                    .testTag("search_ayah_card_${surah.number}_${ayah.numberInSurah}"),
                                shape = AlDeenTokens.ShapeCard,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldContainer.copy(alpha = 0.7f),
                                            border = BorderStroke(0.6.dp, EmeraldPrimary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Surah ${surah.englishName} [${surah.number}:${ayah.numberInSurah}]",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary,
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AlDeenText(
                                                text = surah.arabicName,
                                                style = AlDeenTypography.QuranAyahText.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            )
                                            IconButton(
                                                onClick = {
                                                    val copyText = "${ayah.textArabic}\n\n${ayah.textTranslation}\n[Surah ${surah.englishName} ${surah.number}:${ayah.numberInSurah}]"
                                                    clipboardManager.setText(AnnotatedString(copyText))
                                                    Toast.makeText(context, "Ayah copied", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Ayah",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    AlDeenText(
                                        text = ayah.textArabic,
                                        style = AlDeenTypography.QuranAyahText.copy(
                                            fontSize = 18.sp,
                                            lineHeight = 28.sp,
                                            color = TextPrimary
                                        ),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (ayah.textTranslation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(
                                            color = MintBorder.copy(alpha = 0.6f),
                                            thickness = 0.6.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AlDeenText(
                                            text = ayah.textTranslation,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextSecondary,
                                                fontSize = 13.5.sp,
                                                lineHeight = 20.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 0) {
                // Surahs List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AlDeenTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingSmall),
                    contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
                ) {
                    items(surahs, key = { it.number }) { surah ->
                        val isPlaying = audioState is AudioPlaybackState.Playing &&
                                (audioState as AudioPlaybackState.Playing).trackId == "surah_${surah.number}"
                        val isDownloaded = downloadedSurahs.contains(surah.number)

                        SurahListItem(
                            surah = surah,
                            isPlaying = isPlaying,
                            isDownloaded = isDownloaded,
                            onPlayClick = { viewModel.playSurahAudio(surah) },
                            onSurahClick = {
                                viewModel.openSurah(surah.number, fromPara = false)
                                onSurahSelected(surah.number)
                            }
                        )
                    }
                }
            } else {
                // Juz List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AlDeenTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingSmall),
                    contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
                ) {
                    items(viewModel.juzList, key = { it.juzNumber }) { juz ->
                        JuzListItem(
                            juz = juz,
                            onJuzClick = { viewModel.openJuzSheet(juz) }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Juz
    if (activeJuzForSheet != null) {
        val juz = activeJuzForSheet!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeJuzSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = AlDeenTokens.ShapeSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AlDeenTokens.SpacingLarge)
                    .padding(bottom = AlDeenTokens.SpacingXXLarge)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Juz ${juz.juzNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = "Surahs in this Para",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Text(
                        text = "Part ${juz.juzNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MintBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(juz.surahs) { surahInJuz ->
                        val isJuzSurahDownloaded = downloadedSurahs.contains(surahInJuz.surahNumber)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AlDeenTokens.ShapeSmallCard)
                                .clickable {
                                    viewModel.closeJuzSheet()
                                    viewModel.openSurah(surahInJuz.surahNumber, fromPara = true)
                                    onSurahSelected(surahInJuz.surahNumber)
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${surahInJuz.surahNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = OnEmeraldContainer
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = surahInJuz.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        )
                                        if (isJuzSurahDownloaded) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Downloaded Offline",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Ayahs ${surahInJuz.startAyah} - ${surahInJuz.endAyah}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
                        }
                        HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.7.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahListItem(
    surah: Surah,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    onPlayClick: () -> Unit,
    onSurahClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSurahClick() }
            .testTag("surah_item_${surah.number}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Number Badge + Names and Verses info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Surah Number Badge
                Surface(
                    shape = AlDeenTokens.ShapeSmallCard,
                    color = EmeraldContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${surah.number}",
                            style = AlDeenTypography.SurahGlyphText.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = EmeraldPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = surah.englishName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.5.sp
                            )
                        )
                        if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Audio Downloaded Offline",
                                tint = EmeraldPrimary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .testTag("surah_downloaded_tick_${surah.number}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${surah.revelationType} • ${surah.numberOfAyahs} Verses",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Right: Arabic & Urdu Name + Play button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    AlDeenText(
                        text = surah.arabicName,
                        style = AlDeenTypography.QuranAyahText.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                    AlDeenText(
                        text = surah.urduName,
                        style = AlDeenTypography.UrduHeading.copy(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                }

                // Circular icon-only Play button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(AlDeenTokens.TouchTargetMin)
                        .testTag("surah_audio_btn_${surah.number}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = if (isPlaying) "Pause Surah Audio" else "Play Surah Audio",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JuzListItem(
    juz: JuzInfo,
    onJuzClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onJuzClick() }
            .testTag("juz_item_${juz.juzNumber}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Juz Number Badge + Contains count (No Arabic on left)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${juz.juzNumber}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Para ${juz.juzNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Contains ${juz.surahs.size} Surahs • Tap to view",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            // Right: English Name written
            Text(
                text = "Juz ${juz.juzNumber}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 16.sp
                )
            )
        }
    }
}
