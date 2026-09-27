package com.example.ui.quran

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
import com.example.data.quran.QuranTranslationLanguage
import com.example.data.quran.TranslationDownloadState
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTranslationBottomSheet(
    viewModel: QuranViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedTranslationId.collectAsStateWithLifecycle()
    val statusMap by viewModel.translationDownloadStatusMap.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("quran_translation_sheet")
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
                            text = "قرآن پاک کے تراجم (Quran Translations)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = "تمام مستند تراجم اور زبانیں",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_quran_translation_sheet")) {
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
                    .testTag("quran_translation_on_off_card"),
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
                            text = "ترجمہ دکھائیں (Show Translation)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AlDeenText(
                            text = if (isTranslationEnabled) "آیات کے نیچے منتخب زبان میں ترجمہ فعال ہے" else "صرف عربی قرآن دکھایا جائے گا (سادہ قرآن)",
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
                        modifier = Modifier.testTag("quran_translation_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. AVAILABLE LANGUAGES LIST
            AlDeenText(
                text = "دستیاب زبانیں اور تراجم (Available API Languages)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
            AlDeenText(
                text = "اپنی پسندیدہ زبان منتخب کریں یا مکمل ترجمہ ڈاؤن لوڈ کریں",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.supportedTranslationLanguages) { lang ->
                    val state = statusMap[lang.id] ?: if (viewModel.isTranslationDownloaded(lang.id)) {
                        TranslationDownloadState.Downloaded
                    } else {
                        TranslationDownloadState.NotDownloaded
                    }
                    val isDownloaded = lang.isBuiltIn || state is TranslationDownloadState.Downloaded
                    val isSelected = selectedId == lang.id

                    QuranLanguageItemCard(
                        language = lang,
                        state = state,
                        isDownloaded = isDownloaded,
                        isSelected = isSelected,
                        onDownload = { viewModel.downloadQuranTranslation(lang.id) },
                        onDelete = { viewModel.deleteQuranTranslation(lang.id) },
                        onSelect = {
                            viewModel.selectQuranTranslation(lang.id)
                            viewModel.setTranslationEnabled(true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranLanguageItemCard(
    language: QuranTranslationLanguage,
    state: TranslationDownloadState,
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
            .testTag("quran_lang_${language.id}"),
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
                        Text(
                            text = language.author,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Action area
                if (language.isBuiltIn) {
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = EmeraldContainer,
                        border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            AlDeenText(
                                text = "بلٹ اِن (Ready)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }
                } else {
                    when (state) {
                        is TranslationDownloadState.Downloading -> {
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
                        is TranslationDownloadState.Downloaded -> {
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
                                    modifier = Modifier.size(32.dp).testTag("delete_quran_lang_${language.id}")
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
                                    .testTag("download_quran_lang_${language.id}")
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
}
