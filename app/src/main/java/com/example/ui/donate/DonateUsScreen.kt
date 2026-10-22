package com.example.ui.donate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.ImageLoader
import com.example.data.model.BankConfig
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateUsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonateViewModel = viewModel(),
    isScreenActive: Boolean = true
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val imageLoader by viewModel.imageLoader.collectAsState()

    var selectedQrPreviewUrl by remember { mutableStateOf<String?>(null) }

    // Intercept hardware/system back navigation to guarantee disconnect
    BackHandler(enabled = isScreenActive) {
        viewModel.stopListeningAndGoOffline()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "Donate Us",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        AlDeenText(
                            text = "بقیہ کی معاونت کریں",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopListeningAndGoOffline()
                            onBack()
                        },
                        modifier = Modifier.testTag("donate_us_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmeraldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("donate_us_refresh_btn")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("donate_us_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 1. ISLAMIC HERO BANNER =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donate_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DashboardGradient)
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AlDeenText(
                            text = "مَّثَلُ الَّذِينَ يُنفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ كَمَثَلِ حَبَّةٍ أَنبَتَتْ سَبْعَ سَنَابِلَ",
                            style = AlDeenTypography.ArabicSubheading.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 17.sp,
                                lineHeight = 26.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"The example of those who spend their wealth in the way of Allah is like a seed of grain which grows seven spikes; in each spike is a hundred grains.\" (2:261)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GoldAccent,
                                    modifier = Modifier.size(7.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Donate US",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ================= 2. LIVE BANK ACCOUNTS SECTION =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bank Accounts",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Live Fetch (Secured)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            when (val state = uiState) {
                is DonateUiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MintBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Fetching live bank details...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "براہ کرم انتظار فرمائیں",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                is DonateUiState.Success -> {
                    state.banks.forEach { bank ->
                        BankCard(
                            bank = bank,
                            imageLoader = imageLoader,
                            onCopyText = { label, text ->
                                copyToClipboard(context, label, text)
                            },
                            onShareBank = {
                                shareBankDetails(context, bank)
                            },
                            onPreviewQr = { url ->
                                selectedQrPreviewUrl = url
                            }
                        )
                    }
                }

                is DonateUiState.Empty -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MintBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Again")
                            }
                        }
                    }
                }
            }

            // ================= 3. TRANSPARENCY & PURPOSE CARD =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donate_transparency_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MintBorder.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How Your Donation Helps",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Your generous donations help us improve Baqiyyah and make Islamic knowledge more accessible to everyone. Every contribution supports app maintenance, new features, and a better experience for our users.\n" +
                               "• Every contribution, no matter how small, makes a difference. Thank you for being part of our journey.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // QR Code Large Preview Dialog
    selectedQrPreviewUrl?.takeIf { imageLoader != null && isScreenActive }?.let { qrUrl ->
        AlertDialog(
            onDismissRequest = { selectedQrPreviewUrl = null },
            confirmButton = {
                TextButton(onClick = { selectedQrPreviewUrl = null }) {
                    Text("Close", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "Scan QR to Donate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = qrUrl,
                        imageLoader = requireNotNull(imageLoader),
                        contentDescription = "Bank QR Code",
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        )
    }
}

@Composable
private fun BankCard(
    bank: BankConfig,
    imageLoader: ImageLoader?,
    onCopyText: (String, String) -> Unit,
    onShareBank: () -> Unit,
    onPreviewQr: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_card_${bank.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Bank Title & Account Holder Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = bank.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bank.accountHolder.isNotBlank()) {
                            Text(
                                text = "Title: ${bank.accountHolder}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onShareBank,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Bank Details",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MintBorder.copy(alpha = 0.7f), thickness = 0.8.dp)

            // 1. Account Number Detail Row
            if (bank.accountNumber.isNotBlank()) {
                DetailCopyRow(
                    label = "Account Number",
                    value = bank.accountNumber,
                    testTag = "copy_account_${bank.id}",
                    onCopy = { onCopyText("Account Number", bank.accountNumber) }
                )
            }

            // 2. IBAN Detail Row
            if (bank.iban.isNotBlank()) {
                DetailCopyRow(
                    label = "IBAN",
                    value = bank.iban,
                    testTag = "copy_iban_${bank.id}",
                    onCopy = { onCopyText("IBAN", bank.iban) }
                )
            }

            // 3. Account Holder Detail Row (if not already shown prominently)
            if (bank.accountHolder.isNotBlank() && !bank.displayName.contains(bank.accountHolder, ignoreCase = true)) {
                DetailCopyRow(
                    label = "Account Holder",
                    value = bank.accountHolder,
                    testTag = "copy_holder_${bank.id}",
                    onCopy = { onCopyText("Account Holder", bank.accountHolder) }
                )
            }

            // 4. QR Code Section (if available)
            if (bank.qrImageUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPreviewQr(bank.qrImageUrl) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Scan QR Code",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Tap to expand and scan in banking app",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                )
                            }
                        }

                        if (imageLoader != null) AsyncImage(
                            imageLoader = imageLoader,
                            model = bank.qrImageUrl,
                            contentDescription = "QR Thumbnail",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // 5. Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val fullInfo = buildString {
                            appendLine("Bank: ${bank.displayName}")
                            if (bank.accountHolder.isNotBlank()) appendLine("Account Title: ${bank.accountHolder}")
                            if (bank.accountNumber.isNotBlank()) appendLine("Account No: ${bank.accountNumber}")
                            if (bank.iban.isNotBlank()) appendLine("IBAN: ${bank.iban}")
                        }.trim()
                        onCopyText("Bank Details", fullInfo)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
                }

                Button(
                    onClick = onShareBank,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                }
            }
        }
    }
}

@Composable
private fun DetailCopyRow(
    label: String,
    value: String,
    testTag: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(32.dp)
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareBankDetails(context: Context, bank: BankConfig) {
    val shareText = buildString {
        appendLine("Assalam-u-Alaikum,")
        appendLine("Here are the donation details for Baqiyyah Project:")
        appendLine()
        appendLine("🏦 Bank: ${bank.displayName}")
        if (bank.accountHolder.isNotBlank()) appendLine("👤 Account Title: ${bank.accountHolder}")
        if (bank.accountNumber.isNotBlank()) appendLine("🔢 Account Number: ${bank.accountNumber}")
        if (bank.iban.isNotBlank()) appendLine("🌐 IBAN: ${bank.iban}")
        appendLine()
        appendLine("May Allah accept your charity and bless you abundantly!")
    }.trim()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Baqiyyah Project Donation Details")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Donation Account Details"))
}
