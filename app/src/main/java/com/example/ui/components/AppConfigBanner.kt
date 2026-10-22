package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.repository.BannerRepository
import com.example.data.repository.BannerSession
import com.example.data.repository.BannerState
import com.example.ui.theme.AlDeenTokens
import com.example.ui.theme.MintBorder
import kotlinx.coroutines.*

/** Local OS connectivity observation; this does not open a Firebase connection. */
@Composable
fun rememberBannerOnline(): Boolean {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) {
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    }
    fun connected(): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    var online by remember { mutableStateOf(connected()) }
    DisposableEffect(manager) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var disposed = false
        fun update() { handler.post { if (!disposed) online = connected() } }
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) = update()
            override fun onLost(network: android.net.Network) = update()
            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) = update()
        }
        manager.registerDefaultNetworkCallback(callback)
        update()
        onDispose {
            disposed = true
            manager.unregisterNetworkCallback(callback)
            handler.removeCallbacksAndMessages(null)
        }
    }
    return online
}

data class StartupBanners(
    val home: BannerState = BannerState(),
    val more: BannerState = BannerState(),
    val popup: BannerState = BannerState(),
    val side: BannerState = BannerState()
)

private const val HOME_SLOT = "homepage_banner"
private const val MORE_SLOT = "goal_banner"
private const val POPUP_SLOT = "popup_banner"
private const val SIDE_SLOT = "side_loader_banner"

/**
 * Process-wide: one remote check per app entry, however often the UI is recreated
 * (rotation, dialogs, permission prompts, opening an ad link and coming back, ...).
 *
 * A new entry = a cold start, or coming back after the app spent at least
 * [NEW_ENTRY_AFTER_BACKGROUND_MS] in the background. Set it to 0L to check on every return.
 */
private object AdSyncSession {
    const val NEW_ENTRY_AFTER_BACKGROUND_MS = 30L * 60_000L
    /** True once every slot was checked successfully in this entry. Nothing more is requested. */
    @Volatile var done = false
    @Volatile private var stoppedAt = 0L

    fun onStop() { stoppedAt = SystemClock.elapsedRealtime() }
    fun onStart() {
        val at = stoppedAt
        stoppedAt = 0L
        if (at != 0L && SystemClock.elapsedRealtime() - at >= NEW_ENTRY_AFTER_BACKGROUND_MS) {
            done = false
            PopupSession.handled = false
        }
    }
}

/**
 * 1. Instantly shows the cached ads (image + details) from disk, even offline.
 * 2. ONE remote check per app entry. Per slot: outer switch -> is_ad -> remaining values; a "false"
 *    stops that slot right there. All slots share one connection, which is closed afterwards.
 * 3. After that (or when the app is in the background / offline) there is no communication at all.
 *    A check that failed (offline, server error) is retried only when the network comes back or the
 *    user returns to the app, never on a timer.
 */
