package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Premium Circular Reveal Navigation Drawer Overlay.
 * Expands smoothly from the hamburger menu button's location in a circle,
 * reveals items with a staggered slide + fade + scale effect, and dims the background.
 * Closing the drawer smoothly reverses the circular reveal animation.
 */
@Composable
fun CircularRevealDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onAboutUsClick: () -> Unit,
    onDonateUsClick: () -> Unit = {},
    onContactUsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    sideBanner: com.example.data.repository.BannerState = com.example.data.repository.BannerState(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val animProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isOpen) 320 else 260,
            easing = if (isOpen) AlDeenMotion.EmphasizedDecelerateEasing else AlDeenMotion.EmphasizedAccelerateEasing
        ),
        label = "circular_reveal_drawer_progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Main App Screen Content
        content()

        // Overlay is composed whenever open or animating
        if (animProgress > 0f) {
            // 1. Subtle background dimming scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = animProgress }
                    .background(Color.Black.copy(alpha = 0.52f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                    }
                    .testTag("circular_drawer_scrim")
            )

            // 2. Circular Reveal Drawer Sheet
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 320.dp)
                    .graphicsLayer {
                        clip = true
                        shape = CircularRevealShape(progress = animProgress)
                        alpha = animProgress.coerceIn(0f, 1f)
                        translationX = -(1f - animProgress) * 40f
                    }
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = AlDeenTokens.ShapeSheet
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Consume clicks inside the drawer so scrim isn't triggered
                    }
                    .testTag("circular_drawer_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(vertical = AlDeenTokens.SpacingMedium)
                ) {
                    // Top branding banner with Close morph icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AlDeenTokens.SpacingLarge,
                                vertical = AlDeenTokens.SpacingMedium
                            )
                            .background(DashboardGradient, shape = AlDeenTokens.ShapeCard)
                            .padding(AlDeenTokens.SpacingLarge)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Baqiyyah",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Comprehensive Islamic Companion",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // Smooth Close (X) button with press scale
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(32.dp)
                                    .pressScaleEffect(0.9f)
                                    .testTag("drawer_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Menu",
                                    tint = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))
                    HorizontalDivider(color = MintBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(AlDeenTokens.SpacingMedium))

                    // Navigation items with staggered fade + slide/scale entrance:
                    val items = listOf(
                        Triple("About Us", Icons.Default.Info) {
                            onClose()
                            onAboutUsClick()
                        },
                        Triple("Donate Us", Icons.Default.Favorite) {
                            onClose()
                            onDonateUsClick()
                        },
                        Triple("Contact Us", Icons.Default.Email) {
                            onClose()
                            onContactUsClick()
                        },
                        Triple("Settings", Icons.Default.Settings) {
                            onClose()
                            onSettingsClick()
                        },
                        Triple("Share Us", Icons.Default.Share) {
                            onClose()
                            shareApp(context)
                        }
                    )

                    // Items + side loader ad share a scrollable area, so a tall ad never pushes
                    // the footer off small screens.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        items.forEachIndexed { index, (label, icon, onClick) ->
                            val itemFraction = ((animProgress - (index * 0.08f)) / 0.68f).coerceIn(0f, 1f)
                            val itemEased = FastOutSlowInEasing.transform(itemFraction)

                            val tag = when (label) {
                                "About Us" -> "drawer_item_about_us"
                                "Donate Us" -> "drawer_item_donate_us"
                                "Contact Us" -> "drawer_item_contact_us"
                                "Settings" -> "drawer_item_settings"
                                else -> "drawer_item_share_us"
                            }

                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = EmeraldPrimary
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                },
                                selected = false,
                                onClick = onClick,
                                modifier = Modifier
                                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                                    .graphicsLayer {
                                        alpha = itemEased
                                        translationX = (1f - itemEased) * -28f
                                    }
                                    .pressScaleEffect(0.96f)
                                    .testTag(tag)
                            )
                        }

                        // Side loader ad: very end, below "Share Us".
                        val adEased = ((animProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                        SideLoaderAd(
                            state = sideBanner,
                            modifier = Modifier
                                .padding(
                                    horizontal = AlDeenTokens.SpacingLarge,
                                    vertical = AlDeenTokens.SpacingMedium
                                )
                                .graphicsLayer { alpha = adEased }
                        )
                    }

                    // Footer with gentle delayed entrance derived from animProgress
                    val footerEased = ((animProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AlDeenTokens.SpacingLarge)
                            .graphicsLayer { alpha = footerEased },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard drawer content retained for backward compatibility.
 */
@Composable
fun AlDeenDrawerContent(
    onAboutUsClick: () -> Unit,
    onDonateUsClick: () -> Unit = {},
    onContactUsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = modifier.widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = AlDeenTokens.ShapeSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AlDeenTokens.SpacingXXLarge)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AlDeenTokens.SpacingLarge, vertical = AlDeenTokens.SpacingMedium)
                    .background(DashboardGradient, shape = AlDeenTokens.ShapeCard)
                    .padding(AlDeenTokens.SpacingLarge)
            ) {
                Column {
                    Text(
                        text = "Baqiyyah",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Comprehensive Islamic Companion",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))
            HorizontalDivider(color = MintBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingMedium))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary) },
                label = { Text("About Us", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onAboutUsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .pressScaleEffect(0.96f)
                    .testTag("drawer_item_about_us")
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = EmeraldPrimary) },
                label = { Text("Donate Us", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onDonateUsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .pressScaleEffect(0.96f)
                    .testTag("drawer_item_donate_us")
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                label = { Text("Contact Us", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onContactUsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .pressScaleEffect(0.96f)
                    .testTag("drawer_item_contact_us")
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary) },
                label = { Text("Settings", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onSettingsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .pressScaleEffect(0.96f)
                    .testTag("drawer_item_settings")
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary) },
                label = { Text("Share Us", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    shareApp(context)
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .pressScaleEffect(0.96f)
                    .testTag("drawer_item_share_us")
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AlDeenTokens.SpacingLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Version 1.0.0 • Made with Barakah",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
            }
        }
    }
}

private fun shareApp(context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "Discover Baqiyyah — accurate prayer times, Holy Quran, Hadith collections, and daily worship goals: https://play.google.com/store/apps/details?id=com.ruleno1.baqiyyahproject"
        )
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Baqiyyah")
    context.startActivity(shareIntent)
}
