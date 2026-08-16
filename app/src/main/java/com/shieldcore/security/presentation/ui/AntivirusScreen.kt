package com.shieldcore.security.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldcore.security.domain.model.ScanResult
import com.shieldcore.security.presentation.viewmodel.AntivirusViewModel
import com.shieldcore.security.presentation.viewmodel.AntivirusUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntivirusScreen(viewModel: AntivirusViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("shieldcore_security_prefs", android.content.Context.MODE_PRIVATE) }
    var apiKeyText by remember { mutableStateOf(prefs.getString("virustotal_api_key", "") ?: "") }

    val darkBackground = Color(0xFF0F172A)
    val accentGreen = Color(0xFF10B981)
    val threatRed = Color(0xFFEF4444)
    val cardBackground = Color(0xFF1E293B)

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF60A5FA))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Past Scan Reports (${uiState.scanHistory.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                if (uiState.scanHistory.isEmpty()) {
                    Text("No past scan reports recorded yet.", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.scanHistory) { report ->
                            var isExpanded by remember { mutableStateOf(false) }
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault())
                            val dateStr = sdf.format(java.util.Date(report.endTime))
                            val durationSec = String.format(java.util.Locale.US, "%.1fs", (report.endTime - report.startTime) / 1000f)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (report.threatsFound > 0)
                                        Color(0xFF3B1E22)
                                    else
                                        Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (report.threatsFound > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (report.threatsFound > 0) threatRed else accentGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            if (report.threatsFound > 0) "${report.threatsFound} Threat(s)" else "Clean",
                                            color = if (report.threatsFound > 0) threatRed else accentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Scanned ${report.totalFilesScanned} apps • Duration: $durationSec",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )

                                    if (isExpanded && report.detectedThreats.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = Color.DarkGray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Threats in this scan:", fontSize = 11.sp, color = threatRed, fontWeight = FontWeight.Bold)
                                        report.detectedThreats.forEach { threat ->
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(threat.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(threat.packageName ?: threat.filePath, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Button(
                                                    onClick = {
                                                        threat.packageName?.let { viewModel.removeThreat(it) }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = threatRed),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Uninstall", fontSize = 10.sp)
                                                }
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
                    Text("Close")
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("VirusTotal Cloud API", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter your VirusTotal API Key to enable live cloud multi-engine scanning across 70+ antivirus databases for every file.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("VirusTotal API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putString("virustotal_api_key", apiKeyText.trim()).apply()
                    showApiKeyDialog = false
                    android.widget.Toast.makeText(context, "VirusTotal API Key Saved", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShieldCore • Antivirus", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "Past Scans", tint = Color(0xFF60A5FA))
                    }
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.VpnKey, contentDescription = "VirusTotal API Key", tint = Color(0xFF60A5FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scanner Radar / Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isScanning) {
                    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Rotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .rotate(angle)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(accentGreen.copy(alpha = 0.1f), accentGreen)
                                ),
                                shape = CircleShape
                            )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (uiState.detectedThreats.isNotEmpty()) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = if (uiState.detectedThreats.isNotEmpty()) threatRed else accentGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.isScanning) "Scanning Installed Packages..." else if (uiState.detectedThreats.isNotEmpty()) "Threats Detected!" else "System Protected",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isScanning) {
                        Text(
                            text = "${uiState.scannedCount} / ${uiState.totalCount} • ${uiState.currentAppName}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time Protection Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Real-Time Protection Active", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (apiKeyText.isNotBlank()) "VirusTotal Cloud & Native JNI Monitoring" else "Native JNI Engine (Tap key icon for VirusTotal)",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = { viewModel.onEvent(AntivirusUiEvent.StartScan) },
                enabled = !uiState.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = if (uiState.isScanning) "Scanning Device..." else "Run Whole Device Scan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Results / Threat List Section
            Text(
                text = "Detected Threats (${uiState.detectedThreats.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.detectedThreats.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No active malware threats found.", color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.detectedThreats) { threat ->
                        ThreatItemCard(
                            threat = threat,
                            cardBackground = cardBackground,
                            threatRed = threatRed,
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
    cardBackground: Color,
    threatRed: Color,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = threatRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(threat.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = threat.riskLevel.name,
                    color = threatRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Package: ${threat.packageName}", color = Color.Gray, fontSize = 12.sp)
            Text("Threat Rule: ${threat.threatName ?: "Unknown"}", color = Color.LightGray, fontSize = 13.sp)
            Text("Hash: ${threat.hash ?: "N/A"}", color = Color(0xFF60A5FA), fontSize = 11.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUninstall,
                colors = ButtonDefaults.buttonColors(containerColor = threatRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Uninstall / Remove")
            }
        }
    }
}
