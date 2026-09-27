package com.example.ui.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DailyGoalItem
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    prayerState: com.example.domain.model.PrayerState,
    location: com.example.domain.model.UserLocation,
    formattedCurrentTime: String,
    formattedUpcomingTime: String,
    onOpenDrawer: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val fardPrayers by viewModel.fardPrayers.collectAsStateWithLifecycle()
    val dailyDeeds by viewModel.dailyDeeds.collectAsStateWithLifecycle()
    val customGoals by viewModel.customGoals.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val totalCount = fardPrayers.size + dailyDeeds.size + customGoals.size
    val progressFraction by viewModel.progressFraction.collectAsStateWithLifecycle()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()

    var showAddCustomGoalDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val today = viewModel.getToday()
    val earliestDate = viewModel.getEarliestSelectableDate()
    val installDate = viewModel.getInstallDate()

    val isToday = selectedDate == today
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "goals_progress")

    val formattedDateString = when {
        isToday -> "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        selectedDate == today.minusDays(1) -> "Yesterday, ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("goals_screen_root")
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

            // Title and Report Download Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Worship Goals",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 20.sp
                        )
                    )
                    AlDeenText(
                        text = "أَهْدَافُ الْعِبَادَة",
                        style = AlDeenTypography.ArabicHeading.copy(
                            color = EmeraldPrimary,
                            fontSize = 15.sp
                        )
                    )
                }

                // Download Report Button with Icon
                Button(
                    onClick = { showReportDialog = true },
                    shape = AlDeenTokens.ShapePill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("goals_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Report",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Switcher Row with clickable Calendar trigger
            Surface(
                shape = AlDeenTokens.ShapePill,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousDay() },
                        enabled = viewModel.canGoPreviousDay(),
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Day",
                            tint = if (viewModel.canGoPreviousDay()) EmeraldPrimary else TextTertiary.copy(alpha = 0.35f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showCalendarDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Open Calendar",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDateString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextDay() },
                        enabled = viewModel.canGoNextDay(),
                        modifier = Modifier.size(AlDeenTokens.TouchTargetMin)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Day",
                            tint = if (viewModel.canGoNextDay()) EmeraldPrimary else TextTertiary.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            // Past Record Read-Only Info Banner
            if (!isToday) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = AlDeenTokens.ShapeCard,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AlDeenText(
                            text = "Past Record (صرف ملاحظہ کریں) • Previous days are read-only and cannot be changed.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Body Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(top = AlDeenTokens.SpacingSmall, bottom = AlDeenTokens.SpacingXXLarge)
        ) {
            // Overall Progress Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goals_progress_card"),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DashboardGradient)
                            .padding(AlDeenTokens.SpacingLarge)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}% Completed",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "$completedCount of $totalCount worship goals fulfilled",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                }

                                Surface(
                                    shape = AlDeenTokens.ShapePill,
                                    color = GoldAccent
                                ) {
                                    Text(
                                        text = if (completedCount == totalCount && totalCount > 0) "Completed!" else "In Progress",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(AlDeenTokens.ShapePill),
                                color = GoldAccent,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            AlDeenText(
                                text = "مَا شَاءَ اللَّٰهُ • \"The deeds most loved by Allah are those done regularly, even if small.\" (Sahih al-Bukhari)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Section 1: 5 Fard Prayers
            item {
                AlDeenText(
                    text = "The 5 Fard Prayers (صلوات المفروضة)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(fardPrayers, key = { it.goalId }) { goal ->
                GoalItemRow(
                    goal = goal,
                    isEditable = isToday,
                    onToggle = { viewModel.toggleGoal(goal) }
                )
            }

            // Section 2: Daily Spiritual Deeds
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AlDeenText(
                    text = "Daily Spiritual Habits (الأذكار والأعمال)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 16.sp
                    )
                )
            }

            items(dailyDeeds, key = { it.goalId }) { goal ->
                GoalItemRow(
                    goal = goal,
                    isEditable = isToday,
                    onToggle = { viewModel.toggleGoal(goal) }
                )
            }

            // Section 3: Personal Custom Goals
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        AlDeenText(
                            text = "Custom Personal Goals (ذاتی اہداف)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp
                            )
                        )
                        AlDeenText(
                            text = "اپنے مخصوص اہداف شامل اور ٹریک کریں",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    if (isToday) {
                        FilledTonalButton(
                            onClick = { showAddCustomGoalDialog = true },
                            shape = AlDeenTokens.ShapePill,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("add_custom_goal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Goal",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Goal",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            if (customGoals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_custom_goals_card"),
                        shape = AlDeenTokens.ShapeCard,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MintBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                AlDeenText(
                                    text = "No custom goals yet (کوئی ذاتی ہدف نہیں)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Tap 'Add Goal' above to track Tahajjud, Surah Yaseen, Astaghfirullah, or any personal deed.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                items(customGoals, key = { it.goalId }) { goal ->
                    GoalItemRow(
                        goal = goal,
                        isEditable = isToday,
                        onToggle = { viewModel.toggleGoal(goal) },
                        onDelete = if (isToday) { { viewModel.deleteCustomGoal(goal.goalId) } } else null
                    )
                }
            }
        }
    }

    if (showAddCustomGoalDialog) {
        AddCustomGoalDialog(
            onDismiss = { showAddCustomGoalDialog = false },
            onAdd = { title, urdu ->
                viewModel.addCustomGoal(title, urdu)
                showAddCustomGoalDialog = false
            }
        )
    }

    if (showCalendarDialog) {
        GoalsCalendarDialog(
            selectedDate = selectedDate,
            earliestDate = earliestDate,
            latestDate = today,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    if (showReportDialog) {
        GoalsReportDialog(
            isGenerating = isGeneratingReport,
            installDate = installDate,
            today = today,
            onDismiss = { showReportDialog = false },
            onDownloadReport = { days ->
                viewModel.generatePdfReport(context, days) {
                    showReportDialog = false
                }
            }
        )
    }
}

@Composable
fun AddCustomGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, urdu: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var urduTitle by remember { mutableStateOf("") }

    val presets = listOf(
        "Tahajjud Prayer" to "نماز تہجد",
        "Surah Yaseen Recitation" to "تلاوت سورۃ یٰس",
        "Surah Al-Mulk at Night" to "سورۃ الملک قبل از خواب",
        "100x Astaghfirullah" to "۱۰۰ بار استغفار",
        "100x Durood Shareef" to "۱۰۰ بار درود شریف",
        "Sadaqah / Giving Charity" to "صدقہ و خیرات",
        "Islamic Book Reading" to "دینی کتب کا مطالعہ"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_custom_goal_dialog"),
            shape = AlDeenTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AlDeenTokens.SpacingLarge)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlDeenText(
                        text = "Add Custom Goal (نیا ہدف)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 17.sp
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AlDeenText(
                    text = "Quick Suggestions (تجویز کردہ):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets) { (eng, urd) ->
                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = EmeraldContainer,
                            border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                title = eng
                                urduTitle = urd
                            }
                        ) {
                            Text(
                                text = "$eng ($urd)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (English)") },
                    placeholder = { Text("e.g. Read 5 pages of Tafseer") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MintBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_goal_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = urduTitle,
                    onValueChange = { urduTitle = it },
                    label = { AlDeenText("ہدف کا نام (اردو - اختیاری)") },
                    placeholder = { AlDeenText("مثلاً روزانہ ۵ صفحات تفسیر") },
                    textStyle = AlDeenTypography.UrduBody.copy(color = TextPrimary),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MintBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_goal_urdu_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAdd(title, urduTitle)
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = AlDeenTokens.ShapePill,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.testTag("save_custom_goal_button")
                    ) {
                        AlDeenText("Add Goal (شامل کریں)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalItemRow(
    goal: DailyGoalItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    onDelete: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backgroundColor by animateColorAsState(
        targetValue = if (goal.isCompleted) EmeraldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        label = "goal_bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AlDeenTokens.ShapeCard)
            .clickable {
                if (isEditable) {
                    onToggle()
                } else {
                    android.widget.Toast.makeText(context, "Only present day can be edited (صرف آج کے دن کی تبدیلی ممکن ہے)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .testTag("goal_item_${goal.goalId}"),
        shape = AlDeenTokens.ShapeCard,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            AlDeenTokens.CardBorderWidth,
            if (goal.isCompleted) EmeraldPrimary.copy(alpha = 0.4f) else MintBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = goal.isCompleted,
                    onCheckedChange = {
                        if (isEditable) {
                            onToggle()
                        } else {
                            android.widget.Toast.makeText(context, "Only present day can be edited (صرف آج کے دن کی تبدیلی ممکن ہے)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isEditable,
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldPrimary,
                        checkmarkColor = Color.White,
                        disabledCheckedColor = EmeraldPrimary.copy(alpha = 0.7f),
                        disabledUncheckedColor = TextTertiary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.testTag("goal_checkbox_${goal.goalId}")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = goal.titleEnglish,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (goal.isCompleted) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (goal.isCompleted) TextSecondary else TextPrimary,
                            textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            fontSize = 15.sp
                        )
                    )
                    AlDeenText(
                        text = goal.titleUrdu,
                        style = AlDeenTypography.UrduBody.copy(
                            color = if (goal.isCompleted) EmeraldPrimary.copy(alpha = 0.7f) else EmeraldPrimary,
                            fontSize = 13.sp
                        ),
                        targetScript = ScriptLanguage.URDU
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (goal.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_goal_${goal.goalId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete custom goal",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsCalendarDialog(
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedYearMonth by remember {
        mutableStateOf(YearMonth.from(selectedDate))
    }

    val earliestYearMonth = YearMonth.from(earliestDate)
    val latestYearMonth = YearMonth.from(latestDate)

    val canGoPrevMonth = displayedYearMonth.isAfter(earliestYearMonth)
    val canGoNextMonth = displayedYearMonth.isBefore(latestYearMonth)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = AlDeenTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MintBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(AlDeenTokens.ShapePill)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            AlDeenText(
                                text = "Select Date (تاریخ)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            AlDeenText(
                                text = "Last 60 Days Tracking Window (پچھلے 60 دن)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AlDeenTokens.ShapePill)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (canGoPrevMonth) {
                                displayedYearMonth = displayedYearMonth.minusMonths(1)
                            }
                        },
                        enabled = canGoPrevMonth,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = if (canGoPrevMonth) EmeraldPrimary else TextTertiary.copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = displayedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (canGoNextMonth) {
                                displayedYearMonth = displayedYearMonth.plusMonths(1)
                            }
                        },
                        enabled = canGoNextMonth,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = if (canGoNextMonth) EmeraldPrimary else TextTertiary.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weekday Row (Mon..Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    for (dow in daysOfWeek) {
                        Text(
                            text = dow,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (dow == "Fr") EmeraldPrimary else TextTertiary,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Days Grid
                val firstDayOfWeek = displayedYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val totalSlots = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until (totalSlots / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayNum = slotIndex - (firstDayOfWeek - 1) + 1

                                if (dayNum in 1..daysInMonth) {
                                    val date = displayedYearMonth.atDay(dayNum)
                                    val isSelectable = !date.isBefore(earliestDate) && !date.isAfter(latestDate)
                                    val isSelected = date == selectedDate
                                    val isToday = date == latestDate

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(AlDeenTokens.ShapePill)
                                            .then(
                                                when {
                                                    isSelected -> Modifier.background(EmeraldPrimary)
                                                    isToday -> Modifier.border(1.5.dp, EmeraldPrimary, AlDeenTokens.ShapePill)
                                                    else -> Modifier.background(Color.Transparent)
                                                }
                                            )
                                            .clickable(enabled = isSelectable) {
                                                onDateSelected(date)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> EmeraldPrimary
                                                    isSelectable -> TextPrimary
                                                    else -> TextTertiary.copy(alpha = 0.3f)
                                                },
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDateSelected(latestDate)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AlDeenText(
                            text = "Go to Today (آج)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = AlDeenTokens.ShapePill,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("Close", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsReportDialog(
    isGenerating: Boolean,
    installDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onDownloadReport: (days: Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(30) }

    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = AlDeenTokens.ShapeCard,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MintBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(AlDeenTokens.ShapePill)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            AlDeenText(
                                text = "Worship Report (رپورٹ)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = "Download PDF of Goals & Prayers",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AlDeenText(
                    text = "Select Number of Days (دن منتخب کریں):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Options (7, 15, 30, 60 days)
                val dayOptions = listOf(
                    7 to ("Last 7 Days" to "پچھلے 7 دن"),
                    15 to ("Last 15 Days" to "پچھلے 15 دن"),
                    30 to ("Last 30 Days" to "پچھلے 30 دن"),
                    60 to ("Last 60 Days" to "پچھلے 60 دن")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayOptions.forEach { (days, labels) ->
                        val (en, ur) = labels
                        val isSelected = selectedDays == days
                        val calcStart = today.minusDays((days - 1).toLong())
                        val effectiveStart = calcStart

                        Surface(
                            shape = AlDeenTokens.ShapeCard,
                            color = if (isSelected) EmeraldLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) EmeraldPrimary else MintBorder.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isGenerating) { selectedDays = days }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedDays = days },
                                        enabled = !isGenerating,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = EmeraldPrimary,
                                            unselectedColor = TextTertiary
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = en,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                                fontSize = 14.sp
                                            )
                                        )
                                        Text(
                                            text = "${effectiveStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${today.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                AlDeenText(
                                    text = ur,
                                    style = AlDeenTypography.UrduBody.copy(
                                        color = if (isSelected) EmeraldPrimary else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    targetScript = ScriptLanguage.URDU
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Retention info note
                Surface(
                    shape = AlDeenTokens.ShapeCard,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download reports for any chosen period (7, 15, 30, or 60 days). Days without recorded goals will show as 0% completion. Goals data older than 60 days is automatically cleared.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        shape = AlDeenTokens.ShapePill,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { onDownloadReport(selectedDays) },
                        enabled = !isGenerating,
                        shape = AlDeenTokens.ShapePill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download PDF",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
