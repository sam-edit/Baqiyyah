package com.example.ui.quran16

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthoritativeContentProvider
import com.example.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Reading display filter modes for comfortable recitation in any lighting condition. */
enum class ReadingFilter {
    ORIGINAL,
    SEPIA,
    NIGHT
}

/** Scroll direction modes: continuous vertical scrolling or horizontal page-by-page flipping. */
enum class ReaderScrollMode {
    VERTICAL,
    HORIZONTAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Quran16Reader(store: Quran16Store, initialPage: Int, onBack: () -> Unit) {
    var session by remember { mutableStateOf<PdfSession?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    // Core controls visibility & reading preferences
    var controls by rememberSaveable { mutableStateOf(true) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    var fitPage by rememberSaveable { mutableStateOf(false) }
    var thumbnails by rememberSaveable { mutableStateOf(false) }
    var goTo by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf("") }
    var positionNumber by rememberSaveable { mutableIntStateOf(0) }
    var positionJuz by rememberSaveable { mutableStateOf(true) }

    var filterMode by rememberSaveable { mutableStateOf(ReadingFilter.ORIGINAL) }
    var scrollMode by rememberSaveable { mutableStateOf(ReaderScrollMode.VERTICAL) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    val bookmarks by store.bookmarks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Page synchronization across vertical & horizontal modes
    var currentPage by rememberSaveable { mutableIntStateOf(initialPage.coerceAtLeast(1)) }
    var sliderValue by remember { mutableStateOf<Float?>(null) }

    val activity = LocalContext.current.activity()

    // Immersive Fullscreen handling
    DisposableEffect(activity, fullScreen) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val wasVisible = window?.decorView?.let {
            androidx.core.view.ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.systemBars())
        } ?: true
        val oldBehavior = controller?.systemBarsBehavior

        if (fullScreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullScreen && wasVisible) controller?.show(WindowInsetsCompat.Type.systemBars())
            oldBehavior?.let { controller?.systemBarsBehavior = it }
        }
    }

    BackHandler {
        if (fullScreen) {
            fullScreen = false
            controls = true
        } else {
            onBack()
        }
    }

    // Open native PDF session safely
    LaunchedEffect(store, retry) {
        error = null
        var opened: PdfSession? = null
        try {
            withContext(NonCancellable) { opened = store.open() }
            val pdf = opened!!
            require(Quran16Mappings.valid(initialPage, pdf.count)) { "Requested page is outside this PDF." }
            session = pdf
            awaitCancellation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Unable to open PDF. Please retry."
        } finally {
            session = null
            opened?.close()
        }
    }

    // Save last read page upon disposal
    DisposableEffect(session) {
        val active = session
        onDispose {
            if (active != null && Quran16Mappings.valid(currentPage, active.count)) {
                store.visit(currentPage)
            }
        }
    }

    val pdf = session
    val totalPages = pdf?.count ?: 560

