package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthoritativeContentProvider
import com.example.domain.model.BookmarkItem
import com.example.domain.model.BookmarkType
import com.example.domain.model.Ayah
import com.example.domain.model.Hadith
import com.example.domain.model.DuaItem
import com.example.ui.theme.*

enum class MomentType(val label: String, val urduLabel: String, val icon: ImageVector) {
    AYAH("Ayah", "آیت", Icons.AutoMirrored.Filled.MenuBook),
    HADEETH("Hadith", "حدیث", Icons.Default.LibraryBooks),
    WORD("Word", "لفظ", Icons.Default.Translate),
    DUA("Dua", "دعا", Icons.Default.VolunteerActivism)
}

@Composable
fun HomeMomentSection(
    onPlayAudio: (String, String) -> Unit,
    isPlayingAudio: Boolean,
    onToggleBookmark: (BookmarkItem) -> Unit,
    isBookmarked: (String) -> Boolean,
    ayah: Ayah,
    hadith: Hadith,
    word: AuthoritativeContentProvider.WordOfTheMoment,
    dua: DuaItem,
    onRefreshMoment: (MomentType?) -> Unit,
    ayahTranslation: String = "",
    hadithTranslation: String = "",
    onOpenAyahInQuran: (Int, Int) -> Unit = { _, _ -> },
    onOpenHadithInBook: (String, Int, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // Completely random initial selection on launch
    var selectedMoment by remember { mutableStateOf(MomentType.entries.random()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_moment_section")
    ) {
        // Heading with Refresh prompt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlDeenText(
                text = "Moment of Reflection (لمحاتِ فکر)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            IconButton(
                onClick = {
                    onRefreshMoment(null)
                },
                modifier = Modifier.size(32.dp).testTag("refresh_all_moments")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Moments (سب ریفریش کریں)",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Moment Navigation Menu (Aik cheez at a time, selectable tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MomentType.entries.forEach { type ->
                val isSelected = selectedMoment == type
                Surface(
                    onClick = {
                        selectedMoment = type
                        onRefreshMoment(type)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("moment_menu_tab_${type.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = type.label,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 7. Home Moment Content: Switches smoothly showing ONLY one item at a time
        AnimatedContent(
            targetState = selectedMoment,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "moment_content_anim"
        ) { currentMoment ->
            when (currentMoment) {
                MomentType.AYAH -> AyahMomentCard(
                    ayah = ayah,
                    activeTranslation = ayahTranslation,
                    onPlayAudio = onPlayAudio,
                    isPlayingAudio = isPlayingAudio,
                    onToggleBookmark = onToggleBookmark,
                    isBookmarked = isBookmarked,
                    onRefresh = { onRefreshMoment(MomentType.AYAH) }
                )
                MomentType.HADEETH -> HadeethMomentCard(
                    hadith = hadith,
                    activeTranslation = hadithTranslation,
                    onToggleBookmark = onToggleBookmark,
                    isBookmarked = isBookmarked,
                    onRefresh = { onRefreshMoment(MomentType.HADEETH) }
                )
                MomentType.WORD -> WordMomentCard(
                    word = word,
                    onRefresh = { onRefreshMoment(MomentType.WORD) }
                )
                MomentType.DUA -> DuaMomentCard(
                    dua = dua,
                    onToggleBookmark = onToggleBookmark,
                    isBookmarked = isBookmarked,
                    onRefresh = { onRefreshMoment(MomentType.DUA) }
                )
            }
        }
    }
}

@Composable
private fun AyahMomentCard(
    ayah: Ayah,
    activeTranslation: String,
    onPlayAudio: (String, String) -> Unit,
    isPlayingAudio: Boolean,
    onToggleBookmark: (BookmarkItem) -> Unit,
    isBookmarked: (String) -> Boolean,
    onRefresh: () -> Unit
) {
    val bookmarkRef = "surah_${ayah.surahNumber}_ayah_${ayah.numberInSurah}"
    val bookmarked = isBookmarked(bookmarkRef)
    val surah = AuthoritativeContentProvider.allSurahs.firstOrNull { it.number == ayah.surahNumber }
    val surahName = surah?.englishName ?: "Surah ${ayah.surahNumber}"
    val surahUrdu = surah?.urduName ?: ""
    val displayedTranslation = activeTranslation.ifBlank { ayah.urduTranslation.ifBlank { ayah.textTranslation } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_ayah"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            // Header Row: Surah Name & Ayah Number + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ayah of the Moment",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    AlDeenText(
                        text = "$surahName ($surahUrdu) • Ayah ${ayah.numberInSurah}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Refresh Button
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Random Ayah",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Audio Play
                    IconButton(
                        onClick = {
                            ayah.audioUrl?.let { onPlayAudio(it, "ayah_moment_${ayah.number}") }
                        },
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (isPlayingAudio) "Pause Ayah Audio" else "Play Ayah Audio",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Bookmark
                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                BookmarkItem(
                                    type = BookmarkType.QURAN_AYAH,
                                    referenceId = bookmarkRef,
                                    title = "$surahName ${ayah.surahNumber}:${ayah.numberInSurah}",
                                    subtitle = surahUrdu,
                                    arabicText = ayah.textArabic,
                                    translation = displayedTranslation
                                )
                            )
                        },
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (bookmarked) "Remove bookmark" else "Add to bookmarks",
                            tint = if (bookmarked) GoldAccent else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text
            AlDeenText(
                text = ayah.textArabic,
                style = AlDeenTypography.QuranAyahText.copy(fontSize = 20.sp, lineHeight = 36.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Language Translation
            if (displayedTranslation.isNotBlank()) {
                AlDeenText(
                    text = displayedTranslation,
                    style = AlDeenTypography.UrduBody.copy(
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HadeethMomentCard(
    hadith: Hadith,
    activeTranslation: String,
    onToggleBookmark: (BookmarkItem) -> Unit,
    isBookmarked: (String) -> Boolean,
    onRefresh: () -> Unit
) {
    val bookmarkRef = "hadith_${hadith.bookSlug}_${hadith.hadithNumber}"
    val bookmarked = isBookmarked(bookmarkRef)
    val isNotDownloaded = hadith.bookSlug == "not_downloaded" || hadith.id < 0
    val displayedTranslation = activeTranslation.ifBlank { hadith.activeTranslation.ifBlank { hadith.urduTranslation.ifBlank { hadith.englishTranslation } } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_hadeeth"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hadeeth of the Moment",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    AlDeenText(
                        text = if (isNotDownloaded) "کوئی کتاب ڈاؤن لوڈ نہیں" else "${hadith.bookName} • Hadith #${hadith.hadithNumber}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Random Hadith",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!isNotDownloaded) {
                        IconButton(
                            onClick = {
                                onToggleBookmark(
                                    BookmarkItem(
                                        type = BookmarkType.HADITH,
                                        referenceId = bookmarkRef,
                                        title = "${hadith.bookName} #${hadith.hadithNumber}",
                                        subtitle = hadith.chapterNameUrdu,
                                        arabicText = hadith.arabicText,
                                        translation = displayedTranslation
                                    )
                                )
                            },
                            modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                        ) {
                            Icon(
                                imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Hadith",
                                tint = if (bookmarked) GoldAccent else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isNotDownloaded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AlDeenText(
                            text = "ابھی کوئی حدیث کی کتاب ڈاؤن لوڈ نہیں ہے",
                            style = AlDeenTypography.UrduHeading.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AlDeenText(
                            text = "براہ کرم حدیث سیکشن سے اپنی پسندیدہ کتاب ڈاؤن لوڈ کریں تاکہ یہاں ڈاؤن لوڈ شدہ کتابوں میں سے لمحاتی حدیث ظاہر ہو سکے۔",
                            style = AlDeenTypography.UrduBody.copy(fontSize = 13.sp, lineHeight = 20.sp),
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                if (hadith.arabicText.isNotBlank()) {
                    AlDeenText(
                        text = hadith.arabicText,
                        style = AlDeenTypography.HadithArabicText.copy(fontSize = 18.sp, lineHeight = 32.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (displayedTranslation.isNotBlank()) {
                    AlDeenText(
                        text = displayedTranslation,
                        style = AlDeenTypography.UrduBody.copy(
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WordMomentCard(
    word: AuthoritativeContentProvider.WordOfTheMoment,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_word"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quranic Word of the Moment",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Random Word",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    AlDeenText(
                        text = word.arabicWord,
                        style = AlDeenTypography.ArabicHeading.copy(
                            fontSize = 32.sp,
                            color = EmeraldPrimary
                        )
                    )
                    Text(
                        text = "Pronunciation: ${word.pronunciation}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    )
                }

                Surface(
                    shape = AlDeenTokens.ShapePill,
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "Key Concept",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OnEmeraldContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MintBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            AlDeenText(
                text = "اردو معنی: ${word.urduMeaning}",
                style = AlDeenTypography.UrduBody.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "English: ${word.englishMeaning}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = word.context,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun DuaMomentCard(
    dua: DuaItem,
    onToggleBookmark: (BookmarkItem) -> Unit,
    isBookmarked: (String) -> Boolean,
    onRefresh: () -> Unit
) {
    val bookmarkRef = "dua_${dua.id}"
    val bookmarked = isBookmarked(bookmarkRef)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_dua"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AlDeenTokens.ElevationMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlDeenTokens.SpacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dua of the Moment",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "${dua.title} • ${dua.reference}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Random Dua",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onToggleBookmark(
                                BookmarkItem(
                                    type = BookmarkType.DUA,
                                    referenceId = bookmarkRef,
                                    title = dua.title,
                                    subtitle = dua.reference,
                                    arabicText = dua.arabicText,
                                    translation = dua.urduTranslation
                                )
                            )
                        },
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Dua",
                            tint = if (bookmarked) GoldAccent else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AlDeenText(
                text = dua.arabicText,
                style = AlDeenTypography.QuranAyahText.copy(fontSize = 19.sp, lineHeight = 34.sp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dua.transliteration,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            AlDeenText(
                text = dua.urduTranslation,
                style = AlDeenTypography.UrduBody.copy(
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dua.englishTranslation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )
        }
    }
}
