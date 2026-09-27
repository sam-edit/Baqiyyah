package com.example.ui.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.hadith.HadithDownloadState
import com.example.domain.model.HadithChapter
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaabListScreen(
    viewModel: HadithViewModel,
    onChapterSelected: (HadithChapter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.selectedBook.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val downloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()

    var showTranslationSheet by remember { mutableStateOf(false) }

    val isBookDownloaded = viewModel.isBookDownloaded(book.slug)
    val bookDownloadState = downloadStatusMap[book.slug] ?: if (isBookDownloaded) {
        HadithDownloadState.Downloaded
    } else {
        HadithDownloadState.NotDownloaded
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "${book.englishName} (${book.urduName})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = "Chapters (ابواب)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("baab_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    // Translation Toggle & Language Selector Button (Circle Button with Large Icon)
                    Surface(
                        shape = CircleShape,
                        color = if (isTranslationEnabled) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, if (isTranslationEnabled) EmeraldPrimary else MintBorder),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { showTranslationSheet = true }
                            .padding(end = 4.dp)
                            .testTag("baab_translation_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translations",
                                tint = if (isTranslationEnabled) EmeraldPrimary else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("baab_list_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingSmall)
        ) {
            // If book is not downloaded, display prominent download prompt at the top
            if (!isBookDownloaded) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = AlDeenTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Download Required",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "To read all chapters and ahadith, please download this book first.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (bookDownloadState is HadithDownloadState.Downloading) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { bookDownloadState.progress },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(AlDeenTokens.ShapePill),
                                        color = EmeraldPrimary,
                                        trackColor = EmeraldContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Downloading ${(bookDownloadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.downloadBook(book.slug) },
                                    shape = AlDeenTokens.ShapePill,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Now")
                                }
                            }
                        }
                    }
                }
            }

            items(chapters) { chapter ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AlDeenTokens.ShapeCard)
                        .clickable {
                            viewModel.selectChapter(chapter)
                            onChapterSelected(chapter)
                        }
                        .testTag("chapter_item_${chapter.chapterNumber}"),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Chapter Number
                            Surface(
                                shape = CircleShape,
                                color = EmeraldContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${chapter.chapterNumber}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = OnEmeraldContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                AlDeenText(
                                    text = chapter.arabicTitle.ifBlank { "كتاب ${chapter.chapterNumber}" },
                                    style = AlDeenTypography.ArabicHadithSmall.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        lineHeight = 24.sp
                                    ),
                                    targetScript = ScriptLanguage.ARABIC,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chapter.englishTitle.ifBlank { chapter.urduTitle.ifBlank { "Chapter ${chapter.chapterNumber}" } },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = TextSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTranslationSheet) {
        HadithTranslationBottomSheet(
            viewModel = viewModel,
            onDismiss = { showTranslationSheet = false }
        )
    }
}
