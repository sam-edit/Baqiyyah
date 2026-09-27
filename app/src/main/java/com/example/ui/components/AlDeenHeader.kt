package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserLocation
import com.example.ui.theme.AlDeenTokens
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.TextPrimary

@Composable
fun AlDeenHeader(
    location: UserLocation,
    onMenuClick: () -> Unit,
    onLocationClick: () -> Unit,
    title: String = "Baqiyyah",
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AlDeenTokens.SpacingLarge, vertical = AlDeenTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Menu Icon + App Name or Custom Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_header_left")
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(AlDeenTokens.TouchTargetMin)
                    .testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(AlDeenTokens.IconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(AlDeenTokens.SpacingSmall))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = if (title.length > 10) 18.sp else 22.sp
                    )
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldPrimary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Right: Location selector pill
        Surface(
            modifier = Modifier
                .clip(AlDeenTokens.ShapePill)
                .clickable { onLocationClick() }
                .testTag("location_selector_pill"),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = AlDeenTokens.ShapePill,
            shadowElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Select Location",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location.cityName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
