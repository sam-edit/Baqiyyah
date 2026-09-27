package com.example.ui.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.BookmarksRepository
import com.example.domain.model.BookmarkItem
import com.example.ui.components.AlDeenText
import com.example.domain.model.BookmarkType
import com.example.domain.model.DuaCategory
import com.example.domain.model.DuaItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaZikrScreen(
    bookmarksRepository: BookmarksRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bookmarkedRefs by bookmarksRepository.allBookmarks
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarkedSet = remember(bookmarkedRefs) { bookmarkedRefs.map { it.referenceId }.toSet() }

    val categories = AuthoritativeContentProvider.duaCategories
    val allDuas = AuthoritativeContentProvider.curatedDuas

    var selectedCategory by remember { mutableStateOf<DuaCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Intercept back button if viewing a category or searching
    BackHandler(enabled = selectedCategory != null || searchQuery.isNotBlank()) {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else {
            selectedCategory = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = if (selectedCategory != null) {
                                selectedCategory!!.englishName
                            } else if (searchQuery.isNotBlank()) {
                                "Search Duas"
                            } else {
                                "Duas & Azkar (الأدعية والأذكار)"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = if (selectedCategory != null) {
                                selectedCategory!!.urduName
                            } else {
                                "Authentic Daily Supplications & Fortress Azkar"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            targetScript = if (selectedCategory != null) ScriptLanguage.URDU else null
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedCategory != null) {
                                selectedCategory = null
                            } else if (searchQuery.isNotBlank()) {
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("duas_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("duas_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { AlDeenText("Search by topic, Arabic, Urdu (دعائیں), English...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
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
                    .testTag("duas_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (searchQuery.isNotBlank()) {
                // Search Results
                val filteredDuas = remember(searchQuery) {
                    allDuas.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                        it.urduTranslation.contains(searchQuery) ||
                        it.arabicText.contains(searchQuery) ||
                        it.transliteration.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filteredDuas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No supplications found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filteredDuas, key = { it.id }) { dua ->
                            DuaCard(
                                dua = dua,
                                isBookmarked = bookmarkedSet.contains("dua_${dua.id}"),
                                onToggleBookmark = {
                                    coroutineScope.launch {
                                        val ref = "dua_${dua.id}"
                                        if (bookmarkedSet.contains(ref)) {
                                            bookmarksRepository.removeBookmark(ref)
                                        } else {
                                            bookmarksRepository.toggleBookmark(
                                                BookmarkItem(
                                                    type = BookmarkType.DUA,
                                                    referenceId = ref,
                                                    title = dua.title,
                                                    subtitle = dua.reference,
                                                    arabicText = dua.arabicText,
                                                    translation = dua.urduTranslation
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (selectedCategory != null) {
                // Category Detail View
                val categoryDuas = remember(selectedCategory) {
                    allDuas.filter { it.category.equals(selectedCategory!!.englishName, ignoreCase = true) ||
                            it.category.contains(selectedCategory!!.englishName.split(" ").first(), ignoreCase = true) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${categoryDuas.size} Supplications",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    TextButton(onClick = { selectedCategory = null }) {
                        Text("View All Blocks", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (categoryDuas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No duas available in this category yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(categoryDuas, key = { it.id }) { dua ->
                            DuaCard(
                                dua = dua,
                                isBookmarked = bookmarkedSet.contains("dua_${dua.id}"),
                                onToggleBookmark = {
                                    coroutineScope.launch {
                                        val ref = "dua_${dua.id}"
                                        if (bookmarkedSet.contains(ref)) {
                                            bookmarksRepository.removeBookmark(ref)
                                        } else {
                                            bookmarksRepository.toggleBookmark(
                                                BookmarkItem(
                                                    type = BookmarkType.DUA,
                                                    referenceId = ref,
                                                    title = dua.title,
                                                    subtitle = dua.reference,
                                                    arabicText = dua.arabicText,
                                                    translation = dua.urduTranslation
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Main Category Blocks View
                Text(
                    text = "Dua & Azkar Categories",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize().testTag("dua_categories_grid")
                ) {
                    items(categories, key = { it.id }) { category ->
                        val duaCount = allDuas.count {
                            it.category.equals(category.englishName, ignoreCase = true) ||
                            it.category.contains(category.englishName.split(" ").first(), ignoreCase = true)
                        }

                        DuaCategoryBlock(
                            category = category,
                            duaCount = duaCount,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaCategoryBlock(
    category: DuaCategory,
    duaCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
            .testTag("dua_block_${category.id}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distinctive Symbol
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategorySymbol(category.id),
                            contentDescription = category.englishName,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (duaCount > 0) {
                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$duaCount Duas",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = category.englishName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                AlDeenText(
                    text = category.urduName,
                    style = AlDeenTypography.UrduBody.copy(
                        fontSize = 11.5.sp,
                        color = EmeraldPrimary
                    ),
                    targetScript = ScriptLanguage.URDU,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: DuaItem,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = dua.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OnEmeraldContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (dua.repeatCount > 1) {
                            Surface(
                                shape = AlDeenTokens.ShapePill,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Repeat ${dua.repeatCount}x",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dua.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = dua.reference,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) GoldAccent else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text
            AlDeenText(
                text = dua.arabicText,
                style = AlDeenTypography.QuranAyahText.copy(fontSize = 19.sp, lineHeight = 34.sp),
                targetScript = ScriptLanguage.ARABIC,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transliteration
            Text(
                text = dua.transliteration,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MintBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Urdu translation
            AlDeenText(
                text = dua.urduTranslation,
                style = AlDeenTypography.UrduBody.copy(
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = TextPrimary
                ),
                targetScript = ScriptLanguage.URDU,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(6.dp))

            // English translation
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

private fun getCategorySymbol(categoryId: String): ImageVector {
    return when (categoryId) {
        "travel" -> Icons.Default.Flight
        "before_salah" -> Icons.Default.Schedule
        "after_salah" -> Icons.Default.CheckCircle
        "food" -> Icons.Default.Restaurant
        "sleep" -> Icons.Default.NightsStay
        "morning_evening" -> Icons.Default.WbSunny
        "protection" -> Icons.Default.Security
        "home" -> Icons.Default.Home
        "mosque" -> Icons.Default.LocationCity
        "distress" -> Icons.Default.SentimentSatisfied
        "rain_nature" -> Icons.Default.Cloud
        "fasting" -> Icons.Default.Bedtime
        "hajj_umrah" -> Icons.Default.Navigation
        "forgiveness" -> Icons.Default.Favorite
        "sickness" -> Icons.Default.LocalHospital
        "social" -> Icons.Default.People
        "funeral" -> Icons.Default.Place
        "praise" -> Icons.Default.Star
        else -> Icons.Default.AutoStories
    }
}
