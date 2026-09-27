package com.example.ui.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.hadith.HadithDownloadState
import com.example.data.repository.HadithRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UrduHadithSupplement
import com.example.domain.model.Hadith
import com.example.domain.model.HadithChapter
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    viewModel: HadithViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialChapter: HadithChapter? = null,
    scrollToHadith: String? = null
) {
    val book by viewModel.selectedBook.collectAsStateWithLifecycle()
    val chapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val hadiths by viewModel.hadithsForChapter.collectAsStateWithLifecycle()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val selectedLangCode by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()
    val langStatusMap by viewModel.languageDownloadStatusMap.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val arabicFontSize by settingsRepository.hadithArabicFontSize.collectAsStateWithLifecycle()
    val transFontSize by settingsRepository.hadithTransFontSize.collectAsStateWithLifecycle()
    var showFontSizeSheet by remember { mutableStateOf(false) }

    var showTranslationSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(initialChapter) {
        if (initialChapter != null && (viewModel.selectedChapter.value?.chapterNumber != initialChapter.chapterNumber ||
                    viewModel.selectedChapter.value?.bookSlug != initialChapter.bookSlug)) {
            viewModel.selectChapter(initialChapter)
        }
    }

    LaunchedEffect(hadiths, scrollToHadith, chapter) {
        if (!scrollToHadith.isNullOrBlank() && hadiths.isNotEmpty()) {
            val normScrollTo = HadithRepository.normalizeHadithNumber(scrollToHadith)
            val index = hadiths.indexOfFirst {
                it.hadithNumber == scrollToHadith ||
                HadithRepository.normalizeHadithNumber(it.hadithNumber) == normScrollTo
            }
            if (index != -1) {
                // +1 for the chapter header item if chapter is not null
                val targetIndex = if (chapter != null) index + 1 else index
                kotlinx.coroutines.delay(120)
                listState.scrollToItem(targetIndex)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val activeLanguage = viewModel.supportedLanguages.firstOrNull { it.code == selectedLangCode }
    val activeLanguageName = activeLanguage?.nativeName ?: "ترجمہ"
    val bookmarkedRefs by viewModel.bookmarkedRefs.collectAsStateWithLifecycle()
    val downloadKey = "${book.slug}_${selectedLangCode}"
    val langState = langStatusMap[downloadKey] ?: if (viewModel.isLanguageDownloaded(book.slug, selectedLangCode)) {
        HadithDownloadState.Downloaded
    } else {
        HadithDownloadState.NotDownloaded
    }
    val isLanguageDownloaded = langState is HadithDownloadState.Downloaded
    val isLanguageDownloading = langState is HadithDownloadState.Downloading

    LaunchedEffect(selectedLangCode, book.slug, isLanguageDownloaded, isLanguageDownloading) {
        if (!isLanguageDownloaded && !isLanguageDownloading && (selectedLangCode in listOf("urd", "ur"))) {
            viewModel.downloadLanguage(book.slug, selectedLangCode)
        }
    }

    val bookDownloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()
    val isBookDownloaded = viewModel.isBookDownloaded(book.slug)
    val isBookDownloading = bookDownloadStatusMap[book.slug] is HadithDownloadState.Downloading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = chapter?.let { "${it.urduTitle} (${it.englishTitle})" } ?: "Ahadith",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = "${book.englishName} (${book.urduName}) • Chapter ${chapter?.chapterNumber ?: 1}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("hadith_reader_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                                .testTag("hadith_font_settings_btn")
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

                        // Translation Toggle in Hadith Reader Top Bar (Circle Button with Large Icon)
                        Surface(
                            shape = CircleShape,
                            color = if (isTranslationEnabled) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, if (isTranslationEnabled) EmeraldPrimary else MintBorder),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { showTranslationSheet = true }
                                .padding(end = 4.dp)
                                .testTag("hadith_reader_translation_toggle")
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("hadith_reader_screen")
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingLarge)
        ) {
            // Chapter Arabic Header Card
            if (chapter != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AlDeenTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MintBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AlDeenText(
                                text = chapter!!.arabicTitle,
                                style = AlDeenTypography.ArabicHeading.copy(
                                    color = EmeraldPrimary,
                                    fontSize = 20.sp
                                ),
                                targetScript = ScriptLanguage.ARABIC
                            )
                            val subText = if (selectedLangCode == "urd" || selectedLangCode == "ur") {
                                chapter!!.urduTitle.ifBlank { chapter!!.englishTitle }
                            } else {
                                chapter!!.englishTitle.ifBlank { chapter!!.urduTitle }
                            }
                            if (subText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                AlDeenText(
                                    text = subText,
                                    style = if (selectedLangCode == "urd" || selectedLangCode == "ur") {
                                        AlDeenTypography.UrduBody.copy(
                                            color = TextSecondary,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Language download status banner at the top if needed
            if (isTranslationEnabled && selectedLangCode !in listOf("ara", "ar")) {
                if (isLanguageDownloading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.35f)),
                            border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlDeenText(
                                        text = "ترجمہ ڈاؤن لوڈ ہو رہا ہے ($activeLanguageName)...",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(3.dp),
                                    color = EmeraldPrimary,
                                    trackColor = EmeraldContainer
                                )
                            }
                        }
                    }
                } else if (!isLanguageDownloaded && isBookDownloaded) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(0.8.dp, MintBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlDeenText(
                                    text = "منتخب زبان ($activeLanguageName) کا ترجمہ ڈاؤن لوڈ کریں",
                                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.downloadLanguage(book.slug, selectedLangCode) },
                                    shape = AlDeenTokens.ShapePill,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    AlDeenText("ڈاؤن لوڈ", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Empty & Downloading state handlers
            if (hadiths.isEmpty()) {
                item {
                    val isDownloading = isBookDownloading
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = AlDeenTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MintBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                AlDeenText(
                                    text = "احادیث مبارکہ ڈاؤنلوڈ ہو رہی ہیں...",
                                    style = AlDeenTypography.UrduBody.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    ),
                                    targetScript = ScriptLanguage.URDU
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                AlDeenText(
                                    text = "Please wait a moment while the authentic collection is loaded.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                AlDeenText(
                                    text = if (!isBookDownloaded) "اس کتاب کی تمام احادیث ڈاؤنلوڈ کریں تاکہ تمام ابواب پڑھے جا سکیں" else "اس باب میں فی الوقت احادیث کا متن دستیاب نہیں ہے",
                                    style = AlDeenTypography.UrduBody.copy(
                                        fontSize = 14.5.sp,
                                        color = TextPrimary
                                    ),
                                    targetScript = ScriptLanguage.URDU
                                )
                                if (!isBookDownloaded) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { viewModel.downloadBook(book.slug) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AlDeenText(
                                            text = "کتاب ڈاؤنلوڈ کریں (${book.urduName})",
                                            style = AlDeenTypography.UrduBody.copy(color = Color.White),
                                            targetScript = ScriptLanguage.URDU
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hadiths list: default is Arabic only; translation displayed when isTranslationEnabled == true
            items(hadiths) { hadith ->
                val ref = "hadith_${hadith.bookSlug}_${hadith.hadithNumber}"
                val isBookmarked = bookmarkedRefs.contains(ref)
                val normScrollTo = scrollToHadith?.let { HadithRepository.normalizeHadithNumber(it) }
                val isTarget = !scrollToHadith.isNullOrBlank() && (
                    hadith.hadithNumber == scrollToHadith ||
                    HadithRepository.normalizeHadithNumber(hadith.hadithNumber) == normScrollTo
                )
                HadithDetailCard(
                    hadith = hadith,
                    isBookmarked = isBookmarked,
                    isTranslationEnabled = isTranslationEnabled,
                    isLanguageDownloaded = isLanguageDownloaded,
                    isLanguageDownloading = isLanguageDownloading,
                    selectedLangCode = selectedLangCode,
                    activeLanguageName = activeLanguage?.nativeName ?: "ترجمہ",
                    arabicFontSize = arabicFontSize,
                    transFontSize = transFontSize,
                    isHighlighted = isTarget,
                    onDownloadLanguage = { viewModel.downloadLanguage(book.slug, selectedLangCode) },
                    onBookmarkClick = { viewModel.toggleHadithBookmark(hadith) }
                )
            }
        }
    }

    if (showTranslationSheet) {
        HadithTranslationBottomSheet(
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
            HadithFontSizeBottomSheet(
                arabicFontSize = arabicFontSize,
                transFontSize = transFontSize,
                onArabicFontSizeChange = { settingsRepository.setHadithArabicFontSize(it) },
                onTransFontSizeChange = { settingsRepository.setHadithTransFontSize(it) },
                onDismiss = { showFontSizeSheet = false },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun HadithDetailCard(
    hadith: Hadith,
    isBookmarked: Boolean,
    isTranslationEnabled: Boolean,
    isLanguageDownloaded: Boolean,
    isLanguageDownloading: Boolean,
    selectedLangCode: String,
    activeLanguageName: String,
    arabicFontSize: Float = 22f,
    transFontSize: Float = 14f,
    isHighlighted: Boolean = false,
    onDownloadLanguage: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hadith_card_${hadith.hadithNumber}"),
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
            // Header Row: Hadith number + Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = if (isHighlighted) EmeraldPrimary else EmeraldContainer
                    ) {
                        Text(
                            text = "Hadith #${hadith.hadithNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isHighlighted) Color.White else OnEmeraldContainer,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (isHighlighted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = GoldAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = "Searched Hadith",
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

                IconButton(onClick = onBookmarkClick, modifier = Modifier.size(AlDeenTokens.TouchTargetMin)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) GoldAccent else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main text: Display Arabic if present; otherwise fallback gracefully to Urdu or English narration
            val hasArabic = hadith.arabicText.isNotBlank()
            if (hasArabic) {
                AlDeenText(
                    text = hadith.arabicText,
                    style = AlDeenTypography.HadithArabicText.copy(fontSize = arabicFontSize.sp),
                    targetScript = ScriptLanguage.ARABIC,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (hadith.urduTranslation.isNotBlank()) {
                AlDeenText(
                    text = hadith.urduTranslation,
                    style = AlDeenTypography.UrduBody.copy(
                        fontSize = (arabicFontSize * 0.85f).sp,
                        lineHeight = (arabicFontSize * 1.5f).sp,
                        color = TextPrimary
                    ),
                    targetScript = ScriptLanguage.URDU,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (hadith.englishTranslation.isNotBlank() && selectedLangCode !in listOf("urd", "ur")) {
                AlDeenText(
                    text = hadith.englishTranslation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (arabicFontSize * 0.8f).sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Translation Section (Displayed directly below Arabic text)
            if (isTranslationEnabled) {
                val isUrduSelected = selectedLangCode in listOf("urd", "ur")
                val translationText = when {
                    isUrduSelected -> {
                        when {
                            hadith.activeTranslation.isNotBlank() && UrduHadithSupplement.hasUrduScript(hadith.activeTranslation) -> hadith.activeTranslation
                            hadith.urduTranslation.isNotBlank() && UrduHadithSupplement.hasUrduScript(hadith.urduTranslation) -> hadith.urduTranslation
                            else -> ""
                        }
                    }
                    selectedLangCode in listOf("eng", "en") && hadith.englishTranslation.isNotBlank() -> hadith.englishTranslation
                    hadith.activeTranslation.isNotBlank() -> hadith.activeTranslation
                    hadith.urduTranslation.isNotBlank() -> hadith.urduTranslation
                    hadith.englishTranslation.isNotBlank() -> hadith.englishTranslation
                    else -> ""
                }

                val isDuplicate = !hasArabic && ((selectedLangCode in listOf("urd", "ur") && hadith.urduTranslation.isNotBlank()) ||
                        (selectedLangCode in listOf("eng", "en") && hadith.englishTranslation.isNotBlank() && hadith.urduTranslation.isBlank()))

                if (translationText.isNotBlank() && !isDuplicate) {
                    val isUrduText = isUrduSelected || (translationText == hadith.urduTranslation) || UrduHadithSupplement.hasUrduScript(translationText)
                    val isRtlLanguage = isUrduText || selectedLangCode in listOf("ara", "ar")
                    val displayLangLabel = when {
                        isUrduSelected || isUrduText -> "اردو"
                        translationText == hadith.englishTranslation && selectedLangCode !in listOf("eng", "en") -> "English"
                        else -> activeLanguageName
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MintBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldContainer.copy(alpha = 0.25f), AlDeenTokens.ShapeCard)
                            .padding(12.dp)
                    ) {
                        AlDeenText(
                            text = "ترجمہ ($displayLangLabel):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AlDeenText(
                            text = translationText,
                            style = if (isRtlLanguage) {
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
                            targetScript = if (isRtlLanguage) ScriptLanguage.URDU else null,
                            textAlign = if (isRtlLanguage) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
