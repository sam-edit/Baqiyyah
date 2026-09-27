package com.example.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

enum class MoreItemType(
    val title: String,
    val urduTitle: String,
    val icon: ImageVector,
    val route: String
) {
    QIBLA("Qibla Compass", "قبلہ نما", Icons.Default.Explore, "more_qibla"),
    TASBEEH("Tasbeeh Counter", "تسبیح کاؤنٹر", Icons.Default.TouchApp, "more_tasbeeh"),
    CALENDAR("Islamic Calendar", "اسلامی کیلنڈر", Icons.Default.CalendarMonth, "more_calendar"),
    ALLAH_NAMES("Holy Names", "اسماء مقدسہ", Icons.Default.Stars, "more_allah_names"),
    DUAS("Duas & Azkar", "ادعیہ و اذکار", Icons.Default.VolunteerActivism, "more_duas"),
    BOOKMARKS("Bookmarks", "محفوظ شدہ", Icons.Default.Bookmark, "more_bookmarks"),
    FIND_MASJID("Find Masjid", "مسجد تلاش کریں", Icons.Default.Mosque, "more_find_masjid")
}

@Composable
fun MoreScreen(
    prayerState: com.example.domain.model.PrayerState,
    location: com.example.domain.model.UserLocation,
    formattedCurrentTime: String,
    formattedUpcomingTime: String,
    onOpenDrawer: () -> Unit,
    onLocationClick: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("more_screen_root")
    ) {
        // Consistent Header
        com.example.ui.components.AlDeenHeader(
            location = location,
            onMenuClick = onOpenDrawer,
            onLocationClick = onLocationClick
        )

        // Compact Prayer Dashboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlDeenTokens.SpacingLarge)
        ) {
            com.example.ui.components.CompactPrayerDashboardCard(
                prayerState = prayerState,
                formattedCurrentTime = formattedCurrentTime,
                formattedUpcomingTime = formattedUpcomingTime
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Islamic Utilities",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 22.sp
                    )
                )

                AlDeenText(
                    text = "الْمَزِيد",
                    style = AlDeenTypography.ArabicHeading.copy(
                        color = EmeraldPrimary,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Practical Islamic tools and daily spiritual resources",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        // Grid of More Utilities
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingMedium),
            contentPadding = PaddingValues(top = AlDeenTokens.SpacingMedium, bottom = AlDeenTokens.SpacingXXLarge)
        ) {
            items(MoreItemType.values()) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (item.route == "more_find_masjid") {
                                try {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/Masjid+Near+Me"))
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    onNavigateToItem(item.route)
                                }
                            } else {
                                onNavigateToItem(item.route)
                            }
                        }
                        .testTag("more_tile_${item.name.lowercase()}"),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AlDeenTokens.SpacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        AlDeenText(
                            text = item.urduTitle,
                            style = AlDeenTypography.UrduBody.copy(
                                color = EmeraldPrimary,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
