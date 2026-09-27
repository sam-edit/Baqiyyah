package com.example.ui.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.hadith.HadithLanguage
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithTranslationBottomSheet(
    viewModel: HadithViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val selectedLangCode by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()
    val currentBook by viewModel.selectedBook.collectAsStateWithLifecycle()
    val langStatusMap by viewModel.languageDownloadStatusMap.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("hadith_translation_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlDeenTokens.SpacingLarge)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        AlDeenText(
                            text = "ترجمہ اور زبانیں (Hadith Translations)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = currentBook.englishName,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_hadith_translation_sheet")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. TOP SLIDER / SWITCH: Turn Translation ON / OFF
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hadith_translation_on_off_card"),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(
                    containerColor = if (isTranslationEnabled) EmeraldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, if (isTranslationEnabled) EmeraldPrimary.copy(alpha = 0.6f) else MintBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AlDeenText(
                            text = "ترجمہ فعال کریں (Show Translation)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AlDeenText(
                            text = if (isTranslationEnabled) "عربی حدیث کے ساتھ ترجمہ نظر آئے گا" else "صرف عربی متن دکھایا جائے گا (ڈیفالٹ)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isTranslationEnabled) EmeraldPrimary else TextSecondary,
                                fontSize = 12.5.sp
                            )
                        )
                    }

                    Switch(
                        checked = isTranslationEnabled,
                        onCheckedChange = { viewModel.setTranslationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("hadith_translation_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. AVAILABLE LANGUAGES LIST
            AlDeenText(
                text = "دستیاب زبانیں (Available API Languages)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
            AlDeenText(
                text = "مطلوبہ زبان کا ترجمہ ڈاؤن لوڈ کر کے منتخب کریں",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableLanguages = viewModel.getLanguagesForBook(currentBook.slug)
                items(availableLanguages) { lang ->
                    val downloadKey = "${currentBook.slug}_${lang.code}"
                    val state = langStatusMap[downloadKey] ?: if (viewModel.isLanguageDownloaded(currentBook.slug, lang.code)) {
                        HadithDownloadState.Downloaded
                    } else {
                        HadithDownloadState.NotDownloaded
                    }
                    val isDownloaded = state is HadithDownloadState.Downloaded
                    val isSelected = selectedLangCode == lang.code

                    HadithLanguageItemCard(
                        language = lang,
                        state = state,
                        isDownloaded = isDownloaded,
                        isSelected = isSelected,
                        onDownload = {
                            viewModel.downloadLanguage(currentBook.slug, lang.code)
                        },
                        onDelete = {
                            viewModel.deleteLanguage(currentBook.slug, lang.code)
                        },
                        onSelect = {
                            viewModel.selectLanguage(lang.code)
                            viewModel.setTranslationEnabled(true)
                            if (!isDownloaded) {
                                viewModel.downloadLanguage(currentBook.slug, lang.code)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithLanguageItemCard(
    language: HadithLanguage,
    state: HadithDownloadState,
    isDownloaded: Boolean,
    isSelected: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AlDeenTokens.ShapeCard)
            .clickable { onSelect() }
            .testTag("hadith_lang_${language.code}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MintBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Radio button for selection
                    RadioButton(
                        selected = isSelected,
                        onClick = if (isDownloaded) onSelect else null,
                        enabled = isDownloaded,
                        colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary),
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = language.englishName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AlDeenText(
                                text = "(${language.nativeName})",
                                style = AlDeenTypography.UrduBody.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                        AlDeenText(
                            text = if (isDownloaded) "ڈاؤن لوڈ شدہ • مطالعہ کے لیے تیار" else "ڈاؤن لوڈ درکار ہے",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDownloaded) EmeraldPrimary else TextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Action area: Download button or Delete icon
                when (state) {
                    is HadithDownloadState.Downloading -> {
                        // Downloading progress indicator (showing percentage, no MBs)
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.width(90.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(AlDeenTokens.ShapePill),
                                color = EmeraldPrimary,
                                trackColor = EmeraldContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    is HadithDownloadState.Downloaded -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Downloaded",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp).testTag("delete_hadith_lang_${language.code}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete translation",
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
                                .height(32.dp)
                                .testTag("download_hadith_lang_${language.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AlDeenText(
                                text = "ڈاؤن لوڈ",
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
}
