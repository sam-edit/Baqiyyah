package com.example.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Surah
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSurahPickerBottomSheet(
    surahs: List<Surah>,
    selectedSurahNumber: Int,
    downloadedSurahs: Set<Int>,
    currentReciterDownloadedMap: Map<Int, Set<String>>,
    currentReciterId: String,
    onSelectSurah: (Surah) -> Unit,
    onDismiss: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }

    val filteredSurahs = remember(filterQuery, surahs) {
        if (filterQuery.isBlank()) {
            surahs
        } else {
            val q = filterQuery.trim().lowercase()
            surahs.filter { surah ->
                surah.number.toString() == q ||
                surah.englishName.lowercase().contains(q) ||
                surah.arabicName.contains(q) ||
                surah.urduName.contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = EmeraldPrimary.copy(alpha = 0.5f)) },
        modifier = Modifier.testTag("surah_picker_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Select Surah",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    AlDeenText(
                        text = "سورت منتخب کریں (1-114)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Filter Field
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_picker_search_input"),
                placeholder = { Text("Search by Surah name or number (e.g. Yasin, 36)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Surah List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSurahs, key = { it.number }) { surah ->
                    val isSelected = (surah.number == selectedSurahNumber)
                    val isDownloadedForCurrentReciter =
                        currentReciterDownloadedMap[surah.number]?.contains(currentReciterId) == true ||
                        (currentReciterId == "ar.alafasy" && downloadedSurahs.contains(surah.number))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectSurah(surah)
                                onDismiss()
                            }
                            .testTag("surah_picker_item_${surah.number}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) EmeraldContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Surah Number badge
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${surah.number}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = surah.englishName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) EmeraldPrimary else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "• ${surah.numberOfAyahs} ayahs",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary
                                        )
                                    )
                                }

                                Text(
                                    text = surah.englishTranslation,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Arabic Name
                            AlDeenText(
                                text = surah.arabicName,
                                style = AlDeenTypography.ArabicSubheading.copy(
                                    color = EmeraldPrimary,
                                    fontSize = 17.sp
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Download status badge
                            if (isDownloadedForCurrentReciter) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Downloaded",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Not Downloaded",
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
