package com.example.ui.more

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DhikrPhraseItem(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val urduMeaning: String,
    val defaultTarget: Int = 33,
    val isCustom: Boolean = false
)

data class DhikrHistoryEntry(
    val phraseId: String,
    val phraseName: String,
    val count: Int,
    val timestamp: Long,
    val formattedDate: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasbeehScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        } catch (_: Exception) {
            null
        }
    }
    val prefs = remember { context.getSharedPreferences("tasbeeh_data", Context.MODE_PRIVATE) }

    // Initial default phrases
    val defaultPhrases = remember {
        listOf(
            DhikrPhraseItem("subhanallah", "سُبْحَانَ اللَّٰهِ", "SubhanAllah", "اللہ پاک ہے", 33),
            DhikrPhraseItem("alhamdulillah", "الْحَمْدُ لِلَّٰهِ", "Alhamdulillah", "تمام تعریفیں اللہ کے لیے ہیں", 33),
            DhikrPhraseItem("allahuakbar", "اللَّٰهُ أَكْبَرُ", "Allahu Akbar", "اللہ سب سے بڑا ہے", 34),
            DhikrPhraseItem("astaghfirullah", "أَسْتَغْفِرُ اللَّٰهَ", "Astaghfirullah", "میں اللہ سے بخشش مانگتا ہوں", 100),
            DhikrPhraseItem("lailahaillallah", "لَا إِلَٰهَ إِلَّا اللَّٰهُ", "La ilaha illallah", "اللہ کے سوا کوئی معبود نہیں", 100),
            DhikrPhraseItem("durood", "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Durood Shareef", "اے اللہ! محمد ﷺ پر رحمت بھیج", 100),
            DhikrPhraseItem("hawqala", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّٰهِ", "La Hawla Wa La Quwwata", "اللہ کے سوا کوئی طاقت و قوت نہیں", 33),
            DhikrPhraseItem("subhanallahi_bihamdihi", "سُبْحَانَ اللَّٰهِ وَبِحَمْدِهِ", "SubhanAllahi Wa Bihamdihi", "اللہ پاک ہے اپنی حمد کے ساتھ", 100)
        )
    }

    // Load custom phrases from prefs
    var customPhrases by remember {
        mutableStateOf(run {
            val json = prefs.getString("custom_phrases", "[]") ?: "[]"
            try {
                val array = JSONArray(json)
                val list = mutableListOf<DhikrPhraseItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        DhikrPhraseItem(
                            id = obj.getString("id"),
                            arabic = obj.getString("arabic"),
                            transliteration = obj.getString("transliteration"),
                            urduMeaning = obj.getString("urduMeaning"),
                            defaultTarget = obj.optInt("defaultTarget", 33),
                            isCustom = true
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList<DhikrPhraseItem>()
            }
        })
    }

    val allPhrases = remember(customPhrases) { defaultPhrases + customPhrases }
    var selectedPhrase by remember { mutableStateOf(allPhrases.first()) }

    // Counts & Targets
    var count by remember { mutableIntStateOf(prefs.getInt("current_count_${selectedPhrase.id}", 0)) }
    var targetLimit by remember { mutableIntStateOf(prefs.getInt("target_limit_${selectedPhrase.id}", selectedPhrase.defaultTarget)) }
    var isInfiniteMode by remember { mutableStateOf(prefs.getBoolean("infinite_mode_${selectedPhrase.id}", false)) }
    var completedLaps by remember { mutableIntStateOf(count / (if (targetLimit > 0) targetLimit else 33)) }

    // Sound and vibration toggles
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean("vibrate_enabled", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", false)) }

    // Dialogs & Sheets
    var showCustomTasbeehDialog by remember { mutableStateOf(false) }
    var showCustomCountDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // History of counts per word
    fun saveCountToHistory(phrase: DhikrPhraseItem, sessionCount: Int) {
        if (sessionCount <= 0) return
        val currentHistory = prefs.getString("history_records", "[]") ?: "[]"
        try {
            val array = JSONArray(currentHistory)
            val obj = JSONObject()
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now))
            obj.put("phraseId", phrase.id)
            obj.put("phraseName", phrase.transliteration)
            obj.put("count", sessionCount)
            obj.put("timestamp", now)
            obj.put("formattedDate", dateStr)
            array.put(obj)
            prefs.edit().putString("history_records", array.toString()).apply()

            // Update cumulative count for this phrase
            val totalKey = "total_count_${phrase.id}"
            val existingTotal = prefs.getInt(totalKey, 0)
            prefs.edit().putInt(totalKey, existingTotal + sessionCount).apply()
        } catch (_: Exception) {}
    }

    fun getCumulativeCount(phraseId: String): Int {
        return prefs.getInt("total_count_$phraseId", 0)
    }

    fun triggerHaptic(strong: Boolean = false) {
        if (!vibrateEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (strong) 80L else 30L
                val amp = if (strong) VibrationEffect.DEFAULT_AMPLITUDE else 120
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (strong) 80L else 30L)
            }
        } catch (_: Exception) {}
    }

    fun playBeadSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Exception) {}
    }

    fun onIncrement() {
        count++
        completedLaps = if (targetLimit > 0) count / targetLimit else 0
        prefs.edit().putInt("current_count_${selectedPhrase.id}", count).apply()

        // Check if target reached
        if (!isInfiniteMode && targetLimit > 0 && count % targetLimit == 0) {
            triggerHaptic(strong = true)
            saveCountToHistory(selectedPhrase, targetLimit)
        } else {
            triggerHaptic(strong = false)
        }
        playBeadSound()
    }

    fun onDecrement() {
        if (count > 0) {
            count--
            completedLaps = if (targetLimit > 0) count / targetLimit else 0
            prefs.edit().putInt("current_count_${selectedPhrase.id}", count).apply()
            triggerHaptic(strong = false)
        }
    }

    fun onReset() {
        if (count > 0) {
            saveCountToHistory(selectedPhrase, count)
        }
        count = 0
        completedLaps = 0
        prefs.edit().putInt("current_count_${selectedPhrase.id}", 0).apply()
        triggerHaptic(strong = true)
    }

    // When changing phrase, persist and reload current count
    fun switchPhrase(newPhrase: DhikrPhraseItem) {
        if (count > 0) {
            saveCountToHistory(selectedPhrase, count)
        }
        selectedPhrase = newPhrase
        count = prefs.getInt("current_count_${newPhrase.id}", 0)
        targetLimit = prefs.getInt("target_limit_${newPhrase.id}", newPhrase.defaultTarget)
        isInfiniteMode = prefs.getBoolean("infinite_mode_${newPhrase.id}", false)
        completedLaps = if (targetLimit > 0) count / targetLimit else 0
    }

    val progress = remember(count, targetLimit, isInfiniteMode) {
        if (isInfiniteMode || targetLimit <= 0) 1f
        else ((count % targetLimit).toFloat() / targetLimit.toFloat()).coerceIn(0f, 1f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "Digital Tasbeeh (تسبیح کاؤنٹر)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = "Lifetime for this word: ${getCumulativeCount(selectedPhrase.id) + count}",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tasbeeh_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                actions = {
                    // History Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("tasbeeh_history_btn")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Dhikr History", tint = EmeraldPrimary)
                    }

                    // Vibrate Toggle
                    IconButton(
                        onClick = {
                            vibrateEnabled = !vibrateEnabled
                            prefs.edit().putBoolean("vibrate_enabled", vibrateEnabled).apply()
                        },
                        modifier = Modifier.testTag("tasbeeh_vibrate_btn")
                    ) {
                        Icon(
                            imageVector = if (vibrateEnabled) Icons.Default.Vibration else Icons.Default.Smartphone,
                            contentDescription = "Toggle Vibration",
                            tint = if (vibrateEnabled) EmeraldPrimary else TextSecondary
                        )
                    }

                    // Sound Toggle
                    IconButton(
                        onClick = {
                            soundEnabled = !soundEnabled
                            prefs.edit().putBoolean("sound_enabled", soundEnabled).apply()
                        },
                        modifier = Modifier.testTag("tasbeeh_sound_btn")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = if (soundEnabled) EmeraldPrimary else TextSecondary
                        )
                    }

                    // Reset Counter
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("tasbeeh_reset_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Counter", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("tasbeeh_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Horizontal Dhikr Phrase Selector + Add Custom Tasbeeh
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlDeenText(
                        text = "Select Dhikr (ذکر منتخب کریں)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary)
                    )
                    TextButton(
                        onClick = { showCustomTasbeehDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Custom Tasbeeh", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allPhrases) { phrase ->
                        val isSelected = phrase.id == selectedPhrase.id
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MintBorder),
                            modifier = Modifier
                                .clickable { switchPhrase(phrase) }
                                .testTag("tasbeeh_chip_${phrase.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phrase.transliteration,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                )
                                if (phrase.isCustom) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "★",
                                        color = if (isSelected) GoldAccent else EmeraldPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Dhikr Phrase Card Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AlDeenTokens.ShapeCard,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MintBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlDeenText(
                        text = selectedPhrase.arabic,
                        style = AlDeenTypography.ArabicHeading.copy(
                            color = EmeraldPrimary,
                            fontSize = 28.sp,
                            lineHeight = 36.sp
                        ),
                        targetScript = ScriptLanguage.ARABIC,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedPhrase.transliteration,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    AlDeenText(
                        text = selectedPhrase.urduMeaning,
                        style = AlDeenTypography.UrduBody.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        targetScript = ScriptLanguage.URDU,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Target Limit Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AlDeenTokens.ShapePill)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val targets = listOf(33, 99, 100, 1000)
                targets.forEach { target ->
                    val isSelected = !isInfiniteMode && targetLimit == target
                    Surface(
                        onClick = {
                            isInfiniteMode = false
                            targetLimit = target
                            prefs.edit()
                                .putInt("target_limit_${selectedPhrase.id}", target)
                                .putBoolean("infinite_mode_${selectedPhrase.id}", false)
                                .apply()
                        },
                        shape = CircleShape,
                        color = if (isSelected) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "$target",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Custom target count button
                val isCustomSelected = !isInfiniteMode && !targets.contains(targetLimit)
                Surface(
                    onClick = { showCustomCountDialog = true },
                    shape = CircleShape,
                    color = if (isCustomSelected) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (isCustomSelected) "$targetLimit" else "Custom",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomSelected) Color.White else TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Free (Infinite) Mode
                Surface(
                    onClick = {
                        isInfiniteMode = true
                        prefs.edit().putBoolean("infinite_mode_${selectedPhrase.id}", true).apply()
                    },
                    shape = CircleShape,
                    color = if (isInfiniteMode) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "Free ∞",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isInfiniteMode) Color.White else TextSecondary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 4. Giant Interactive Circular Counter Tap Button
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val animatedScale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1.0f,
                animationSpec = spring(stiffness = 800f),
                label = "tasbeeh_tap_scale"
            )

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, radius = 115.dp, color = EmeraldPrimary)
                    ) { onIncrement() }
                    .testTag("tasbeeh_tap_area"),
                contentAlignment = Alignment.Center
            ) {
                // Circular Progress Ring
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = EmeraldPrimary,
                    trackColor = EmeraldContainer.copy(alpha = 0.4f),
                    strokeWidth = 9.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 62.sp
                        )
                    )

                    Text(
                        text = if (isInfiniteMode) "Target: ∞" else "Target: $targetLimit • Lap: $completedLaps",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "TAP TO COUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // 5. Bottom Controls: Undo (-1) and Reset Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo button
                OutlinedButton(
                    onClick = { onDecrement() },
                    shape = AlDeenTokens.ShapePill,
                    border = BorderStroke(1.dp, MintBorder),
                    enabled = count > 0,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("tasbeeh_undo_btn")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Minus 1", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Minus (-1)", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }

                // Reset button
                Button(
                    onClick = { showResetDialog = true },
                    shape = AlDeenTokens.ShapePill,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("tasbeeh_reset_trigger")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    AlDeenText("Reset (صفر)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Dialog: Add Custom Tasbeeh
    if (showCustomTasbeehDialog) {
        var newArabic by remember { mutableStateOf("") }
        var newTransliteration by remember { mutableStateOf("") }
        var newMeaning by remember { mutableStateOf("") }
        var newTargetText by remember { mutableStateOf("33") }

        AlertDialog(
            onDismissRequest = { showCustomTasbeehDialog = false },
            title = {
                AlDeenText(
                    text = "Add Custom Tasbeeh (اپنی تسبیح شامل کریں)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTransliteration,
                        onValueChange = { newTransliteration = it },
                        label = { Text("Name / Title (e.g., Astaghfar)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newArabic,
                        onValueChange = { newArabic = it },
                        label = { AlDeenText("Arabic Text (عربی متن)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMeaning,
                        onValueChange = { newMeaning = it },
                        label = { Text("Meaning / Translation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTargetText,
                        onValueChange = { newTargetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Count (e.g. 33, 100, 313)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newTransliteration.ifBlank { "Custom Dhikr" }
                        val item = DhikrPhraseItem(
                            id = "custom_${System.currentTimeMillis()}",
                            arabic = newArabic.ifBlank { title },
                            transliteration = title,
                            urduMeaning = newMeaning.ifBlank { title },
                            defaultTarget = newTargetText.toIntOrNull() ?: 33,
                            isCustom = true
                        )
                        val updated = customPhrases + item
                        customPhrases = updated
                        // Persist to SharedPreferences
                        val array = JSONArray()
                        updated.forEach { cp ->
                            val obj = JSONObject()
                            obj.put("id", cp.id)
                            obj.put("arabic", cp.arabic)
                            obj.put("transliteration", cp.transliteration)
                            obj.put("urduMeaning", cp.urduMeaning)
                            obj.put("defaultTarget", cp.defaultTarget)
                            array.put(obj)
                        }
                        prefs.edit().putString("custom_phrases", array.toString()).apply()
                        switchPhrase(item)
                        showCustomTasbeehDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Add Tasbeeh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTasbeehDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Custom Target Count
    if (showCustomCountDialog) {
        var inputCount by remember { mutableStateOf(targetLimit.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomCountDialog = false },
            title = { AlDeenText("Set Custom Count (مرضی کا ہدف)", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
            text = {
                OutlinedTextField(
                    value = inputCount,
                    onValueChange = { inputCount = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Enter Target Count (e.g., 70, 313, 500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = inputCount.toIntOrNull()
                        if (num != null && num > 0) {
                            targetLimit = num
                            isInfiniteMode = false
                            prefs.edit()
                                .putInt("target_limit_${selectedPhrase.id}", num)
                                .putBoolean("infinite_mode_${selectedPhrase.id}", false)
                                .apply()
                        }
                        showCustomCountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Count")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Reset Confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Counter?") },
            text = { Text("Current count ($count) will be saved to history before resetting to 0.") },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Reset to 0")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom Sheet: Dhikr History
    if (showHistorySheet) {
        val historyList = remember {
            val json = prefs.getString("history_records", "[]") ?: "[]"
            try {
                val array = JSONArray(json)
                val list = mutableListOf<DhikrHistoryEntry>()
                for (i in array.length() - 1 downTo 0) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        DhikrHistoryEntry(
                            phraseId = obj.optString("phraseId", ""),
                            phraseName = obj.optString("phraseName", "Dhikr"),
                            count = obj.optInt("count", 0),
                            timestamp = obj.optLong("timestamp", 0L),
                            formattedDate = obj.optString("formattedDate", "")
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList<DhikrHistoryEntry>()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AlDeenText(
                            text = "Tasbeeh History (تسبیح کی تاریخ)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        )
                        Text(
                            text = "Word-by-word cumulative counts and sessions",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    // Clear history button
                    TextButton(
                        onClick = {
                            prefs.edit().putString("history_records", "[]").apply()
                            showHistorySheet = false
                        }
                    ) {
                        Text("Clear All", color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary by word
                Text(
                    text = "Cumulative Totals per Word:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPhrases.forEach { p ->
                        val tot = getCumulativeCount(p.id) + (if (p.id == selectedPhrase.id) count else 0)
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = EmeraldContainer.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, MintBorder)
                        ) {
                            Text(
                                text = "${p.transliteration}: $tot",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recent Sessions Log:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No completed sessions yet. Start reciting!", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = entry.phraseName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                        )
                                        Text(
                                            text = entry.formattedDate,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                    Surface(
                                        shape = AlDeenTokens.ShapePill,
                                        color = EmeraldPrimary
                                    ) {
                                        Text(
                                            text = "+${entry.count}",
                                            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
