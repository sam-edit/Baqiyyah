package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AlDeenText(
                        text = "About Us",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("about_us_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("about_us_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AlDeenTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingLarge)
        ) {
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DashboardGradient)
                        .padding(AlDeenTokens.SpacingXXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Baqiyyah",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }

            // Mission Statement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AlDeenTokens.SpacingLarge)) {
                    Text(
                        text = "Our Vision",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Baqiyyah was built to provide Muslims around the world with an uncompromising, authentic, and peaceful digital Islamic companion. Designed with modern Material 3 craft and reverent Islamic aesthetics, Baqiyyah empowers your daily Fard prayers, Quran recitation, Hadith study, and spiritual goals with absolute clarity and privacy.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // Authoritative References & Methodology (APIs & Data Sources)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AlDeenTokens.SpacingLarge)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Api,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Authoritative References & Methodology",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API 1: AlAdhan API
                    ApiDetailItem(
                        title = "1. AlAdhan API (https://api.aladhan.com/)",
                        purpose = "Used for precise solar zenith prayer times calculation (Fajr, Dhuhr, Asr, Maghrib, Isha), Islamic Hijri calendar integration, and jurisprudential methods (Hanafi & Shafi'i calculations, Karachi, Umm Al-Qura, MWL, and ISNA conventions)."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API 2: AlQuran Cloud API
                    ApiDetailItem(
                        title = "2. AlQuran Cloud API (Islamic Network)",
                        purpose = "Used for multi-language Quranic translations database, supporting 30+ international languages and authorized editions with on-demand synchronization."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API 3: Quran.com API (v4)
                    ApiDetailItem(
                        title = "3. Quran.com API (v4)",
                        purpose = "Used for word-by-word Quranic vocabulary analysis, authentic Madani/Uthmani script typography, and Arabic root word definitions."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API 4: Islamic Network Audio CDN
                    ApiDetailItem(
                        title = "4. Islamic Network Quran Audio CDN",
                        purpose = "Used for streaming and offline downloading high-quality Quran recitations by 10 world-renowned Qaris (including Mishary Rashid Alafasy, Abdul Basit, Saud Al-Shuraim, and Abdul Rahman Al-Sudais)."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API 5: Hadith Open API (jsDelivr CDN)
                    ApiDetailItem(
                        title = "5. Hadith Open API (jsDelivr CDN)",
                        purpose = "Used for accessing verified canonical Hadith collections (Sahih al-Bukhari, Sahih Muslim, Sunan Abi Dawud, Jami' at-Tirmidhi, Sunan an-Nasa'i, Sunan Ibn Majah, and Muwatta Malik) with English & Urdu translations."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Offline Local Core
                    ApiDetailItem(
                        title = "6. Built-in Offline Core Data",
                        purpose = "Complete 114 Surahs (6,236 Ayahs) with authentic Arabic text and Fateh Muhammad Jalandhry Urdu translation, Masnoon Duas (Hisn al-Muslim), 99 Names of Allah, and Digital Tasbeeh embedded locally for 100% offline availability."
                    )
                }
            }

            // Privacy & User Respect
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(AlDeenTokens.SpacingLarge)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Privacy & User Respect",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Baqiyyah does not sell your data, display commercial banner advertisements, or harvest location telemetry. Your spiritual worship goals and bookmarks are stored securely and privately in your device's local database.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Meet our Team Section (Now placed above the standalone logo)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("meet_our_team_section"),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AlDeenTokens.SpacingLarge)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Meet our Team",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Two photos in a single row taking half-half space
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Team Member 1: Muhammad Waseem
                        TeamMemberCard(
                            imageRes = R.drawable.team_wasi,
                            name = "Muhammad Waseem",
                            role = "Co-Founder",
                            email = "wa***rt@gmail.com",
                            modifier = Modifier.weight(1f).testTag("team_member_waseem")
                        )

                        // Team Member 2: Muhammad Samran
                        TeamMemberCard(
                            imageRes = R.drawable.team_sam,
                            name = "Muhammad Samran",
                            role = "Co-Founder",
                            email = "sa***00@gmail.com",
                            modifier = Modifier.weight(1f).testTag("team_member_samran")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Standalone Studio Logo closer to the team section with thin grey lines and 'ruleno1studio' label
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("standalone_studio_logo_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left thin grey line
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0xFFD0D5DD)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Studio Logo Card (Standalone, 1:1 aspect ratio, 100dp)
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .aspectRatio(1f)
                            .testTag("studio_logo_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, MintBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.studio_logo),
                                contentDescription = "Studio Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Right thin grey line
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0xFFD0D5DD)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Studio Name Label below logo
                Text(
                    text = "ruleno1studio",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))
        }
    }
}

@Composable
private fun ApiDetailItem(
    title: String,
    purpose: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MintSurfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MintBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = purpose,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun TeamMemberCard(
    imageRes: Int,
    name: String,
    role: String,
    email: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MintSurfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MintBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Photo Frame matching App Theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldContainer)
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            // Block 1: Name
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnEmeraldContainer,
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            // Block 2: Role
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldLight,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AmberHighlight,
                        fontSize = 11.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 5.dp)
                )
            }

            // Block 3: Email
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

