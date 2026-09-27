package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SettingsRepository
import com.example.domain.model.CalculationMethod
import com.example.domain.model.PrayerType
import com.example.notification.PrayerNotificationManager
import com.example.ui.components.AlDeenText
import com.example.ui.components.CityPickerDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val location by settingsRepository.userLocation.collectAsStateWithLifecycle()
    val primaryLocation by settingsRepository.primaryLocation.collectAsStateWithLifecycle()
    val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()
    val is24Hour by settingsRepository.is24HourFormat.collectAsStateWithLifecycle()
    val hijriOffset by settingsRepository.hijriAdjustment.collectAsStateWithLifecycle()
    val haptics by settingsRepository.hapticsEnabled.collectAsStateWithLifecycle()
    val calcMethod by settingsRepository.calculationMethod.collectAsStateWithLifecycle()
    val juristicSchool by settingsRepository.juristicSchool.collectAsStateWithLifecycle()
    val prePrayerMin by settingsRepository.prePrayerReminderMin.collectAsStateWithLifecycle()
    val isQuietMode by settingsRepository.isQuietMode.collectAsStateWithLifecycle()
    val notifMap by settingsRepository.prayerNotifications.collectAsStateWithLifecycle()

    var showCityPicker by remember { mutableStateOf(false) }
    var showCalcMethodDialog by remember { mutableStateOf(false) }
    var showGpsPromptDialog by remember { mutableStateOf(false) }
    var showMorePrayers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AlDeenText(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(vertical = AlDeenTokens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingLarge)
        ) {
            // Section 1: Location & Coordinates
            item {
                SectionHeader("Location & Calculation")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCityPicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Selected City",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${location.cityName}, ${location.countryName}" + if (primaryLocation?.cityName?.equals(location.cityName, ignoreCase = true) == true) " ★ (Primary)" else "",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = EmeraldPrimary)
                                )
                                if (primaryLocation != null && !primaryLocation!!.cityName.equals(location.cityName, ignoreCase = true)) {
                                    Text(
                                        text = "Primary for Notifications & Startup: ${primaryLocation!!.cityName}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = GoldAccent, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (!settingsRepository.isDeviceLocationEnabled(context)) {
                                    showGpsPromptDialog = true
                                } else {
                                    settingsRepository.detectAndUseCurrentLocation(context)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("settings_use_current_location_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldContainer,
                                contentColor = EmeraldPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            shape = AlDeenTokens.ShapePill
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AlDeenText(
                                text = "Use Current Location (موجودہ لوکیشن حاصل کریں)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Calculation Method",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Calculation Method Selector Button / List Opener
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCalcMethodDialog = true }
                                .testTag("calculation_method_selector")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = calcMethod.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    AlDeenText(
                                        text = "Tap to change method (طریقہ کار تبدیل کریں)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select method",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        AlDeenText(
                            text = "Asr Juristic Method (طریقۂ حسابِ عصر)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Determines Asr time calculation standard for any country",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.domain.model.JuristicSchool.values().forEach { school ->
                                val isSelected = juristicSchool == school
                                Surface(
                                    onClick = { settingsRepository.setJuristicSchool(school) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) EmeraldPrimary else MintBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("school_btn_${school.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = school.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                                fontSize = 13.sp
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        AlDeenText(
                                            text = school.urduName,
                                            style = AlDeenTypography.UrduBody.copy(
                                                color = if (isSelected) EmeraldPrimary else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Display & Appearance
            item {
                SectionHeader("Display & Time Format")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        // 24-Hour Format Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "24-Hour Time Format",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (is24Hour) "Using 24-hour clock (e.g. 13:30)" else "Using 12-hour clock (e.g. 01:30 PM)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                            Switch(
                                checked = is24Hour,
                                onCheckedChange = { settingsRepository.set24HourFormat(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary, checkedTrackColor = EmeraldContainer)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MintBorder.copy(alpha = 0.5f))

                        // Theme Mode
                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeMode.values().forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { settingsRepository.setThemeMode(mode) },
                                    label = { Text(mode.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MintBorder.copy(alpha = 0.5f))

                        // Hijri Calendar Offset
                        Text(
                            text = "Hijri Calendar Adjustment",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Adjust Islamic calendar day according to local moon sighting",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-2, -1, 0, 1, 2).forEach { offset ->
                                val isSelected = hijriOffset == offset
                                val label = if (offset > 0) "+$offset" else "$offset"
                                Surface(
                                    onClick = { settingsRepository.setHijriAdjustment(offset) },
                                    shape = AlDeenTokens.ShapePill,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Prayer Notifications & Adhan
            item {
                SectionHeader("Prayer Notifications & Adhan")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        // Quiet Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Quiet Mode",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Mute audio alarms during prayer times",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                            Switch(
                                checked = isQuietMode,
                                onCheckedChange = { settingsRepository.toggleQuietMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary, checkedTrackColor = EmeraldContainer)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MintBorder.copy(alpha = 0.5f))

                        // Per-Prayer Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlDeenText(
                                text = "Farz Prayer Notifications (فرض نمازیں)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isQuietMode) TextTertiary else TextPrimary
                                )
                            )
                            if (isQuietMode) {
                                Surface(
                                    shape = AlDeenTokens.ShapePill,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Muted in Quiet Mode",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Farz Prayers List (Fajr, Dhuhr, Asr, Maghrib, Isha)
                        val farzPrayers = listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)
                        farzPrayers.forEach { prayer ->
                            val isEnabled = notifMap[prayer] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .alpha(if (isQuietMode) 0.4f else 1.0f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlDeenText(
                                    text = "${prayer.displayName} (${prayer.urduName})",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isQuietMode) TextTertiary else TextPrimary
                                    )
                                )
                                Switch(
                                    checked = isEnabled && !isQuietMode,
                                    enabled = !isQuietMode,
                                    onCheckedChange = { settingsRepository.setPrayerNotification(prayer, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = EmeraldPrimary,
                                        checkedTrackColor = EmeraldContainer,
                                        disabledCheckedThumbColor = EmeraldPrimary.copy(alpha = 0.35f),
                                        disabledCheckedTrackColor = EmeraldContainer.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Expandable Button for Additional Prayers (Sunrise, Tahajjud, etc.)
                        val additionalPrayers = listOf(
                            PrayerType.SUNRISE,
                            PrayerType.TAHAJJUD,
                            PrayerType.ISHRAQ,
                            PrayerType.CHASHT,
                            PrayerType.ZAWAL,
                            PrayerType.JUMMAH
                        )

                        TextButton(
                            onClick = { showMorePrayers = !showMorePrayers },
                            enabled = !isQuietMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isQuietMode) 0.4f else 1.0f)
                        ) {
                            AlDeenText(
                                text = if (showMorePrayers) "Hide Additional Times (طلوع، تہجد وغیرہ چھپائیں) ▲"
                                else "More Times: Sunrise, Tahajjud, Ishraq (دیگر اوقات) ▼",
                                color = if (isQuietMode) TextTertiary else EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }

                        if (showMorePrayers) {
                            additionalPrayers.forEach { prayer ->
                                val isEnabled = notifMap[prayer] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .alpha(if (isQuietMode) 0.4f else 1.0f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlDeenText(
                                        text = "${prayer.displayName} (${prayer.urduName})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isQuietMode) TextTertiary else TextPrimary
                                        )
                                    )
                                    Switch(
                                        checked = isEnabled && !isQuietMode,
                                        enabled = !isQuietMode,
                                        onCheckedChange = { settingsRepository.setPrayerNotification(prayer, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = EmeraldPrimary,
                                            checkedTrackColor = EmeraldContainer,
                                            disabledCheckedThumbColor = EmeraldPrimary.copy(alpha = 0.35f),
                                            disabledCheckedTrackColor = EmeraldContainer.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MintBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Notification Button
                        OutlinedButton(
                            onClick = {
                                PrayerNotificationManager.showTestNotification(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("settings_test_notification_button"),
                            border = BorderStroke(1.2.dp, EmeraldPrimary),
                            shape = AlDeenTokens.ShapePill
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AlDeenText(
                                text = "Send Test Notification (ٹیسٹ نوٹیفکیشن)",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Section 4: Haptics & Feedback
            item {
                SectionHeader("Preferences")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Haptic Vibration Feedback",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Subtle vibration for tasbeeh counter and button taps",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                            Switch(
                                checked = haptics,
                                onCheckedChange = { settingsRepository.setHapticsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary, checkedTrackColor = EmeraldContainer)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            currentLocation = location,
            primaryLocation = primaryLocation,
            onLocationSelected = { settingsRepository.updateLocation(it) },
            onSetAsPrimaryLocation = { settingsRepository.setPrimaryLocation(it) },
            onClearPrimaryLocation = { settingsRepository.clearPrimaryLocation() },
            onUseCurrentLocation = {
                if (!settingsRepository.isDeviceLocationEnabled(context)) {
                    showGpsPromptDialog = true
                } else {
                    settingsRepository.detectAndUseCurrentLocation(context)
                }
            },
            onDismissRequest = { showCityPicker = false }
        )
    }

    // Calculation Method List Dialog
    if (showCalcMethodDialog) {
        AlertDialog(
            onDismissRequest = { showCalcMethodDialog = false },
            title = {
                AlDeenText(
                    text = "Calculation Method (طریقۂ حساب)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 17.sp
                    )
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(CalculationMethod.values()) { method ->
                        val isSelected = calcMethod == method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setCalculationMethod(method)
                                    showCalcMethodDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    settingsRepository.setCalculationMethod(method)
                                    showCalcMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else TextPrimary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalcMethodDialog = false }) {
                    Text("Close", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = AlDeenTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // GPS Disabled Prompt Dialog
    if (showGpsPromptDialog) {
        AlertDialog(
            onDismissRequest = { showGpsPromptDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Location Service Turned Off",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                AlDeenText(
                    text = "Device location is disabled. Please turn on Location (GPS) in your device settings to detect your current location.\n\nبراہ کرم موجودہ لوکیشن حاصل کرنے کے لیے موبائل کی لوکیشن (GPS) آن کریں۔",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsPromptDialog = false
                        settingsRepository.openLocationSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Turn On Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsPromptDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = AlDeenTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary,
            fontSize = 14.sp
        ),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
