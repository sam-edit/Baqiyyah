package com.example.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

class TasbeehWidgetResetDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val phraseId = intent.getStringExtra("phrase_id") ?: "subhanallah"
        val phraseName = intent.getStringExtra("phrase_name") ?: "SubhanAllah"
        val phraseArabic = intent.getStringExtra("phrase_arabic") ?: "سُبْحَانَ اللَّٰهِ"
        val currentCount = intent.getIntExtra("current_count", 0)

        setContent {
            AlDeenTheme(themeMode = AppThemeMode.DARK) {
                ResetDialogContent(
                    phraseArabic = phraseArabic,
                    phraseName = phraseName,
                    currentCount = currentCount,
                    onConfirm = {
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            TasbeehSharedRepository.resetCount(applicationContext, appWidgetId)
                            TasbeehWidgetProvider.updateAllWidgets(applicationContext)
                        }
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ResetDialogContent(
    phraseArabic: String,
    phraseName: String,
    currentCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF10201A),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF2E453E)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2B240B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AlDeenText(
                    text = "کاؤنٹر دوبارہ صفر کریں؟",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFF0F6F3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    targetScript = ScriptLanguage.URDU
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Reset $phraseName Counter?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFA0B5AC),
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF142B23),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = phraseArabic,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF2DD4A2),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current Count: $currentCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFF0F6F3),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Current count will be safely saved to your lifetime history.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF768E85),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E453E)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0B5AC))
                    ) {
                        Text("منسوخ (Cancel)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text("ری سیٹ (Reset)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
