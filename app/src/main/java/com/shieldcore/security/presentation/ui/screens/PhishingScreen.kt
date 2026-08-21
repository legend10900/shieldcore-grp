package com.shieldcore.security.presentation.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.model.FraudAnalysisReport
import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.ScamCategory
import com.shieldcore.security.domain.model.UpiVerificationResult
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.PhishingInspectorTab
import com.shieldcore.security.presentation.viewmodel.PhishingViewModel
import com.shieldcore.security.service.PhishingVpnService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhishingScreen(
    viewModel: PhishingViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val startIntent = Intent(context, PhishingVpnService::class.java).apply {
                action = PhishingVpnService.ACTION_START
            }
            context.startService(startIntent)
            viewModel.setVpnActive(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = CoralTeal)
                        Text(" • Threat Shield", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 15.sp)
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    // Senior Protection Mode Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = if (uiState.isSeniorMode) ElectricViolet.copy(alpha = 0.2f) else DarkCardSurface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (uiState.isSeniorMode) ElectricViolet else DarkCardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.toggleSeniorMode(!uiState.isSeniorMode) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Elderly,
                            contentDescription = "Senior Mode",
                            tint = if (uiState.isSeniorMode) ElectricViolet else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Senior View",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSeniorMode) TextPrimary else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Web Shield VPN Hero Card
            item(key = "vpn_hero_card") {
                val vpnAccent = if (uiState.isVpnActive) MatrixGreen else CoralTeal

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurfaceElevated,
                    borderBrush = Brush.linearGradient(listOf(vpnAccent.copy(alpha = 0.5f), Color.Transparent))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(vpnAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, vpnAccent.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isVpnActive) Icons.Default.VpnLock else Icons.Default.Language,
                                contentDescription = null,
                                tint = vpnAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Web & SMS Shield", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                NeonBadge(
                                    text = if (uiState.isVpnActive) "PROTECTED" else "READY",
                                    accentColor = vpnAccent
                                )
                            }
                            Text(
                                text = if (uiState.isVpnActive) "Active DNS filter & live link inspection enabled." else "Inspect incoming SMS, payment traps & fake links.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = uiState.isVpnActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val vpnIntent = VpnService.prepare(context)
                                    if (vpnIntent != null) {
                                        vpnLauncher.launch(vpnIntent)
                                    } else {
                                        val startIntent = Intent(context, PhishingVpnService::class.java).apply {
                                            action = PhishingVpnService.ACTION_START
                                        }
                                        context.startService(startIntent)
                                        viewModel.setVpnActive(true)
                                    }
                                } else {
                                    val stopIntent = Intent(context, PhishingVpnService::class.java).apply {
                                        action = PhishingVpnService.ACTION_STOP
                                    }
                                    context.startService(stopIntent)
                                    viewModel.setVpnActive(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MatrixGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardSurface
                            )
                        )
                    }
                }
            }

            // 2. Tab Navigation Switcher
            item(key = "tab_switcher") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCardSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        title = "📩 SMS / Text",
                        selected = uiState.selectedTab == PhishingInspectorTab.SMS_MESSAGE_ANALYZER,
                        accentColor = CoralTeal,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(PhishingInspectorTab.SMS_MESSAGE_ANALYZER) }
                    )
                    TabButton(
                        title = "🔗 Web Link",
                        selected = uiState.selectedTab == PhishingInspectorTab.URL_DEEP_INSPECTOR,
                        accentColor = SkyBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(PhishingInspectorTab.URL_DEEP_INSPECTOR) }
                    )
                    TabButton(
                        title = "💳 UPI Verifier",
                        selected = uiState.selectedTab == PhishingInspectorTab.UPI_PAYMENT_VERIFIER,
                        accentColor = ElectricViolet,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(PhishingInspectorTab.UPI_PAYMENT_VERIFIER) }
                    )
                }
            }

            // 3. Quick-Test Scam Scenario Chips
            item(key = "scam_scenario_chips") {
                Column {
                    Text(
                        text = "🧪 Test Real-World Scam Scenarios:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScamPresetChip("📦 Fake Delivery", LaserRed) { viewModel.loadScamTemplate("FAKE_DELIVERY") }
                        ScamPresetChip("💳 UPI Cashback Trap", LaserRed) { viewModel.loadScamTemplate("UPI_CASHBACK") }
                        ScamPresetChip("⚡ Electricity Cutoff", LaserRed) { viewModel.loadScamTemplate("ELECTRICITY_CUTOFF") }
                        ScamPresetChip("🏦 Bank KYC Freeze", LaserRed) { viewModel.loadScamTemplate("BANK_KYC") }
                        ScamPresetChip("✅ Genuine Order", MatrixGreen) { viewModel.loadScamTemplate("LEGITIMATE") }
                    }
                }
            }

            // 4. Tab Content Rendering
            when (uiState.selectedTab) {
                PhishingInspectorTab.SMS_MESSAGE_ANALYZER -> {
                    item(key = "sms_analyzer_section") {
                        SmsMessageAnalyzerView(
                            uiState = uiState,
                            onMessageChange = viewModel::onMessageTextChanged,
                            onSenderChange = viewModel::onSenderInputChanged,
                            onAnalyze = viewModel::analyzeMessage
                        )
                    }
                }
                PhishingInspectorTab.URL_DEEP_INSPECTOR -> {
                    item(key = "url_inspector_section") {
                        UrlDeepInspectorView(
                            uiState = uiState,
                            onUrlChange = viewModel::onUrlInputChanged,
                            onInspect = viewModel::testUrlSafety,
                            onMarkSafety = viewModel::markCurrentUrl
                        )
                    }
                }
                PhishingInspectorTab.UPI_PAYMENT_VERIFIER -> {
                    item(key = "upi_verifier_section") {
                        UpiPaymentVerifierView(
                            uiState = uiState,
                            onUpiChange = viewModel::onUpiInputChanged,
                            onVerify = viewModel::verifyUpi
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    selected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

@Composable
fun ScamPresetChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ----------------------------------------------------
// 1. SMS & Message Text Analyzer View
// ----------------------------------------------------
@Composable
fun SmsMessageAnalyzerView(
    uiState: com.shieldcore.security.presentation.viewmodel.PhishingUiState,
    onMessageChange: (String) -> Unit,
    onSenderChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkCardSurface,
            borderBrush = Brush.linearGradient(listOf(CoralTeal.copy(alpha = 0.4f), Color.Transparent))
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paste Incoming SMS or Message Text",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clip.isNullOrBlank()) {
                                onMessageChange(clip)
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = CoralTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Text", fontSize = 12.sp, color = CoralTeal, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.messageText,
                    onValueChange = onMessageChange,
                    placeholder = { Text("e.g. India Post: Your parcel is blocked due to invalid address. Update at indiapost-track.xyz...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoralTeal,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.senderInput,
                        onValueChange = onSenderChange,
                        placeholder = { Text("Sender ID / Phone (e.g. VM-INDPOST)", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoralTeal,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    GlowGradientButton(
                        text = "Scan Fraud",
                        onClick = onAnalyze,
                        icon = Icons.Default.Security,
                        gradient = if (uiState.messageReport?.isScam == true) DangerRedGradient else PrimaryGradient,
                        isLoading = uiState.isAnalyzingMessage,
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }

        // Display Analysis Report
        uiState.messageReport?.let { report ->
            FraudAnalysisReportCard(report = report, isSeniorMode = uiState.isSeniorMode)
        }
    }
}

@Composable
fun FraudAnalysisReportCard(report: FraudAnalysisReport, isSeniorMode: Boolean) {
    val context = LocalContext.current
    val isScam = report.isScam
    val statusColor = if (isScam) LaserRed else MatrixGreen
    val statusGradient = if (isScam) DangerRedGradient else SafeGreenGradient

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isSeniorMode) DarkSurfaceElevated else DarkCardSurface,
        borderBrush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.8f), Color.Transparent))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape)
                        .border(BorderStroke(2.dp, statusColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isScam) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isScam) "⚠️ SCAM DETECTED" else "✅ SAFE MESSAGE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isSeniorMode) 18.sp else 16.sp,
                        color = statusColor
                    )
                    Text(
                        text = report.category.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = "RISK ${report.riskScore}%",
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Senior-Friendly Plain Language Guidance Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = (if (isScam) LaserRed else MatrixGreen).copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, (if (isScam) LaserRed else MatrixGreen).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isScam) Icons.Default.GppBad else Icons.Default.GppGood,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSeniorMode) "WHAT YOU SHOULD DO (SENIOR ADVISORY):" else "ACTION GUIDANCE:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isSeniorMode) 13.sp else 12.sp,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = report.seniorAdvice,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSeniorMode) 16.sp else 14.sp,
                        color = TextPrimary,
                        lineHeight = if (isSeniorMode) 22.sp else 19.sp
                    )
                }
            }

            // Highlighted Scam Keywords
            if (report.highlightedKeywords.isNotEmpty()) {
                Column {
                    Text("Deceptive Keywords & Tactics Found:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        report.highlightedKeywords.forEach { kw ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LaserRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, LaserRed.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "⚠️ $kw",
                                    color = LaserRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Extracted Link / UPI Warnings
            if (report.extractedUrls.isNotEmpty()) {
                Column {
                    Text("Suspicious Links in Message:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    report.extractedUrls.forEach { url ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, tint = LaserRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(url, fontSize = 12.sp, color = LaserRed, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            if (report.extractedUpiHandles.isNotEmpty()) {
                Column {
                    Text("UPI Payment Request Triggers:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    report.extractedUpiHandles.forEach { upi ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(upi, fontSize = 12.sp, color = ElectricViolet, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            // Share Warning Button
            if (isScam) {
                OutlinedButton(
                    onClick = {
                        val shareText = "⚠️ ShieldCore Scam Alert!\n\nThis message is a ${report.category.displayName.uppercase()}:\n\n\"${report.rawText}\"\n\n🛑 Advisory: ${report.seniorAdvice}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Scam Warning", shareText))
                        Toast.makeText(context, "Warning copied! Paste to family/friends on WhatsApp.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LaserRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserRed)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Warning to Warn Family Members", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. Web Link Inspector View
// ----------------------------------------------------
@Composable
fun UrlDeepInspectorView(
    uiState: com.shieldcore.security.presentation.viewmodel.PhishingUiState,
    onUrlChange: (String) -> Unit,
    onInspect: () -> Unit,
    onMarkSafety: (LinkSafetyStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkCardSurface,
            borderBrush = Brush.linearGradient(listOf(SkyBlue.copy(alpha = 0.4f), Color.Transparent))
        ) {
            Column {
                Text(
                    text = "Enter Website URL or Domain",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.testUrl,
                        onValueChange = onUrlChange,
                        placeholder = { Text("https://indiapost-tracking.xyz", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    GlowGradientButton(
                        text = "Inspect",
                        onClick = onInspect,
                        icon = Icons.Default.Search,
                        gradient = PrimaryGradient,
                        isLoading = uiState.isCheckingUrl,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }
        }

        // Display URL Result
        uiState.checkResult?.let { result ->
            val isMalicious = result.isMalicious
            val statusColor = if (isMalicious) LaserRed else MatrixGreen

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.6f), Color.Transparent))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(statusColor.copy(alpha = 0.15f), CircleShape)
                                .border(BorderStroke(1.dp, statusColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMalicious) Icons.Default.Dangerous else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMalicious) "Dangerous / Phishing Link" else "Safe & Verified Website",
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 16.sp
                            )
                            Text(
                                text = result.threatType ?: "No malicious signatures or brand spoofing detected",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        NeonBadge(
                            text = if (isMalicious) "MALICIOUS" else "VERIFIED",
                            accentColor = statusColor
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Detection Engine:", fontSize = 12.sp, color = TextMuted)
                        Text(result.detectionSource ?: "ShieldCore Engine", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onMarkSafety(LinkSafetyStatus.PHISHING) },
                            colors = ButtonDefaults.buttonColors(containerColor = LaserRed.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LaserRed.copy(alpha = 0.5f))
                        ) {
                            Text("Report Scam", color = LaserRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onMarkSafety(LinkSafetyStatus.SAFE) },
                            colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.5f))
                        ) {
                            Text("Mark Safe", color = MatrixGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. UPI Payment Trap Verifier View
// ----------------------------------------------------
@Composable
fun UpiPaymentVerifierView(
    uiState: com.shieldcore.security.presentation.viewmodel.PhishingUiState,
    onUpiChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Educational Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ElectricViolet.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GOLDEN RULE: You NEVER need a PIN to receive money! Entering your UPI PIN always SENDS money out of your account.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkCardSurface,
            borderBrush = Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.4f), Color.Transparent))
        ) {
            Column {
                Text(
                    text = "Verify UPI Payment Link or VPA Handle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.upiInput,
                    onValueChange = onUpiChange,
                    placeholder = { Text("upi://pay?pa=refund@paytm&pn=Cashback&am=2000", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                GlowGradientButton(
                    text = "Verify UPI Link / Handle",
                    onClick = onVerify,
                    icon = Icons.Default.QrCodeScanner,
                    gradient = DangerRedGradient,
                    isLoading = uiState.isCheckingUpi,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Display UPI Verification Result
        uiState.upiResult?.let { upi ->
            val isTrap = upi.isDangerousTrap
            val statusColor = if (isTrap) LaserRed else MatrixGreen

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.7f), Color.Transparent))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(statusColor.copy(alpha = 0.15f), CircleShape)
                                .border(BorderStroke(1.dp, statusColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTrap) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTrap) "⚠️ UPI MONEY DEDUCTION TRAP" else "Standard UPI VPA",
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isTrap) "Initiates DEBIT from your bank account" else "Valid UPI recipient address",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    upi.warningMessage?.let { warn ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LaserRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, LaserRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = warn,
                                color = LaserRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Breakdown
                    if (upi.payeeAddress != null || upi.amount != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            upi.payeeName?.let {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Payee Name:", fontSize = 12.sp, color = TextMuted)
                                    Text(it, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            upi.payeeAddress?.let {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Recipient VPA:", fontSize = 12.sp, color = TextMuted)
                                    Text(it, fontSize = 12.sp, color = ElectricViolet, fontWeight = FontWeight.Bold)
                                }
                            }
                            upi.amount?.let {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Amount to Deduct:", fontSize = 12.sp, color = TextMuted)
                                    Text("₹$it", fontSize = 13.sp, color = LaserRed, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    Text(
                        text = upi.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
