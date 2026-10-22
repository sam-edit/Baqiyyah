package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.repository.BannerState
import com.example.ui.theme.AlDeenTokens
import com.example.ui.theme.MintBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Something worth showing: an image or at least a title / description. */
private val BannerState.hasContent: Boolean
    get() = image != null || title.isNotBlank() || description.isNotBlank()

private val BannerState.opensLink: Boolean
    get() = clickUrl.startsWith("https://") || clickUrl.startsWith("http://")

/** Re-evaluates start_at / expires_at while the ad is on screen. */
@Composable
private fun rememberAdLive(state: BannerState): Boolean {
    var now by remember(state) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return state.visible && state.inSchedule(now) && state.hasContent
}



// ------------------------------------------------------------------ side loader (drawer)

/** Ad shown at the very end of the side drawer, below "Share Us". Nothing is drawn without a live ad. */
@Composable
fun SideLoaderAd(state: BannerState, modifier: Modifier = Modifier) {
    // No custom banner (including an empty placeholder) when the device is offline.
    if (!rememberBannerOnline() || !state.visible) return
    val adLive = rememberAdLive(state)
    val uriHandler = LocalUriHandler.current

    if (!adLive) {
        // Thin horizontal empty banner box with centered Advertisement
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("drawer_side_loader_ad"),
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

    Card(
        modifier = modifier.fillMaxWidth().testTag("drawer_side_loader_ad"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.opensLink) { runCatching { uriHandler.openUri(state.clickUrl) } }
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

            // Image flush with card width and natural aspect
            state.image?.let { file ->
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
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

// ------------------------------------------------------------------ popup

/** Once per app entry: the decision is made once, so rotation / dialogs never re-trigger the popup. */
internal object PopupSession {
    @Volatile var handled = false
}

private const val POPUP_PREFS = "popup_banner_display"
private const val KEY_LAST_SHOWN = "last_shown_at"

private fun popupIntervalElapsed(context: Context, repeatMinutes: Long): Boolean {
    if (repeatMinutes <= 0L) return true
    val last = context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SHOWN, 0L)
    val now = System.currentTimeMillis()
    return last == 0L || now < last || now - last >= repeatMinutes.coerceAtMost(5_256_000L) * 60_000L
}

private fun markPopupShown(context: Context) {
    context.getSharedPreferences(POPUP_PREFS, Context.MODE_PRIVATE)
        .edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
}

/**
 * Waits until this foreground launch's config check finishes. If values are unchanged the existing
 * verified cache is used; if they changed, AppConfigBanner updates the cache/state before this host
 * is allowed to display. is_popup is represented by state.visible. is_on_app_open=true ignores the
 * minute interval; false obeys repeat_after_minute.
 */
@Composable
fun PopupBannerHost(state: BannerState) {
    val context = LocalContext.current.applicationContext
    val online = rememberBannerOnline()
    val owner = LocalLifecycleOwner.current
    var show by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> show = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val eligible = online && state.visible && state.startupCheckComplete && (state.isOnlyOneTime || state.showOn == "app_open")
    LaunchedEffect(eligible, state.isOnAppOpen, state.isOnlyOneTime, state.repeatMinutes, state.adEnabled,
        state.start, state.end, state.image, state.title, state.description) {
        if (!eligible || PopupSession.handled) {
            if (!eligible) show = false
            return@LaunchedEffect
        }
        // Mark handled before suspension so recomposition cannot open a duplicate dialog.
        PopupSession.handled = true
        val due = if (state.isOnlyOneTime || state.isOnAppOpen) true else {
            withContext(Dispatchers.IO) { popupIntervalElapsed(context, state.repeatMinutes) }
        }
        val adCanShow = !state.adEnabled || state.inSchedule()
        if (due && adCanShow) {
            if (!state.isOnlyOneTime) markPopupShown(context)
            show = true
        } else {
            show = false
        }
    }

    val adCanShow = !state.adEnabled || state.inSchedule()
    if (show && eligible && adCanShow) {
        PopupBannerDialog(state = state, onDismiss = { show = false })
    }
}

@Composable
private fun PopupBannerDialog(state: BannerState, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardWidth = if (maxWidth > 400.dp) 400.dp else maxWidth
            // Room left for the creative once the title / description are accounted for.
            val hasText = state.title.isNotBlank() || state.description.isNotBlank()
            val imageMaxHeight = (maxHeight - (if (hasText) 150.dp else 24.dp)).coerceAtLeast(160.dp)
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

            Box(Modifier.width(cardWidth).testTag("popup_banner")) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = state.opensLink) {
                                runCatching { uriHandler.openUri(state.clickUrl) }
                                onDismiss()
                            }
                    ) {
                        // Top centered Advertisement header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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

                        // Image starts directly below line, flush with card borders and with natural aspect ratio
                        if (state.image != null) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(state.image)
                                    .memoryCacheKey("${state.image.path}:${state.image.lastModified()}:${state.image.length()}")
                                    .build(),
                                contentDescription = state.title.ifBlank { "Advertisement" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(imgAspect)
                                    .heightIn(max = imageMaxHeight),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            // If no image, keep 1:1 box representation for popup as requested
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Advertisement",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                // Clearly visible close button: dark disc + white ring + white X, 48dp touch target.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .testTag("popup_banner_close"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close advertisement",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