@Composable
fun rememberStartupBanners(online: Boolean): StartupBanners {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(StartupBanners()) }
    var cacheLoaded by remember { mutableStateOf(false) }

    fun repositories(session: BannerSession = BannerSession()) = listOf(
        BannerRepository(context, HOME_SLOT, "is_banner", session),
        BannerRepository(context, MORE_SLOT, "is_banner", session),
        BannerRepository(context, POPUP_SLOT, "is_popup", session),
        BannerRepository(context, SIDE_SLOT, "is_side_loader", session)
    )
    fun StartupBanners.at(index: Int) = when (index) { 0 -> home; 1 -> more; 2 -> popup; else -> side }
    fun StartupBanners.with(index: Int, value: BannerState) = when (index) {
        0 -> copy(home = value); 1 -> copy(more = value); 2 -> copy(popup = value); else -> copy(side = value)
    }

    // Step 1: cache first, no network involved.
    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) {
            val r = repositories()
            StartupBanners(r[0].cached(), r[1].cached(), r[2].cached(), r[3].cached())
        }
        if (!cacheLoaded) {
            // Nothing left to wait for when this entry was already checked, or we are offline.
            val settled = AdSyncSession.done || !online
            state = cached.copy(popup = cached.popup.copy(startupCheckComplete = settled))
            cacheLoaded = true
        }
    }

    DisposableEffect(online, owner, cacheLoaded) {
        var job: Job? = null
        var session: BannerSession? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        fun stop() {
            job?.cancel()
            job = null
            session?.close()
            session = null
        }
        fun start() {
            if (!cacheLoaded || job?.isActive == true || AdSyncSession.done) return
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            if (!online) {
                state = state.copy(popup = state.popup.copy(startupCheckComplete = true))
                return
            }
            state = state.copy(popup = state.popup.copy(startupCheckComplete = false))
            val visit = BannerSession()
            session = visit
            val repos = repositories(visit)
            job = scope.launch {
                var failed = false
                // A failed slot keeps whatever is cached; the others are unaffected.
                suspend fun sync(index: Int) {
                    val current = state.at(index)
                    val result = try {
                        withContext(Dispatchers.IO) { repos[index].check() }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { failed = true; null }
                    state = state.with(index, (result ?: current).copy(startupCheckComplete = true))
                }
                try {
                    // Popup first: it is what the user waits for, and it opens the connection
                    // the other slots then reuse (one TLS handshake for everything).
                    sync(2)
                    coroutineScope { listOf(0, 1, 3).forEach { launch { sync(it) } } }
                    if (!failed) AdSyncSession.done = true
                } finally {
                    visit.close()
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> AdSyncSession.onStart()
                Lifecycle.Event.ON_RESUME -> start()
                // ON_STOP (not ON_PAUSE): a permission dialog only pauses and must not restart anything.
                Lifecycle.Event.ON_STOP -> { stop(); AdSyncSession.onStop() }
                Lifecycle.Event.ON_DESTROY -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            stop()
            scope.cancel()
        }
    }
    return state
}

@Composable
fun AppConfigBanner(state: BannerState, modifier: Modifier = Modifier) {
    // Custom banners are never rendered without validated internet connectivity.
    if (!rememberBannerOnline() || !state.visible) return
    var now by remember(state) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val showAd = state.inSchedule(now)
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clickable = showAd && (state.clickUrl.startsWith("https://") || state.clickUrl.startsWith("http://"))

    val hasContent = showAd && (state.image != null || state.title.isNotBlank() || state.description.isNotBlank())
    if (!hasContent) {
        // Thin horizontal empty banner box with centered Advertisement
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = AlDeenTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AlDeenTokens.SpacingMedium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val imgAspect = remember(state.image, state.aspect) {
            val file = state.image
            if (file != null && file.exists()) {
                try {
                    val opt = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(file.path, opt)
                    if (opt.outWidth > 0 && opt.outHeight > 0) {
                        opt.outWidth.toFloat() / opt.outHeight
                    } else state.aspect.coerceIn(0.2f, 5f)
                } catch (_: Exception) { state.aspect.coerceIn(0.2f, 5f) }
            } else state.aspect.coerceIn(0.2f, 5f)
        }
        val hasText = state.title.isNotBlank() || state.description.isNotBlank()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = clickable) {
                    runCatching { uriHandler.openUri(state.clickUrl) }
                }
        ) {
            // Top centered Advertisement header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Divider immediately above image
            HorizontalDivider(
                color = MintBorder,
                thickness = AlDeenTokens.CardBorderWidth
            )

            // Image directly starts below the line, fitting exact card width with natural aspect ratio
            state.image?.let { file ->
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(file)
                        .memoryCacheKey("${file.path}:${file.lastModified()}:${file.length()}")
                        .build(),
                    contentDescription = state.title.ifBlank { "Advertisement" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(imgAspect),
                    contentScale = ContentScale.FillWidth
                )
            }

            // Bottom divider & text only if title or description exists
            if (hasText) {
                HorizontalDivider(
                    color = MintBorder,
                    thickness = AlDeenTokens.CardBorderWidth
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AlDeenTokens.SpacingMedium, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.title.isNotBlank()) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.description.isNotBlank()) {
                        if (state.title.isNotBlank()) Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
