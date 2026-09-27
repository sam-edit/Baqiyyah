package com.example.ui.hadith

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.hadith.HadithDownloadState
import com.example.domain.model.Hadith
import com.example.domain.model.HadithBook
import com.example.domain.model.HadithSearchMode
import com.example.domain.model.SearchLanguage
import com.example.ui.search.HadithSearchResultCard
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithScreen(
    viewModel: HadithViewModel,
    prayerState: com.example.domain.model.PrayerState,
    location: com.example.domain.model.UserLocation,
    formattedCurrentTime: String,
    formattedUpcomingTime: String,
    onOpenDrawer: () -> Unit,
    onLocationClick: () -> Unit,
    onBookSelected: (HadithBook) -> Unit,
    onOpenHadith: (String, Int, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchProgress by viewModel.searchProgress.collectAsStateWithLifecycle()
    val hadithMode by viewModel.hadithMode.collectAsStateWithLifecycle()
    val searchLanguage by viewModel.searchLanguage.collectAsStateWithLifecycle()
    val downloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()
    val langStatusMap by viewModel.languageDownloadStatusMap.collectAsStateWithLifecycle()
    val selectedBookFilter by viewModel.selectedBookFilter.collectAsStateWithLifecycle()
    val bookmarkedRefs by viewModel.bookmarkedRefs.collectAsStateWithLifecycle()

    var showBookFilterDropdown by remember { mutableStateOf(false) }
    var showSearchByDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }

    var bookPromptForDownload by remember { mutableStateOf<HadithBook?>(null) }
    var bookToDelete by remember { mutableStateOf<HadithBook?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("hadith_screen_root")
    ) {
        // Consistent Header
        com.example.ui.components.AlDeenHeader(
            location = location,
            onMenuClick = onOpenDrawer,
            onLocationClick = onLocationClick
        )

        // Compact Prayer Dashboard & Search
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlDeenTokens.SpacingLarge)
        ) {
            com.example.ui.components.CompactPrayerDashboardCard(
                prayerState = prayerState,
                formattedCurrentTime = formattedCurrentTime,
                formattedUpcomingTime = formattedUpcomingTime
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hadeeth-e-Nabvi",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "Prophetic Traditions & Kutub al-Sittah",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                AlDeenText(
                    text = "الْحَدِيثُ النَّبَوِيّ",
                    style = AlDeenTypography.ArabicHeading.copy(
                        color = EmeraldPrimary,
                        fontSize = 22.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Book Selection Option (All Books + Individual Books) Above the Two Filters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
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

                        // When a book is selected, show download button / status right in front of it
                        if (selectedBookFilter != null) {
                            val selBook = selectedBookFilter!!
                            val bookState = downloadStatusMap[selBook.slug]
                                ?: if (viewModel.isBookDownloaded(selBook.slug)) HadithDownloadState.Downloaded else HadithDownloadState.NotDownloaded
                            val isBookDownloaded = bookState is HadithDownloadState.Downloaded || viewModel.isBookDownloaded(selBook.slug)
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
                                        onClick = {
                                            viewModel.downloadBook(selBook.slug)
                                        },
                                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                                        shape = AlDeenTokens.ShapePill,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("book_filter_download_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download ${selBook.name}",
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
                    onDismissRequest = { showBookFilterDropdown = false },
                    modifier = Modifier
                        .widthIn(min = 280.dp)
                        .heightIn(max = 380.dp)
                ) {
                    val isAllSelected = selectedBookFilter == null
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "All Books",
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) EmeraldPrimary else TextPrimary
                                    )
                                    AlDeenText(
                                        text = "تمام کتبِ احادیث",
                                        style = AlDeenTypography.UrduCaption.copy(color = TextSecondary, fontSize = 10.sp)
                                    )
                                }
                                if (isAllSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        onClick = {
                            viewModel.setSelectedBookFilter(null)
                            showBookFilterDropdown = false
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MintBorder.copy(alpha = 0.5f))

                    viewModel.books.forEach { book ->
                        val isBookSelected = selectedBookFilter?.slug == book.slug
                        val isDownloaded = viewModel.isBookDownloaded(book.slug)
                        val itemState = downloadStatusMap[book.slug]
                            ?: if (isDownloaded) HadithDownloadState.Downloaded else HadithDownloadState.NotDownloaded
                        val isItemDownloading = itemState is HadithDownloadState.Downloading
                        val itemProgress = (itemState as? HadithDownloadState.Downloading)?.progress ?: 0f

                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = book.name,
                                            fontWeight = if (isBookSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isBookSelected) EmeraldPrimary else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        AlDeenText(
                                            text = book.arabicName,
                                            style = AlDeenTypography.ArabicHadithSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            ),
                                            targetScript = ScriptLanguage.ARABIC,
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isDownloaded) {
                                            Surface(
                                                shape = AlDeenTokens.ShapePill,
                                                color = EmeraldContainer
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Downloaded",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Downloaded",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = EmeraldPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        )
                                                    )
                                                }
                                            }
                                        } else if (isItemDownloading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier
                                                    .clip(AlDeenTokens.ShapePill)
                                                    .background(EmeraldContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(11.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = EmeraldPrimary
                                                )
                                                Text(
                                                    text = "${(itemProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = EmeraldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                )
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.downloadBook(book.slug)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = AlDeenTokens.ShapePill,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = EmeraldContainer,
                                                    contentColor = EmeraldPrimary
                                                ),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download",
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Download",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        if (isBookSelected) {
                                            Icon(
                                                Icons.Default.RadioButtonChecked,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setSelectedBookFilter(book)
                                showBookFilterDropdown = false
                            }
                        )
                    }
                }
            }

            // Search By & Language Selectors Side-by-Side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                        expanded = showSearchByDropdown,
                        onDismissRequest = { showSearchByDropdown = false }
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
                                    viewModel.setHadithMode(mode)
                                    showSearchByDropdown = false
                                }
                            )
                        }
                    }
                }

                // Right: Language Selector
                val isLanguageDisabled = (hadithMode == HadithSearchMode.BY_NUMBER)
                val targetBookSlug = selectedBookFilter?.slug ?: viewModel.books.first().slug

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLanguageDisabled) 0.3f else 0.6f),
                        border = BorderStroke(1.dp, if (isLanguageDisabled) MintBorder.copy(alpha = 0.3f) else MintBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLanguageDisabled) { showLanguageDropdown = true }
                            .testTag("hadith_search_language_dropdown_trigger")
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
                                Text(
                                    text = if (isLanguageDisabled) "Not Applicable" else "${searchLanguage.englishName} (${searchLanguage.nativeName})",
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
                        onDismissRequest = { showLanguageDropdown = false },
                        modifier = Modifier
                            .widthIn(min = 290.dp)
                            .heightIn(max = 420.dp)
                    ) {
                        SearchLanguage.hadithLanguages.forEach { lang ->
                            val selected = searchLanguage == lang
                            val normLang = viewModel.downloadManager.normalizeLang(lang.code)

                            // When a specific book is selected, ONLY show downloaded if it is downloaded for that book!
                            val isDownloaded = if (selectedBookFilter != null) {
                                viewModel.downloadManager.isLanguageDownloaded(selectedBookFilter!!.slug, normLang)
                            } else {
                                viewModel.downloadManager.isLanguageDownloadedAnyBook(normLang)
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
                                                Text(
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
                                            // Download button that immediately sinks/syncs into the book!
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.downloadLanguageAndSync(targetBookSlug, lang.code)
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
                                        // Immediately sinks & syncs with the book reader
                                        viewModel.selectLanguageAndSync(lang)
                                        showLanguageDropdown = false
                                    } else if (!isDownloading) {
                                        viewModel.downloadLanguageAndSync(targetBookSlug, lang.code)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { AlDeenText(text = hadithMode.hint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        }
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
        }

        // Body: Search Results or 2-column Book Collection Grid
        if (searchQuery.isNotBlank()) {
            if (isSearching && searchResults.isEmpty()) {
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
                        Spacer(modifier = Modifier.height(12.dp))
                        AlDeenText(
                            text = "Searching across books... $searchProgress%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else {
                // Search Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AlDeenTokens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                    contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
                ) {
                    if (isSearching) {
                        item {
                            LinearProgressIndicator(
                                progress = { (searchProgress.coerceIn(1, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = EmeraldPrimary,
                                trackColor = MintSurface
                            )
                        }
                    }
                    if (searchResults.isEmpty()) {
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
                                    text = "No Ahadith found matching \"$searchQuery\"",
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
                            text = "Found ${searchResults.size} Ahadith in ${searchLanguage.englishName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    items(searchResults) { hadith ->
                        val ref = "hadith_${hadith.bookSlug}_${hadith.hadithNumber}"
                        val isBookmarked = bookmarkedRefs.contains(ref)
                        HadithSearchResultCard(
                            hadith = hadith,
                            selectedLanguage = searchLanguage,
                            isBookmarked = isBookmarked,
                            onReadBook = {
                                onOpenHadith(hadith.bookSlug, hadith.chapterNumber, hadith.hadithNumber)
                            },
                            onToggleBookmark = { viewModel.toggleHadithBookmark(hadith) },
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
            val displayedBooks = if (selectedBookFilter == null) {
                viewModel.books
            } else {
                viewModel.books.filter { it.slug == selectedBookFilter?.slug }
            }

            if (selectedBookFilter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AlDeenTokens.SpacingLarge, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered Book: ${selectedBookFilter?.name}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextButton(
                        onClick = { viewModel.setSelectedBookFilter(null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        AlDeenText(
                            text = "Show All (تمام)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Primary Books Collection in clean 2-column layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AlDeenTokens.SpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium)
            ) {
                items(displayedBooks) { book ->
                    val downloadState = downloadStatusMap[book.slug] ?: if (viewModel.isBookDownloaded(book.slug)) {
                        HadithDownloadState.Downloaded
                    } else {
                        HadithDownloadState.NotDownloaded
                    }
                    val isDownloaded = viewModel.isBookDownloaded(book.slug)

                    HadithBookCard(
                        book = book,
                        downloadState = downloadState,
                        isDownloaded = isDownloaded,
                        onDownload = { viewModel.downloadBook(book.slug) },
                        onDeleteRequest = { bookToDelete = book },
                        onClick = {
                            if (isDownloaded) {
                                viewModel.selectBook(book)
                                onBookSelected(book)
                            } else {
                                bookPromptForDownload = book
                            }
                        }
                    )
                }
            }
        }
    }

    // Download Required Alert Dialog before opening un-downloaded Hadith book (All in English)
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
                        viewModel.downloadBook(book.slug)
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

    // Delete Warning Alert Dialog (All in English)
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
                        viewModel.deleteBook(book.slug)
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
fun HadithBookCard(
    book: HadithBook,
    downloadState: HadithDownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onDeleteRequest: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("hadith_book_${book.slug}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            AlDeenTokens.CardBorderWidth,
            if (isDownloaded) EmeraldPrimary.copy(alpha = 0.5f) else MintBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emblem / Logo Placeholder
            Surface(
                shape = CircleShape,
                color = if (isDownloaded) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.AutoStories else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isDownloaded) EmeraldPrimary else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Title: English Name (Arabic name removed from top/left)
            Text(
                text = book.englishName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 14.5.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle: Urdu Name
            AlDeenText(
                text = book.urduName,
                style = AlDeenTypography.UrduBody.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 12.5.sp
                ),
                targetScript = ScriptLanguage.URDU,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Hadith Count Badge
            Surface(
                shape = AlDeenTokens.ShapePill,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${book.totalHadithCount} Ahadith",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Download Action / Progress Area with visible line and percentage
            when (downloadState) {
                is HadithDownloadState.Downloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(AlDeenTokens.ShapePill),
                            color = EmeraldPrimary,
                            trackColor = EmeraldContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloading ${(downloadState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                is HadithDownloadState.Downloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = EmeraldContainer,
                            border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDeleteRequest,
                            modifier = Modifier.size(28.dp).testTag("delete_book_${book.slug}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete book",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = onDownload,
                        shape = AlDeenTokens.ShapePill,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("download_book_${book.slug}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
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
