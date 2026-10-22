package com.example.ui.happiness

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.hadith.HadithDownloadManager
import com.example.data.hadith.HadithDownloadState
import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.BuiltInHadithsProvider
import com.example.data.repository.HadithRepository
import com.example.data.repository.OfflineQuranProvider
import com.example.domain.model.Ayah
import com.example.domain.model.Surah
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class InsertContentType(val title: String, val urduTitle: String = "") {
    MANUAL("Custom Text"),
    QURAN("Quran Ayah"),
    HADITH("Hadith")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertContentDialog(
    initialText: String = "",
    initialFont: HappinessFontFamily = HappinessFontFamily.URDU,
    initialTab: InsertContentType = InsertContentType.MANUAL,
    onConfirm: (text: String, font: HappinessFontFamily, sourceType: HappinessSourceType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    var selectedFont by remember { mutableStateOf(initialFont) }

    // Manual Tab State
    var manualText by remember { mutableStateOf(initialText) }

    // Quran Tab State
    val surahs = remember { AuthoritativeContentProvider.allSurahs }
    var selectedSurah by remember { mutableStateOf(surahs.firstOrNull { it.number == 1 } ?: surahs.first()) }
    var surahDropdownExpanded by remember { mutableStateOf(false) }
    var ayahNumberInput by remember { mutableStateOf("1") }
    var includeQuranTranslation by remember { mutableStateOf(true) }
    var loadedAyah by remember { mutableStateOf<Ayah?>(null) }

    // Hadith Tab State & Download Integration
    val hadithManager = remember { HadithDownloadManager.getInstance(context) }
    val hadithRepository = remember { HadithRepository(context, hadithManager) }
    val downloadStatusMap by hadithManager.downloadStatusMap.collectAsStateWithLifecycle()
    val hadithBooks = remember { AuthoritativeContentProvider.hadithBooks }
    var selectedBook by remember { mutableStateOf(hadithBooks.first()) }
    var hadithNumberInput by remember { mutableStateOf("1") }
    var includeHadithTranslation by remember { mutableStateOf(true) }
    var loadedHadithText by remember { mutableStateOf("") }
    var loadedHadithTrans by remember { mutableStateOf("") }
    var isLoadingHadith by remember { mutableStateOf(false) }

    val bookDownloadState = downloadStatusMap[selectedBook.slug]
    val isBookDownloaded = remember(selectedBook, bookDownloadState, downloadStatusMap) {
        hadithManager.isBookDownloaded(selectedBook.slug) || bookDownloadState is HadithDownloadState.Downloaded
    }

    // Quick phrases for greeting / wishes
    val quickPhrases = remember {
        listOf(
            "جمعہ مبارک",
            "رمضان مبارک",
            "عید مبارک",
            "جزاك الله خيرا",
            "سبحان الله وبحمده",
            "الحمد لله رب العالمين",
            "ما شاء الله لا قوة إلا بالله",
            "تقبل الله منا ومنکم",
            "اللهم صل وسلم على نبينا محمد",
            "دعاؤں میں یاد رکھیئے گا"
        )
    }

    // Popular Surahs quick access
    val popularSurahs = remember {
        listOf(
            1 to "الفاتحة",
            2 to "البقرة",
            36 to "يس",
            55 to "الرحمن",
            67 to "الملك",
            112 to "الإخلاص",
            113 to "الفلق",
            114 to "الناس"
        )
    }

    // Effect to load Ayah when Surah or Ayah Number changes
    LaunchedEffect(selectedSurah, ayahNumberInput) {
        val ayahNum = ayahNumberInput.toIntOrNull()?.coerceIn(1, selectedSurah.numberOfAyahs) ?: 1
        val ayahs = OfflineQuranProvider.getAyahsForSurah(context, selectedSurah.number)
        loadedAyah = ayahs.firstOrNull { it.numberInSurah == ayahNum }
            ?: ayahs.firstOrNull()
    }

    // Effect to load Hadith accurately when Book or Hadith Number changes, or when Book is downloaded
    LaunchedEffect(selectedBook, hadithNumberInput, isBookDownloaded, bookDownloadState) {
        val queryNum = hadithNumberInput.trim().ifBlank { "1" }
        isLoadingHadith = true

        val hadith = withContext(Dispatchers.IO) {
            hadithRepository.getSingleHadith(selectedBook.slug, queryNum)
        }

        if (hadith != null && (hadith.arabicText.isNotBlank() || hadith.urduTranslation.isNotBlank())) {
            loadedHadithText = hadith.arabicText
            loadedHadithTrans = hadith.urduTranslation.ifBlank { hadith.activeTranslation }
        } else {
            val builtInList = BuiltInHadithsProvider.getHadithsForBook(selectedBook.slug)
            val found = builtInList.firstOrNull { it.hadithNumber == queryNum || it.hadithNumber == queryNum.toIntOrNull()?.toString() }
                ?: builtInList.firstOrNull()
            if (found != null) {
                loadedHadithText = found.arabicText
                loadedHadithTrans = found.urduTranslation.ifBlank { found.activeTranslation }
            } else {
                val fallbackList = AuthoritativeContentProvider.getHadithsForChapter(selectedBook.slug, 1)
                val fb = fallbackList.firstOrNull()
                if (fb != null) {
                    loadedHadithText = fb.arabicText
                    loadedHadithTrans = fb.urduTranslation.ifBlank { fb.activeTranslation }
                } else {
                    loadedHadithText = "قال رسول الله ﷺ: «إنما الأعمال بالنيات»"
                    loadedHadithTrans = "رسول اللہ صلی اللہ علیہ وسلم نے فرمایا: تمام اعمال کا دارومدار نیتوں پر ہے۔"
                }
            }
        }
        isLoadingHadith = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialText.isNotBlank() && selectedTab == InsertContentType.MANUAL) {
                        "Edit Text"
                    } else {
                        when (selectedTab) {
                            InsertContentType.MANUAL -> "Custom Text"
                            InsertContentType.QURAN -> "Quran Ayah"
                            InsertContentType.HADITH -> "Hadith"
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 17.sp
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    InsertContentType.values().forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedTab == type,
                            onClick = { selectedTab = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = InsertContentType.values().size)
                        ) {
                            Text(text = type.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                when (selectedTab) {
                    InsertContentType.MANUAL -> {
                        // Freeform Manual Text Input
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            label = { Text("Enter your text") },
                            placeholder = { Text("Type any Dua, greeting, or message...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 5
                        )

                        Text(
                            text = "Quick Greetings:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickPhrases) { phrase ->
                                SuggestionChip(
                                    onClick = {
                                        manualText = if (manualText.isBlank()) phrase else "$manualText\n$phrase"
                                    },
                                    label = { Text(phrase, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Font Selection: Urdu or Arabic built-in font only (Requirement)
                        Text(
                            text = "Select Font:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HappinessFontFamily.values().forEach { font ->
                                val isSelected = (selectedFont == font)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFont = font },
                                    label = {
                                        Text(
                                            text = when (font) {
                                                HappinessFontFamily.URDU -> "Urdu"
                                                HappinessFontFamily.ARABIC -> "Arabic"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }

                    InsertContentType.QURAN -> {
                        // Quick Popular Surah Chips
                        Text(
                            text = "Popular Surahs:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularSurahs) { (num, name) ->
                                val isSelected = (selectedSurah.number == num)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        surahs.firstOrNull { it.number == num }?.let {
                                            selectedSurah = it
                                            ayahNumberInput = "1"
                                        }
                                    },
                                    label = { Text("$num. $name", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Surah Picker Dropdown
                        ExposedDropdownMenuBox(
                            expanded = surahDropdownExpanded,
                            onExpandedChange = { surahDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${selectedSurah.number}. ${selectedSurah.englishName} (${selectedSurah.arabicName})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Surah") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surahDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = surahDropdownExpanded,
                                onDismissRequest = { surahDropdownExpanded = false }
                            ) {
                                surahs.take(40).forEach { surah ->
                                    DropdownMenuItem(
                                        text = { Text("${surah.number}. ${surah.englishName} - ${surah.arabicName}") },
                                        onClick = {
                                            selectedSurah = surah
                                            ayahNumberInput = "1"
                                            surahDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Ayah Number Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ayahNumberInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    val num = filtered.toIntOrNull()
                                    if (num == null || num <= selectedSurah.numberOfAyahs) {
                                        ayahNumberInput = filtered
                                    }
                                },
                                label = { Text("Ayah # (1-${selectedSurah.numberOfAyahs})") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    val current = ayahNumberInput.toIntOrNull() ?: 1
                                    if (current > 1) ayahNumberInput = (current - 1).toString()
                                },
                                enabled = (ayahNumberInput.toIntOrNull() ?: 1) > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Previous Ayah")
                            }

                            IconButton(
                                onClick = {
                                    val current = ayahNumberInput.toIntOrNull() ?: 1
                                    if (current < selectedSurah.numberOfAyahs) ayahNumberInput = (current + 1).toString()
                                },
                                enabled = (ayahNumberInput.toIntOrNull() ?: 1) < selectedSurah.numberOfAyahs
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Next Ayah")
                            }
                        }

                        // Include Translation Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldContainer.copy(alpha = 0.4f))
                                .clickable { includeQuranTranslation = !includeQuranTranslation }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Include Urdu Translation",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Switch(
                                checked = includeQuranTranslation,
                                onCheckedChange = { includeQuranTranslation = it }
                            )
                        }

                        // Ayah Preview Card
                        loadedAyah?.let { ayah ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MintBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = ayah.textArabic,
                                        style = AlDeenTypography.ArabicBody.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            textAlign = TextAlign.Center,
                                            color = EmeraldPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (includeQuranTranslation && ayah.urduTranslation.isNotBlank()) {
                                        HorizontalDivider(color = MintBorder.copy(alpha = 0.5f))
                                        Text(
                                            text = ayah.urduTranslation,
                                            style = AlDeenTypography.UrduBody.copy(
                                                fontSize = 12.5.sp,
                                                lineHeight = 19.sp,
                                                textAlign = TextAlign.Center,
                                                color = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Auto-font note (Arabic built-in + Urdu built-in)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "The Ayah will be in Arabic font and Trasnaltion will be in Urdu font",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    InsertContentType.HADITH -> {
                        // Hadith Book Selector Row
                        Text(
                            text = "Select Hadith Book:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(hadithBooks) { book ->
                                val isSelected = (selectedBook.slug == book.slug)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedBook = book
                                        hadithNumberInput = when (book.slug) {
                                            "sahih-muslim" -> "93"
                                            else -> "1"
                                        }
                                    },
                                    label = { Text(book.urduName.ifBlank { book.englishName }, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Book Download Status & Download Button (Requirement)
                        if (isBookDownloaded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(EmeraldContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Book Downloaded",
                                    fontSize = 11.5.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "This book is not downloaded yet",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (bookDownloadState is HadithDownloadState.Downloading) {
                                        LinearProgressIndicator(
                                            progress = { bookDownloadState.progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Downloading: ${(bookDownloadState.progress * 100).toInt()}%",
                                            fontSize = 10.5.sp,
                                            color = EmeraldPrimary
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                hadithManager.downloadBook(selectedBook.slug)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Download Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Popular / Recommended Hadiths quick access chips for selected book
                        val popularHadithsForBook = remember(selectedBook) {
                            when (selectedBook.slug) {
                                "sahih-bukhari" -> listOf(
                                    "1" to "نیتیں (Intentions)",
                                    "2" to "وحی (Revelation)",
                                    "8" to "ارکان اسلام (Pillars)",
                                    "9" to "حیا (Modesty)",
                                    "13" to "اخوت (Brotherhood)"
                                )
                                "sahih-muslim" -> listOf(
                                    "93" to "حدیث جبریل (Iman)",
                                    "177" to "برائی روکنا (Evil)",
                                    "196" to "نصیحت (Sincerity)",
                                    "1" to "حدیث 1"
                                )
                                "arbaeen-nawawi" -> listOf(
                                    "1" to "نیتیں",
                                    "2" to "اسلام و ایمان",
                                    "3" to "ارکان",
                                    "7" to "نصیحت",
                                    "16" to "غصہ نہ کرو"
                                )
                                "hadith-qudsi" -> listOf(
                                    "1" to "حدیث 1",
                                    "2" to "حدیث 2",
                                    "5" to "حدیث 5",
                                    "10" to "حدیث 10"
                                )
                                "sunan-an-nasai" -> listOf(
                                    "1" to "طہارت (Purity)",
                                    "5" to "مسواک (Siwak)"
                                )
                                "sunan-abi-dawud" -> listOf(
                                    "1" to "قضائے حاجت",
                                    "4" to "دعا بیت الخلاء"
                                )
                                "jami-at-tirmidhi" -> listOf(
                                    "1" to "طہارت",
                                    "3" to "مفتاح الصلاة"
                                )
                                "sunan-ibn-majah" -> listOf(
                                    "1" to "اتباع سنت",
                                    "224" to "طلب علم"
                                )
                                "muwatta-malik" -> listOf(
                                    "1" to "اوقات نماز",
                                    "2" to "نماز عصر"
                                )
                                else -> listOf("1" to "حدیث 1")
                            }
                        }

                        Text(
                            text = "Popular Hadiths:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularHadithsForBook) { (hNum, title) ->
                                AssistChip(
                                    onClick = { hadithNumberInput = hNum },
                                    label = { Text(title, fontSize = 10.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (hadithNumberInput == hNum) EmeraldContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (hadithNumberInput == hNum) EmeraldPrimary else MintBorder
                                    )
                                )
                            }
                        }

                        // Hadith Number Input
                        OutlinedTextField(
                            value = hadithNumberInput,
                            onValueChange = { hadithNumberInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Hadith # (1 - ${selectedBook.totalHadithCount})") },
                            placeholder = { Text("e.g. 1") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Include Translation Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldContainer.copy(alpha = 0.4f))
                                .clickable { includeHadithTranslation = !includeHadithTranslation }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Include Translation",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Switch(
                                checked = includeHadithTranslation,
                                onCheckedChange = { includeHadithTranslation = it }
                            )
                        }

                        // Hadith Preview Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MintBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLoadingHadith) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = EmeraldPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading Hadith...",
                                            fontSize = 12.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    if (loadedHadithText.isNotBlank()) {
                                        Text(
                                            text = loadedHadithText,
                                            style = AlDeenTypography.ArabicBody.copy(
                                                fontSize = 14.5.sp,
                                                lineHeight = 22.sp,
                                                textAlign = TextAlign.Center,
                                                color = EmeraldPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (includeHadithTranslation && loadedHadithTrans.isNotBlank()) {
                                        if (loadedHadithText.isNotBlank()) {
                                            HorizontalDivider(color = MintBorder.copy(alpha = 0.5f))
                                        }
                                        Text(
                                            text = loadedHadithTrans,
                                            style = AlDeenTypography.UrduBody.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Center,
                                                color = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (loadedHadithText.isBlank() && loadedHadithTrans.isBlank()) {
                                        Text(
                                            text = "No Hadith text found for this number",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = TextSecondary,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Auto-font note (Arabic built-in + Urdu built-in)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "The Ayah will be in Arabic font and Trasnaltion will be in Urdu font",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (finalResult, finalSourceType) = when (selectedTab) {
                        InsertContentType.MANUAL -> manualText.trim() to HappinessSourceType.CUSTOM
                        InsertContentType.QURAN -> {
                            val res = loadedAyah?.let { ayah ->
                                if (includeQuranTranslation && ayah.urduTranslation.isNotBlank()) {
                                    "${ayah.textArabic}\n\n${ayah.urduTranslation}\n(سورة ${selectedSurah.arabicName}: ${ayah.numberInSurah})"
                                } else {
                                    "${ayah.textArabic}\n(سورة ${selectedSurah.arabicName}: ${ayah.numberInSurah})"
                                }
                            } ?: manualText.trim()
                            res to HappinessSourceType.QURAN
                        }
                        InsertContentType.HADITH -> {
                            val citation = "(${selectedBook.urduName.ifBlank { selectedBook.englishName }}: ${hadithNumberInput})"
                            val res = if (includeHadithTranslation && loadedHadithTrans.isNotBlank()) {
                                if (loadedHadithText.isNotBlank()) {
                                    "$loadedHadithText\n\n$loadedHadithTrans\n$citation"
                                } else {
                                    "$loadedHadithTrans\n$citation"
                                }
                            } else {
                                "$loadedHadithText\n$citation"
                            }
                            res to HappinessSourceType.HADITH
                        }
                    }
                    if (finalResult.isNotBlank()) {
                        onConfirm(finalResult, selectedFont, finalSourceType)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(if (initialText.isNotBlank() && selectedTab == InsertContentType.MANUAL) "Update" else "Insert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