    // Vertical pager state (one full page at a time, exactly like horizontal pager)
    val verticalPagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceAtLeast(0),
        pageCount = { totalPages }
    )
    // Horizontal pager state
    val horizontalPagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceAtLeast(0),
        pageCount = { totalPages }
    )

    // Sync vertical pager to currentPage
    LaunchedEffect(verticalPagerState, scrollMode) {
        if (scrollMode == ReaderScrollMode.VERTICAL && pdf != null) {
            snapshotFlow { verticalPagerState.currentPage + 1 }
                .distinctUntilChanged()
                .filter { it in 1..pdf.count }
                .collectLatest { page ->
                    currentPage = page
                    store.visit(page)
                }
        }
    }

    // Sync horizontal pager to currentPage
    LaunchedEffect(horizontalPagerState, scrollMode) {
        if (scrollMode == ReaderScrollMode.HORIZONTAL && pdf != null) {
            snapshotFlow { horizontalPagerState.currentPage + 1 }
                .distinctUntilChanged()
                .filter { it in 1..pdf.count }
                .collectLatest { page ->
                    currentPage = page
                    store.visit(page)
                }
        }
    }

    fun jump(target: Int) {
        val activePdf = session ?: return
        if (Quran16Mappings.valid(target, activePdf.count)) {
            currentPage = target
            store.visit(target)
            scope.launch {
                if (scrollMode == ReaderScrollMode.VERTICAL) {
                    verticalPagerState.scrollToPage(target - 1)
                } else {
                    horizontalPagerState.scrollToPage(target - 1)
                }
            }
        } else {
            error = "PDF page $target is outside this document (1–${activePdf.count})."
        }
    }

    val currentPara = remember(currentPage) {
        Quran16Mappings.juzForPage(currentPage)
    }

    val isCurrentBookmarked = currentPage in bookmarks

    val viewerBackground = when (filterMode) {
        ReadingFilter.ORIGINAL -> if (LocalAppIsDark.current) Color(0xFF0F1715) else Color(0xFFF7F9F8)
        ReadingFilter.SEPIA -> Color(0xFFFBF4E6)
        ReadingFilter.NIGHT -> Color(0xFF0A0E0D)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(viewerBackground)
    ) {
        if (pdf == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (error == null) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Opening 16 Line Quran…",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                } else {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { retry++ },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Retry")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBack) {
                    Text("Back to Index", color = TextSecondary)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TOP BAR: DIRECTLY ACCESSIBLE ACTIONS ---
                if (controls) {
                    Surface(
                        tonalElevation = 4.dp,
                        color = if (filterMode == ReadingFilter.NIGHT) Color(0xFF141A18) else MintSurface,
                        border = BorderStroke(0.5.dp, MintBorder)
                    ) {
                        TopAppBar(
                            title = {
                                Surface(
                                    onClick = { goTo = true },
                                    shape = AlDeenTokens.ShapePill,
                                    color = EmeraldContainer,
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Para $currentPara",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                        )
                                        Text(
                                            text = "Page $currentPage / ${pdf.count}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Jump",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = EmeraldPrimary
                                    )
                                }
                            },
                            actions = {
                                // 1. Bookmark toggle (Direct visible icon)
                                IconButton(onClick = { store.bookmark(currentPage) }) {
                                    Icon(
                                        imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (isCurrentBookmarked) "Remove bookmark" else "Bookmark page",
                                        tint = if (isCurrentBookmarked) GoldAccent else TextSecondary
                                    )
                                }

                                // 2. Reading Filter (Day / Sepia / Night)
                                IconButton(onClick = {
                                    filterMode = when (filterMode) {
                                        ReadingFilter.ORIGINAL -> ReadingFilter.SEPIA
                                        ReadingFilter.SEPIA -> ReadingFilter.NIGHT
                                        ReadingFilter.NIGHT -> ReadingFilter.ORIGINAL
                                    }
                                }) {
                                    Icon(
                                        imageVector = when (filterMode) {
                                            ReadingFilter.ORIGINAL -> Icons.Default.LightMode
                                            ReadingFilter.SEPIA -> Icons.Default.FilterVintage
                                            ReadingFilter.NIGHT -> Icons.Default.DarkMode
                                        },
                                        contentDescription = "Reading Tone: ${filterMode.name}",
                                        tint = if (filterMode != ReadingFilter.ORIGINAL) EmeraldPrimary else TextSecondary
                                    )
                                }

                                // 3. Scroll Direction: Horizontal Flip vs Vertical Continuous Scroll
                                IconButton(onClick = {
                                    val newMode = if (scrollMode == ReaderScrollMode.VERTICAL) {
                                        ReaderScrollMode.HORIZONTAL
                                    } else {
                                        ReaderScrollMode.VERTICAL
                                    }
                                    scrollMode = newMode
                                    jump(currentPage)
                                }) {
                                    Icon(
                                        imageVector = if (scrollMode == ReaderScrollMode.HORIZONTAL) {
                                            Icons.Default.SwapHoriz
                                        } else {
                                            Icons.Default.SwapVert
                                        },
                                        contentDescription = "Switch Scroll: ${if (scrollMode == ReaderScrollMode.HORIZONTAL) "Horizontal Flip" else "Vertical Scroll"}",
                                        tint = EmeraldPrimary
                                    )
                                }

                                // 4. Page Fit Mode: Fit to Screen vs Fit to Width
                                IconButton(onClick = { fitPage = !fitPage }) {
                                    Icon(
                                        imageVector = if (fitPage) Icons.Default.AspectRatio else Icons.Default.FitScreen,
                                        contentDescription = if (fitPage) "Fit to Width" else "Fit to Screen",
                                        tint = if (fitPage) EmeraldPrimary else TextSecondary
                                    )
                                }

                                // 5. TOC / Index Drawer
                                IconButton(onClick = { panel = "Juz Index" }) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "Index & Bookmarks",
                                        tint = EmeraldPrimary
                                    )
                                }

                                // 6. Thumbnails Strip
                                IconButton(onClick = { thumbnails = !thumbnails }) {
                                    Icon(
                                        imageVector = Icons.Default.ViewCarousel,
                                        contentDescription = "Thumbnail Strip",
                                        tint = if (thumbnails) EmeraldPrimary else TextSecondary
                                    )
                                }

                                // 7. Fullscreen Toggle
                                IconButton(onClick = {
                                    fullScreen = !fullScreen
                                    controls = !fullScreen
                                }) {
                                    Icon(
                                        imageVector = if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                // --- MAIN READING VIEWPORT (HORIZONTAL OR VERTICAL) ---
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val viewportHeight = maxHeight

                    if (scrollMode == ReaderScrollMode.VERTICAL) {
                        VerticalPager(
                            state = verticalPagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { "v_$it" }
                        ) { pageIndex ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfPageImage(
                                    session = pdf,
                                    index = pageIndex,
                                    modifier = Modifier.fillMaxSize(),
                                    fitHeight = viewportHeight,
                                    readingFilter = filterMode,
                                    userScale = zoomLevel,
                                    toggleControls = { controls = !controls }
                                )
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = horizontalPagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { "h_$it" }
                        ) { pageIndex ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PdfPageImage(
                                    session = pdf,
                                    index = pageIndex,
                                    modifier = Modifier.fillMaxSize(),
                                    fitHeight = viewportHeight,
                                    readingFilter = filterMode,
                                    userScale = zoomLevel,
                                    toggleControls = { controls = !controls }
                                )
                            }
                        }
                    }
                }

                // --- BOTTOM THUMBNAILS PREVIEW STRIP ---
                if (controls && thumbnails) {
                    val thumbState = rememberLazyListState((currentPage - 1).coerceAtLeast(0))
                    Surface(
                        tonalElevation = 6.dp,
                        color = if (filterMode == ReadingFilter.NIGHT) Color(0xFF141A18) else MintSurface,
                        border = BorderStroke(0.5.dp, MintBorder)
                    ) {
                        LazyRow(
                            state = thumbState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            items(pdf.count, key = { "thumb_$it" }) { index ->
                                val isSelected = index + 1 == currentPage
                                Card(
                                    onClick = { jump(index + 1) },
                                    modifier = Modifier
                                        .width(72.dp)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else MintBorder
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) EmeraldContainer else MintSurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        PdfPageImage(
                                            session = pdf,
                                            index = index,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            thumbnail = true,
                                            readingFilter = filterMode
                                        )
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) EmeraldPrimary else TextSecondary
                                            ),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- BOTTOM FLOATING CONTROLS & FAST PAGE SCRUBBER ---
                if (controls) {
                    Surface(
                        tonalElevation = 4.dp,
                        color = if (filterMode == ReadingFilter.NIGHT) Color(0xFF141A18) else MintSurface,
                        border = BorderStroke(0.5.dp, MintBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Page
                                IconButton(
                                    onClick = { jump(currentPage - 1) },
                                    enabled = currentPage > 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Page",
                                        tint = if (currentPage > 1) EmeraldPrimary else TextTertiary
                                    )
                                }

                                // Zoom Out
                                IconButton(onClick = {
                                    zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(1f)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "Zoom Out",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Interactive Slider Scrubber
                                Slider(
                                    value = sliderValue ?: currentPage.toFloat(),
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = {
                                        sliderValue?.let { jump(it.toInt()) }
                                        sliderValue = null
                                    },
                                    valueRange = 1f..maxOf(2, pdf.count).toFloat(),
                                    enabled = pdf.count > 1,
                                    colors = SliderDefaults.colors(
                                        thumbColor = EmeraldPrimary,
                                        activeTrackColor = EmeraldPrimary,
                                        inactiveTrackColor = EmeraldContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                // Zoom In
                                IconButton(onClick = {
                                    zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.5f)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Zoom In",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Next Page
                                IconButton(
                                    onClick = { jump(currentPage + 1) },
                                    enabled = currentPage < pdf.count
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Page",
                                        tint = if (currentPage < pdf.count) EmeraldPrimary else TextTertiary
                                    )
                                }

                                // Direct Page Jump Button
                                TextButton(
                                    onClick = { goTo = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "${sliderValue?.toInt() ?: currentPage}",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Restore Floating Action Button when controls are hidden
            if (!controls) {
                SmallFloatingActionButton(
                    onClick = { controls = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(14.dp),
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Show reading controls"
                    )
                }
            }

            // Dialogs & Bottom Sheet Panels
            if (goTo) {
                GoToPageDialog(
                    count = pdf.count,
                    dismiss = { goTo = false }
                ) { target ->
                    goTo = false
                    jump(target)
                }
            }

            // Quick Index Drawer: Juz, Surah, Bookmarks
            if (panel.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { panel = "" },
                    containerColor = MintSurface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    var selectedTab by remember { mutableIntStateOf(if (panel == "Bookmarks") 2 else 0) }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MintSurface,
                        contentColor = EmeraldPrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Juz (Paras)", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Surahs", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Bookmarks (${bookmarks.size})", fontWeight = FontWeight.Bold) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (selectedTab) {
                            0 -> items(AuthoritativeContentProvider.allJuz) { juz ->
                                val sPage = Quran16Mappings.juz(juz.juzNumber, 0) ?: 2
                                Card(
                                    onClick = {
                                        positionJuz = true
                                        positionNumber = juz.juzNumber
                                        panel = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = AlDeenTokens.ShapeCard,
                                    colors = CardDefaults.cardColors(containerColor = MintSurfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${juz.juzNumber}. Para (Page $sPage)",
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = juz.urduName,
                                            fontFamily = AlDeenFontRegistry.UrduFontFamily,
                                            color = EmeraldPrimary,
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                            }
                            1 -> items(AuthoritativeContentProvider.allSurahs) { surah ->
                                val sPage = Quran16Mappings.surah(surah.number, 0) ?: 2
                                Card(
                                    onClick = {
                                        panel = ""
                                        jump(sPage)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = AlDeenTokens.ShapeCard,
                                    colors = CardDefaults.cardColors(containerColor = MintSurfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "${surah.number}. ${surah.englishName}",
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Page $sPage",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                            )
                                        }
                                        Text(
                                            text = surah.urduName,
                                            fontFamily = AlDeenFontRegistry.UrduFontFamily,
                                            color = EmeraldPrimary,
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                            }
                            2 -> {
                                if (bookmarks.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No bookmarks saved yet. Tap the bookmark icon in the top toolbar to save a page.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                items(bookmarks) { saved ->
                                    Card(
                                        onClick = {
                                            panel = ""
                                            jump(saved)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = AlDeenTokens.ShapeCard,
                                        colors = CardDefaults.cardColors(containerColor = MintSurfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "PDF Page $saved • Para ${Quran16Mappings.juzForPage(saved)}",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = GoldAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (positionNumber > 0) {
                PositionDialog(
                    juz = positionJuz,
                    number = positionNumber,
                    dismiss = { positionNumber = 0 }
                ) { p ->
                    positionNumber = 0
                    jump(p)
                }
            }

            error?.let { message ->
                AlertDialog(
                    onDismissRequest = { error = null },
                    title = { Text("Page Unavailable") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { error = null }) {
                            Text("OK", color = EmeraldPrimary)
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MintSurface
                )
            }
        }
    }
}

/**
 * High-performance PDF Page renderer with pinch zoom, double-tap zoom,
 * and reading filters (Day / Sepia / Night).
 */
@Composable
private fun PdfPageImage(
    session: PdfSession,
    index: Int,
    modifier: Modifier,
    fitHeight: androidx.compose.ui.unit.Dp? = null,
    thumbnail: Boolean = false,
    readingFilter: ReadingFilter = ReadingFilter.ORIGINAL,
    userScale: Float = 1f,
    toggleControls: () -> Unit = {}
) {
    var bitmap by remember(session, index) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    var gestureScale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    val totalScale = (gestureScale * userScale).coerceIn(1f, 4.5f)

    LaunchedEffect(fitHeight) {
        gestureScale = 1f
        pan = Offset.Zero
    }

    // ColorFilter matrix calculation
    val colorFilter: ColorFilter? = remember(readingFilter) {
        when (readingFilter) {
            ReadingFilter.ORIGINAL -> null
            ReadingFilter.NIGHT -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ReadingFilter.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.393f * 1.15f, 0.769f, 0.189f, 0f, 25f,
                        0.349f, 0.686f * 1.1f, 0.168f, 0f, 18f,
                        0.272f, 0.534f, 0.131f * 0.9f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val width = with(density) { maxWidth.toPx().toInt() }

        LaunchedEffect(width) {
            gestureScale = 1f
            pan = Offset.Zero
        }

        val renderWidth = if (thumbnail) 180 else (width * totalScale).toInt().coerceIn(200, 2400)

        LaunchedEffect(session, index, renderWidth, retry) {
            if (bitmap != null) delay(160)
            error = null
            try {
                bitmap = session.render(index, renderWidth)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Page could not be rendered."
            }
        }

        val image = bitmap
        val ratio = image?.let { it.width.toFloat() / it.height } ?: 0.65f
        val pageSize = if (fitHeight != null) {
            Modifier
                .fillMaxWidth()
                .height(fitHeight)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        }

        val pageBg = when (readingFilter) {
            ReadingFilter.ORIGINAL -> Color.White
            ReadingFilter.SEPIA -> Color(0xFFFBF4E6)
            ReadingFilter.NIGHT -> Color(0xFF0F0F0F)
        }

        Box(
            modifier = pageSize
                .background(pageBg)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                val gestures = if (thumbnail) {
                    Modifier
                } else {
                    Modifier
                        .pointerInput(index) {
                            detectTapGestures(
                                onTap = { toggleControls() },
                                onDoubleTap = {
                                    gestureScale = if (gestureScale > 1f) 1f else 2.4f
                                    pan = Offset.Zero
                                }
                            )
                        }
                        .pointerInput(index) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } > 1 || gestureScale > 1.01f) {
                                        gestureScale = (gestureScale * event.calculateZoom()).coerceIn(1f, 4f)
                                        val delta = event.calculatePan()
                                        val maxX = size.width * (totalScale - 1) / 2f
                                        val maxY = size.height * (totalScale - 1) / 2f
                                        pan = Offset(
                                            x = (pan.x + delta.x).coerceIn(-maxX, maxX),
                                            y = (pan.y + delta.y).coerceIn(-maxY, maxY)
                                        )
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                }

                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "PDF page ${index + 1}",
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(gestures)
                        .graphicsLayer {
                            scaleX = totalScale
                            scaleY = totalScale
                            translationX = pan.x
                            translationY = pan.y
                        },
                    contentScale = ContentScale.Fit
                )
            } else if (error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = EmeraldPrimary
                )
            }

            if (error != null) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) {
                        Text("Retry page", color = EmeraldPrimary)
                    }
                }
            }
        }
    }
}
