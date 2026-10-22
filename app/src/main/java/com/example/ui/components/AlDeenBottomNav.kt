package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Default.Home, "tab_home"),
    QURAN("Quran", Icons.AutoMirrored.Filled.MenuBook, "tab_quran"),
    HADEETH("Hadeeth", Icons.AutoMirrored.Filled.LibraryBooks, "tab_hadeeth"),
    GOALS("Goals", Icons.Default.CheckCircle, "tab_goals"),
    MORE("More", Icons.Default.MoreHoriz, "tab_more")
}

@Composable
fun AlDeenBottomNav(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavTab.values().forEach { tab ->
            val isSelected = currentTab == tab

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 400f
                ),
                label = "nav_icon_scale_${tab.name}"
            )

            val iconOffsetY by animateDpAsState(
                targetValue = if (isSelected) (-2).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 400f
                ),
                label = "nav_icon_offset_${tab.name}"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier
                            .offset(y = iconOffsetY)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldContainer,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier
                    .testTag("bottom_nav_${tab.name.lowercase()}")
            )
        }
    }
}

