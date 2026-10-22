package com.example.ui.quran16

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthoritativeContentProvider
import com.example.ui.theme.*

@Composable
internal fun ObservePdfFile(store: Quran16Store) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, store) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) store.refresh()
        }
        owner.lifecycle.addObserver(observer)
        store.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Enhanced, interactive cards for the main Quran tab selection hub.
 * Responsive and polished for both Light and Dark modes.
 */
@Composable
fun QuranEntryCards(
    onTranslation: () -> Unit,
    onPdf: () -> Unit
) {
    val store = Quran16Store.get(LocalContext.current)
    val status by store.status.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    ObservePdfFile(store)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Tarjma-Tul-Quran Card ---
        Card(
            onClick = onTranslation,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("al_quran_block"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MintSurface
            ),
            border = BorderStroke(
                width = 1.2.dp,
                color = EmeraldPrimary.copy(alpha = if (LocalAppIsDark.current) 0.45f else 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Tarjma-Tul-Quran",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = EmeraldContainer
                    ) {
                        Text(
                            text = "Translation • 30 Paras",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = OnEmeraldContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tarjma-Tul-Quran",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "القرآن الكريم مع الترجمة",
                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Read all 114 Surahs with authentic various translations, word-by-word meanings, and offline audio.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Feature tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniFeatureTag("Various Languages", Modifier.weight(1f))
                    MiniFeatureTag("Audio Recitation", Modifier.weight(1f))
                    MiniFeatureTag("Word-by-Word", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTranslation,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = AlDeenTokens.ShapePill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Tarjma-Tul-Quran",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 2. 16 Line Quran Card (Identical size & structure) ---
        Card(
            onClick = {
                if (status.pages > 0) onPdf() else if (!status.busy && !status.checking) store.download()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MintSurface
            ),
            border = BorderStroke(
                width = 1.2.dp,
                color = GoldAccent.copy(alpha = if (LocalAppIsDark.current) 0.5f else 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GoldLight,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "16 Line Quran",
                                tint = GoldAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Surface(
                        shape = AlDeenTokens.ShapePill,
                        color = GoldLight
                    ) {
                        Text(
                            text = "Tajweed & Hifz • 30 Paras",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "16 Line Quran",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (LocalAppIsDark.current) GoldAccent else Color(0xFFB45309),
                        fontSize = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "١٦ سطر قرآن پاک",
                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Classic 16-line Tajweed script ideal Quran for Hifz and full offline reading.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Feature tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniFeatureTag("16 Lines Format", Modifier.weight(1f))
                    MiniFeatureTag("Night Filter", Modifier.weight(1f))
                    MiniFeatureTag("Full Offline", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Area: EXACTLY 48.dp height to match Tarjma card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (status.checking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Checking local PDF…",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    } else if (status.busy) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (status.progress == null) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(AlDeenTokens.ShapePill),
                                        color = EmeraldPrimary
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = { status.progress!! },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(AlDeenTokens.ShapePill),
                                        color = EmeraldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = status.progress?.let { "Downloading ${(it * 100).toInt()}%" } ?: "Downloading PDF…",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            TextButton(
                                onClick = store::cancelDownload,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (status.pages > 0) {
                        // Downloaded: Open 16 Line Quran button + Delete button (as requested)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = AlDeenTokens.ShapePill,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open 16 Line Quran", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Button(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = AlDeenTokens.ShapePill,
                                modifier = Modifier.fillMaxHeight(),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Not Downloaded: Download button
                        Button(
                            onClick = { store.download() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (LocalAppIsDark.current) Color(0xFFD97706) else Color(0xFFB45309)
                            ),
                            shape = AlDeenTokens.ShapePill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete downloaded PDF?",
            text = "Reading history, bookmarks, and last-read page will remain saved.",
            action = "Delete",
            dismiss = { confirmDelete = false }
        ) {
            confirmDelete = false
            store.delete()
        }
    }
}

@Composable
private fun MiniFeatureTag(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = AlDeenTokens.ShapeSmallCard,
        color = MintSurfaceVariant,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Modern, interactive 16-Line Quran Hub & Index Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Quran16Screen(onBack: () -> Unit) {
    val store = Quran16Store.get(LocalContext.current)
    val status by store.status.collectAsStateWithLifecycle()
    val recent by store.recent.collectAsStateWithLifecycle()
    val bookmarks by store.bookmarks.collectAsStateWithLifecycle()

    var route by rememberSaveable { mutableStateOf("hub") }
    var readerParent by rememberSaveable { mutableStateOf("hub") }
    var readerPage by rememberSaveable { mutableIntStateOf(1) }
    var selectedNumber by rememberSaveable { mutableIntStateOf(0) }
    var selectedJuz by rememberSaveable { mutableStateOf(true) }
    var goTo by rememberSaveable { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var surahSearchQuery by remember { mutableStateOf("") }

    val filteredSurahs = remember(surahSearchQuery) {
        if (surahSearchQuery.isBlank()) {
            AuthoritativeContentProvider.allSurahs
        } else {
            val q = surahSearchQuery.trim().lowercase()
            AuthoritativeContentProvider.allSurahs.filter {
                it.number.toString() == q ||
                    it.englishName.lowercase().contains(q) ||
                    it.urduName.contains(q)
            }
        }
    }

    ObservePdfFile(store)

    fun back() {
        if (route == "reader") {
            route = readerParent
        } else if (route != "hub") {
            route = "hub"
        } else {
            onBack()
        }
    }

    fun ready(): Boolean {
        if (status.checking) {
            message = "Checking local PDF. Please wait a moment."
            return false
        }
        if (status.pages == 0) {
            message = "Please download the 16 Line Quran PDF first."
            return false
        }
        return true
    }

    fun open(page: Int) {
        if (!ready()) return
        if (!Quran16Mappings.valid(page, status.pages)) {
            message = "PDF page $page is outside this document (1–${status.pages})."
        } else {
            readerParent = route
            readerPage = page
            route = "reader"
        }
    }

    BackHandler { back() }

    if (route == "reader") {
        Quran16Reader(store, readerPage, onBack = { back() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (route) {
                            "juz" -> "Juz (Para) Index"
                            "surah" -> "Surah Index"
                            "recent" -> "Recent Locations"
                            else -> "16 Line Quran"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    if (route == "recent" && recent.isNotEmpty()) {
                        TextButton(onClick = { clear = true }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MintBackground
                )
            )
        },
        containerColor = MintBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (route) {
                "hub" -> {
                    // Hero Banner Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = AlDeenTokens.ShapePill,
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "Indo-Pak Tajweed Script",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = AlDeenTokens.ShapePill,
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${status.pages.coerceAtLeast(560)} Pages",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "16 Line Holy Quran",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "المصحف الشريف ستة عشر سطراً",
                                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 18.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Clear South Asian 16-line typography with Para, quarter markers, and bookmarking.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        lineHeight = 20.sp
                                    )
                                )

                                if (store.lastPage > 1 && status.pages > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { open(store.lastPage) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF0F4736)
                                        ),
                                        shape = AlDeenTokens.ShapePill,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Resume Reading • Page ${store.lastPage}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4 Feature Cards Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModernHubCard(
                                title = "Juz (Para) Index",
                                subtitle = "٣٠ پارے",
                                badge = "30 Paras",
                                icon = Icons.Default.FormatListNumbered,
                                modifier = Modifier.weight(1f),
                                onClick = { route = "juz" }
                            )

                            ModernHubCard(
                                title = "Surah Index",
                                subtitle = "۱۱۴ سورتیں",
                                badge = "114 Surahs",
                                icon = Icons.Default.MenuBook,
                                modifier = Modifier.weight(1f),
                                onClick = { route = "surah" }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModernHubCard(
                                title = "Recent Pages",
                                subtitle = "تاریخچہ",
                                badge = "${recent.size} Locations",
                                icon = Icons.Default.History,
                                modifier = Modifier.weight(1f),
                                onClick = { route = "recent" }
                            )

                            ModernHubCard(
                                title = "Go to Page",
                                subtitle = "براہ راست صفحہ",
                                badge = "1–${if (status.pages > 0) status.pages else 560}",
                                icon = Icons.Default.FindInPage,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (ready()) goTo = true
                                }
                            )
                        }
                    }

                    // Bookmarks Preview section
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saved Bookmarks (${bookmarks.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        items(bookmarks, key = { "bm_$it" }) { bPage ->
                            Card(
                                onClick = { open(bPage) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AlDeenTokens.ShapeCard,
                                colors = CardDefaults.cardColors(containerColor = MintSurface),
                                border = BorderStroke(1.dp, MintBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = GoldLight,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "PDF Page $bPage",
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Para ${Quran16Mappings.juzForPage(bPage)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                "juz" -> {
                    item {
                        Text(
                            text = "Tap any Para to choose start or quarter (Ruba, Nisf, Salasah):",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }

                    items(AuthoritativeContentProvider.allJuz, key = { it.juzNumber }) { juz ->
                        val startPage = Quran16Mappings.juz(juz.juzNumber, 0) ?: 2
                        Card(
                            onClick = {
                                selectedJuz = true
                                selectedNumber = juz.juzNumber
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MintSurface),
                            border = BorderStroke(1.dp, MintBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${juz.juzNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${juz.juzNumber}. Para",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Starts at Page $startPage",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }

                                Text(
                                    text = juz.urduName,
                                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                                    fontSize = 18.sp,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.End
                                )

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View quarters",
                                    tint = TextTertiary
                                )
                            }
                        }
                    }
                }

                "surah" -> {
                    item {
                        OutlinedTextField(
                            value = surahSearchQuery,
                            onValueChange = { surahSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search Surah by name or number…") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary)
                            },
                            trailingIcon = {
                                if (surahSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { surahSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            shape = AlDeenTokens.ShapePill,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MintSurface,
                                unfocusedContainerColor = MintSurface,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MintBorder
                            )
                        )
                    }

                    items(filteredSurahs, key = { it.number }) { surah ->
                        val startPage = Quran16Mappings.surah(surah.number, 0) ?: 2
                        Card(
                            onClick = {
                                open(startPage)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MintSurface),
                            border = BorderStroke(1.dp, MintBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${surah.number}",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${surah.number}. ${surah.englishName}",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = AlDeenTokens.ShapePill,
                                            color = MintSurfaceVariant
                                        ) {
                                            Text(
                                                text = surah.revelationType,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "Page $startPage",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                }

                                Text(
                                    text = surah.urduName,
                                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                                    fontSize = 18.sp,
                                    color = EmeraldPrimary,
                                    textAlign = TextAlign.End
                                )

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = TextTertiary
                                )
                            }
                        }
                    }
                }

                "recent" -> {
                    item {
                        Card(
                            onClick = { open(store.lastPage) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Resume Last Reading",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Page ${store.lastPage} • Para ${Quran16Mappings.juzForPage(store.lastPage)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = OnEmeraldContainer)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Resume",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    if (recent.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No recent reading locations yet",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Previously Opened Pages:",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }

                        items(recent, key = { "rec_$it" }) { rPage ->
                            Card(
                                onClick = { open(rPage) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AlDeenTokens.ShapeCard,
                                colors = CardDefaults.cardColors(containerColor = MintSurface),
                                border = BorderStroke(1.dp, MintBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FindInPage,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "PDF Page $rPage",
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Para ${Quran16Mappings.juzForPage(rPage)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open",
                                        tint = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedNumber != 0) {
        PositionDialog(
            juz = selectedJuz,
            number = selectedNumber,
            dismiss = { selectedNumber = 0 }
        ) { targetPage ->
            selectedNumber = 0
            open(targetPage)
        }
    }

    if (goTo) {
        GoToPageDialog(
            count = if (status.pages > 0) status.pages else 560,
            dismiss = { goTo = false }
        ) { targetPage ->
            goTo = false
            open(targetPage)
        }
    }

    if (clear) {
        ConfirmDialog(
            title = "Clear reading history?",
            text = "This will remove recently read page locations only. Bookmarks will be kept.",
            action = "Clear All",
            dismiss = { clear = false }
        ) {
            store.clearRecent()
            clear = false
        }
    }

    message?.let { value ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("16 Line Quran") },
            text = { Text(value) },
            confirmButton = {
                TextButton(onClick = { message = null }) {
                    Text("OK", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MintSurface
        )
    }
}

@Composable
private fun ModernHubCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 135.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MintSurface),
        border = BorderStroke(1.dp, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = AlDeenTokens.ShapePill,
                    color = MintSurfaceVariant
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
internal fun PositionDialog(
    juz: Boolean,
    number: Int,
    dismiss: () -> Unit,
    open: (Int) -> Unit
) {
    val title = if (juz) "Para $number" else AuthoritativeContentProvider.allSurahs[number - 1].englishName
    val urduTitle = if (juz) AuthoritativeContentProvider.allJuz[number - 1].urduName else AuthoritativeContentProvider.allSurahs[number - 1].urduName

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = urduTitle,
                    fontFamily = AlDeenFontRegistry.UrduFontFamily,
                    color = EmeraldPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!juz) {
                    Text(
                        text = "Surah starting page in the 16-line Mushaf:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                } else {
                    Text(
                        text = "Select position inside Para $number:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Quran16Mappings.labels.forEachIndexed { index, label ->
                    val page = if (juz) Quran16Mappings.juz(number, index) else Quran16Mappings.surah(number, index)
                    val isAvailable = page != null
                    OutlinedButton(
                        onClick = { page?.let(open) },
                        enabled = isAvailable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = AlDeenTokens.ShapePill,
                        border = BorderStroke(
                            1.dp,
                            if (isAvailable) EmeraldPrimary.copy(alpha = 0.5f) else MintBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isAvailable) EmeraldContainer.copy(alpha = 0.3f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isAvailable) TextPrimary else TextTertiary,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isAvailable) {
                                Surface(
                                    shape = AlDeenTokens.ShapePill,
                                    color = EmeraldPrimary
                                ) {
                                    Text(
                                        text = "Page $page",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) {
                Text("Close", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MintSurface
    )
}

@Composable
internal fun GoToPageDialog(
    count: Int,
    dismiss: () -> Unit,
    open: (Int) -> Unit
) {
    var value by rememberSaveable { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val page = value.toIntOrNull()
    val valid = page != null && Quran16Mappings.valid(page, count)

    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                text = "Go to PDF Page",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { char -> char.isDigit() }.take(5) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("PDF Page Number (1–$count)") },
                    isError = attempted && !valid,
                    supportingText = {
                        Text(
                            text = if (attempted && !valid) "Enter a valid page between 1 and $count." else "Directly jump to any 16-line page.",
                            color = if (attempted && !valid) MaterialTheme.colorScheme.error else TextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MintBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attempted = true
                    if (valid) open(page!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = AlDeenTokens.ShapePill
            ) {
                Text("Jump to Page", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MintSurface
    )
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    action: String,
    dismiss: () -> Unit,
    confirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        },
        confirmButton = {
            Button(
                onClick = confirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = AlDeenTokens.ShapePill
            ) {
                Text(action, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MintSurface
    )
}
