package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

class TasbeehWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            AlDeenTheme(themeMode = AppThemeMode.DARK) {
                TasbeehWidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onSave = { selectedPhraseId, target ->
                        TasbeehSharedRepository.setWidgetPhrase(applicationContext, appWidgetId, selectedPhraseId)
                        TasbeehSharedRepository.setWidgetTarget(applicationContext, appWidgetId, target)

                        TasbeehWidgetProvider.updateAppWidget(
                            applicationContext,
                            AppWidgetManager.getInstance(applicationContext),
                            appWidgetId
                        )

                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasbeehWidgetConfigScreen(
    appWidgetId: Int,
    onSave: (phraseId: String, target: Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allPhrases = remember { TasbeehSharedRepository.getAllPhrases(context) }
    val initialPhrase = remember { TasbeehSharedRepository.getWidgetPhrase(context, appWidgetId) }
    val initialTarget = remember { TasbeehSharedRepository.getTarget(context, appWidgetId, initialPhrase) }

    var selectedPhraseId by remember { mutableStateOf(initialPhrase.id) }
    var selectedTarget by remember { mutableIntStateOf(initialTarget) }

    val targetOptions = listOf(33, 99, 100, 313, 1000)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "Quick Tasbeeh Widget",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2DD4A2)
                            )
                        )
                        Text(
                            text = "Configure Dhikr & Target Limit",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0B5AC))
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF2DD4A2))
                    }
                },
                actions = {
                    Button(
                        onClick = { onSave(selectedPhraseId, selectedTarget) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4A2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF052A20), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", color = Color(0xFF052A20), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1D18))
            )
        },
        containerColor = Color(0xFF0B1713)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "TARGET COUNT (ہدف)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFACC15),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targetOptions.forEach { t ->
                        val isSelected = selectedTarget == t
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF134235) else Color(0xFF13231D),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2DD4A2) else Color(0xFF2E453E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTarget = t }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$t",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF2DD4A2) else Color(0xFFF0F6F3),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "SELECT DHIKR (ذکر کا انتخاب)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF2DD4A2),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            items(allPhrases) { phrase ->
                val isSelected = phrase.id == selectedPhraseId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPhraseId = phrase.id
                            if (selectedTarget == 33 || selectedTarget == 100) {
                                selectedTarget = phrase.defaultTarget
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF134235) else Color(0xFF10201A)
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isSelected) Color(0xFF2DD4A2) else Color(0xFF2E453E)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = phrase.arabic,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF2DD4A2),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = phrase.transliteration,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFF0F6F3),
                                    fontSize = 13.sp
                                )
                            )
                            if (phrase.urduMeaning.isNotBlank()) {
                                Text(
                                    text = phrase.urduMeaning,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFA0B5AC),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedPhraseId = phrase.id
                                selectedTarget = phrase.defaultTarget
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF2DD4A2),
                                unselectedColor = Color(0xFF768E85)
                            )
                        )
                    }
                }
            }
        }
    }
}
