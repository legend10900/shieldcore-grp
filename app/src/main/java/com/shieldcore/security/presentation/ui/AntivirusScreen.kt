package com.shieldcore.security.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldcore.security.domain.model.ScanResult
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.AntivirusUiEvent
import com.shieldcore.security.presentation.viewmodel.AntivirusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntivirusScreen(
    viewModel: AntivirusViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("shieldcore_security_prefs", android.content.Context.MODE_PRIVATE) }
    var apiKeyText by remember { mutableStateOf(prefs.getString("virustotal_api_key", "") ?: "") }

    val hasThreats = uiState.detectedThreats.isNotEmpty()
    val statusColor = if (uiState.isScanning) NeonCyan else if (hasThreats) LaserRed else MatrixGreen

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = SkyBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Past Scan Reports (${uiState.scanHistory.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                }
            },
            text = {
                if (uiState.scanHistory.isEmpty()) {
                    Text("No past scan reports recorded yet.", color = TextMuted)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.scanHistory, key = { it.id ?: it.endTime }) { report ->
                            var isExpanded by remember { mutableStateOf(false) }
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
                            val dateStr = sdf.format(java.util.Date(report.endTime))
                            val durationSec = String.format(java.util.Locale.US, "%.1fs", (report.endTime - report.startTime) / 1000f)

                            val cardBorderColor = if (report.threatsFound > 0) LaserRed else MatrixGreen

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                backgroundColor = DarkCardSurface,
                                borderBrush = Brush.linearGradient(
                                    listOf(cardBorderColor.copy(alpha = 0.5f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (report.threatsFound > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (report.threatsFound > 0) LaserRed else MatrixGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    NeonBadge(
                                        text = if (report.threatsFound > 0) "${report.threatsFound} Threat(s)" else "Clean",
                                        accentColor = if (report.threatsFound > 0) LaserRed else MatrixGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Scanned ${report.totalFilesScanned} apps • Duration: $durationSec",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                if (isExpanded && report.detectedThreats.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = DarkCardBorder)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Detected Threats:", fontSize = 12.sp, color = LaserRed, fontWeight = FontWeight.Bold)
                                    report.detectedThreats.forEach { threat ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(threat.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(threat.packageName ?: threat.filePath, fontSize = 10.sp, color = TextMuted)
                                            }
                                            Button(
                                                onClick = { threat.packageName?.let { viewModel.removeThreat(it) } },
                                                colors = ButtonDefaults.buttonColors(containerColor = LaserRed),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Uninstall", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VirusTotal Cloud API", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        "Enter your VirusTotal API Key to enable live cloud multi-engine scanning across 70+ antivirus databases for every file.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("VirusTotal API Key", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putString("virustotal_api_key", apiKeyText.trim()).apply()
                        showApiKeyDialog = false
                        android.widget.Toast.makeText(context, "VirusTotal API Key Saved", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Key", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                        Text(" • Antivirus", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
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
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Past Scans", tint = SkyBlue)
                    }
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.VpnKey, contentDescription = "VirusTotal API Key", tint = RadiantAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Futuristic Radar Hero
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                backgroundColor = DarkCardSurface,
                borderBrush = Brush.linearGradient(
                    listOf(statusColor.copy(alpha = 0.6f), Color(0x20818CF8))
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Sweeping Radar Beam
                    if (uiState.isScanning) {
                        val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "RadarAngle"
                        )
                        val pulseWave by infiniteTransition.animateFloat(
                            initialValue = 0.7f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "PulseWave"
                        )

                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .graphicsLayer {
                                    scaleX = pulseWave
                                    scaleY = pulseWave
                                }
                                .background(NeonCyan.copy(alpha = 0.06f), CircleShape)
                                .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)), CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .graphicsLayer {
                                    rotationZ = angle
                                }
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(Color.Transparent, NeonCyan.copy(alpha = 0.35f), NeonCyan)
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(statusColor.copy(alpha = 0.15f), CircleShape)
                                .border(BorderStroke(2.dp, statusColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasThreats) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = "Security Shield",
                                tint = statusColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (uiState.isScanning) "Deep Scanning APK Signatures..."
                            else if (hasThreats) "${uiState.detectedThreats.size} Threat(s) Detected!"
                            else "All Systems Protected",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (uiState.isScanning) {
                            Text(
                                text = "${uiState.scannedCount} / ${uiState.totalCount} apps inspected",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.currentAppName.isNotBlank()) {
                                Text(
                                    text = uiState.currentAppName,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Text(
                                text = if (hasThreats) "Immediate quarantine or removal required" else "Engine: Native C++ & VirusTotal Cloud",
                                color = if (hasThreats) LaserRed else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time & Cloud Engine Status Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = DarkCardSurface,
                    borderBrush = Brush.linearGradient(listOf(MatrixGreen.copy(alpha = 0.4f), Color.Transparent))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MatrixGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Real-Time Shield", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Active Guard", fontSize = 10.sp, color = MatrixGreen)
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showApiKeyDialog = true },
                    backgroundColor = DarkCardSurface,
                    borderBrush = Brush.linearGradient(
                        listOf(
                            (if (apiKeyText.isNotBlank()) RadiantAmber else TextMuted).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (apiKeyText.isNotBlank()) RadiantAmber else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("VirusTotal Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(if (apiKeyText.isNotBlank()) "Key Configured" else "Tap to Add Key", fontSize = 10.sp, color = if (apiKeyText.isNotBlank()) RadiantAmber else TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Run Scan Button
            GlowGradientButton(
                text = if (uiState.isScanning) "Scanning Installed Packages..." else "Run Whole Device Scan",
                onClick = { viewModel.onEvent(AntivirusUiEvent.StartScan) },
                icon = if (uiState.isScanning) null else Icons.Default.Radar,
                gradient = if (hasThreats) DangerRedGradient else PrimaryGradient,
                enabled = !uiState.isScanning,
                isLoading = uiState.isScanning
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Threat List Section Header
            SectionHeader(
                title = "Detected Threats (${uiState.detectedThreats.size})",
                accentColor = if (hasThreats) LaserRed else MatrixGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.detectedThreats.isEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkCardSurface,
                    borderBrush = Brush.linearGradient(listOf(MatrixGreen.copy(alpha = 0.3f), Color.Transparent))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No malware or spyware detected", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("Device signatures match safe baseline databases.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.detectedThreats, key = { it.id }) { threat ->
                        ThreatItemCard(
                            threat = threat,
                            onUninstall = { threat.packageName?.let { pkg -> viewModel.removeThreat(pkg) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatItemCard(
    threat: ScanResult,
    onUninstall: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkCardSurface,
        borderBrush = Brush.linearGradient(listOf(LaserRed.copy(alpha = 0.6f), Color.Transparent))
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = LaserRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(threat.label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                NeonBadge(
                    text = threat.riskLevel.name,
                    accentColor = LaserRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Package: ${threat.packageName ?: threat.filePath}", color = TextMuted, fontSize = 11.sp)
            Text("Threat Pattern: ${threat.threatName ?: "Suspicious Binary"}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (!threat.hash.isNullOrBlank()) {
                Text("SHA-256: ${threat.hash}", color = SkyBlue, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onUninstall,
                colors = ButtonDefaults.buttonColors(containerColor = LaserRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Uninstall / Remove", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
