package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AlDeenDrawerContent(
    onAboutUsClick: () -> Unit,
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
            // Top branding banner
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

            // Navigation items:
            // 1. About Us
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary) },
                label = {
                    Text(
                        "About Us",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onAboutUsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .testTag("drawer_item_about_us")
            )

            // 2. Contact Us
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                label = {
                    Text(
                        "Contact Us",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onContactUsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .testTag("drawer_item_contact_us")
            )

            // 3. Settings
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary) },
                label = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onSettingsClick()
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .testTag("drawer_item_settings")
            )

            // 4. Share Us
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary) },
                label = {
                    Text(
                        "Share Us",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    shareApp(context)
                },
                modifier = Modifier
                    .padding(horizontal = AlDeenTokens.SpacingMedium)
                    .testTag("drawer_item_share_us")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
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
