package com.example.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthoritativeContentProvider
import com.example.domain.model.AsmaAlHusna
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

data class ProphetName(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val urduMeaning: String,
    val englishMeaning: String
)

val prophetNamesList: List<ProphetName> get() = com.example.data.repository.ProphetNamesData.list

enum class HolyNamesTab {
    ALLAH_NAMES,
    PROPHET_NAMES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllahNamesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(HolyNamesTab.ALLAH_NAMES) }
    var searchQuery by remember { mutableStateOf("") }
    val allAllahNames = AuthoritativeContentProvider.asmaAlHusna
    val allProphetNames = prophetNamesList

    var selectedAllahName by remember { mutableStateOf<AsmaAlHusna?>(null) }
    var selectedProphetName by remember { mutableStateOf<ProphetName?>(null) }

    val filteredAllahNames = remember(searchQuery) {
        if (searchQuery.isBlank()) allAllahNames
        else allAllahNames.filter {
            it.transliteration.contains(searchQuery, ignoreCase = true) ||
            it.englishMeaning.contains(searchQuery, ignoreCase = true) ||
            it.urduMeaning.contains(searchQuery) ||
            it.arabicName.contains(searchQuery)
        }
    }

    val filteredProphetNames = remember(searchQuery) {
        if (searchQuery.isBlank()) allProphetNames
        else allProphetNames.filter {
            it.transliteration.contains(searchQuery, ignoreCase = true) ||
            it.englishMeaning.contains(searchQuery, ignoreCase = true) ||
            it.urduMeaning.contains(searchQuery) ||
            it.arabicName.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "Holy Names (اسماء مقدسہ)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = if (currentTab == HolyNamesTab.ALLAH_NAMES) "99 Names of Allah (أَسْمَاءُ اللَّٰهِ الْحُسْنَىٰ)"
                                   else "99 Names of Hazrat Muhammad ﷺ (أَسْمَاءُ النَّبِيِّ ﷺ)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("allah_names_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("allah_names_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector: Allah SWT Names vs Hazrat Muhammad ﷺ Names
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AlDeenTokens.ShapePill)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                // Tab 1: Allah Names
                val isAllah = currentTab == HolyNamesTab.ALLAH_NAMES
                Surface(
                    onClick = { currentTab = HolyNamesTab.ALLAH_NAMES },
                    shape = AlDeenTokens.ShapePill,
                    color = if (isAllah) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Names of Allah (99)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isAllah) Color.White else TextPrimary
                            )
                        )
                    }
                }

                // Tab 2: Prophet Muhammad ﷺ Names
                val isProphet = currentTab == HolyNamesTab.PROPHET_NAMES
                Surface(
                    onClick = { currentTab = HolyNamesTab.PROPHET_NAMES },
                    shape = AlDeenTokens.ShapePill,
                    color = if (isProphet) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AlDeenText(
                            text = "Hazrat Muhammad ﷺ (99)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isProphet) Color.White else TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(if (currentTab == HolyNamesTab.ALLAH_NAMES) "Search Allah's name or meaning..." else "Search Prophet's name or meaning...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                singleLine = true,
                shape = AlDeenTokens.ShapePill,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = MintBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("allah_names_search")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content Grid
            if (currentTab == HolyNamesTab.ALLAH_NAMES) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAllahNames) { name ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAllahName = name }
                                 .testTag("allah_name_${name.number}"),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MintBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Number Badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${name.number}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Arabic Name
                                AlDeenText(
                                    text = name.arabicName,
                                    style = AlDeenTypography.ArabicHeading.copy(
                                        color = EmeraldPrimary,
                                        fontSize = 20.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Transliteration
                                Text(
                                    text = name.transliteration,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                // Urdu Meaning
                                AlDeenText(
                                    text = name.urduMeaning,
                                    style = AlDeenTypography.UrduBody.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                // English Meaning
                                Text(
                                    text = name.englishMeaning,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProphetNames) { name ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProphetName = name }
                                .testTag("prophet_name_${name.number}"),
                            shape = AlDeenTokens.ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MintBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Number Badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${name.number}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Arabic Name
                                AlDeenText(
                                    text = name.arabicName,
                                    style = AlDeenTypography.ArabicHeading.copy(
                                        color = EmeraldPrimary,
                                        fontSize = 20.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Transliteration
                                Text(
                                    text = name.transliteration,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                // Urdu Meaning
                                AlDeenText(
                                    text = name.urduMeaning,
                                    style = AlDeenTypography.UrduBody.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                // English Meaning
                                Text(
                                    text = name.englishMeaning,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog for Allah's Name
    selectedAllahName?.let { name ->
        AlertDialog(
            onDismissRequest = { selectedAllahName = null },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlDeenText(
                        text = name.arabicName,
                        style = AlDeenTypography.ArabicHeading.copy(fontSize = 32.sp, color = EmeraldPrimary),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${name.number}. ${name.transliteration}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlDeenText(
                        text = name.urduMeaning,
                        style = AlDeenTypography.UrduHeading.copy(fontSize = 18.sp, color = EmeraldPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name.englishMeaning,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Whoever memorizes and contemplates the 99 Names of Allah will enter Paradise. (Sahih al-Bukhari)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, lineHeight = 16.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedAllahName = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Detail Dialog for Prophet's Name
    selectedProphetName?.let { name ->
        AlertDialog(
            onDismissRequest = { selectedProphetName = null },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlDeenText(
                        text = name.arabicName,
                        style = AlDeenTypography.ArabicHeading.copy(fontSize = 32.sp, color = EmeraldPrimary),
                        textAlign = TextAlign.Center
                    )
                    AlDeenText(
                        text = "${name.number}. ${name.transliteration} ﷺ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlDeenText(
                        text = name.urduMeaning,
                        style = AlDeenTypography.UrduHeading.copy(fontSize = 18.sp, color = EmeraldPrimary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = name.englishMeaning,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        AlDeenText(
                            text = "Sending blessings upon the Prophet ﷺ brings tenfold blessings and elevates ranks. (Sahih Muslim)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, lineHeight = 16.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedProphetName = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
