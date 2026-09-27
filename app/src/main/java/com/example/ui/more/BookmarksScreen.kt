package com.example.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BookmarksRepository
import com.example.domain.model.BookmarkItem
import com.example.domain.model.BookmarkType
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    bookmarksRepository: BookmarksRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allBookmarks by bookmarksRepository.allBookmarks.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedFilter by remember { mutableStateOf<BookmarkType?>(null) }

    val filteredBookmarks = remember(allBookmarks, selectedFilter) {
        if (selectedFilter == null) allBookmarks
        else allBookmarks.filter { it.type == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AlDeenText(
                        text = "Saved Bookmarks (المحفوظات)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("bookmarks_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("bookmarks_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${allBookmarks.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                BookmarkType.values().forEach { type ->
                    val count = allBookmarks.count { it.type == type }
                    item {
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = type },
                            label = { Text("${type.displayName} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredBookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AlDeenTokens.SpacingXXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No bookmarks saved yet",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the bookmark icon on any Ayah, Hadith, or Dua to keep it here for quick reference.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                    contentPadding = PaddingValues(bottom = AlDeenTokens.SpacingXXLarge)
                ) {
                    items(filteredBookmarks, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    Column {
                                        Surface(
                                            shape = AlDeenTokens.ShapePill,
                                            color = EmeraldContainer
                                        ) {
                                            Text(
                                                text = item.type.displayName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = OnEmeraldContainer,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AlDeenText(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        if (item.subtitle.isNotBlank()) {
                                            AlDeenText(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                bookmarksRepository.removeBookmarkById(item.id)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkRemove,
                                            contentDescription = "Delete Bookmark",
                                            tint = Color(0xFFC0392B)
                                        )
                                    }
                                }

                                if (item.arabicText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AlDeenText(
                                        text = item.arabicText,
                                        style = AlDeenTypography.ArabicBody.copy(
                                            fontSize = 17.sp,
                                            color = EmeraldPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (item.translation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AlDeenText(
                                        text = item.translation,
                                        style = AlDeenTypography.UrduBody.copy(
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
