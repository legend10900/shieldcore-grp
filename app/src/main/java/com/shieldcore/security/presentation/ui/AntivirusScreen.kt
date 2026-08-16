package com.shieldcore.security.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    val prefs = remember { context.getSharedPreferences("shieldcore_security_prefs", android.content.Context.MODE_PRIVATE) }
    var apiKeyText by remember { mutableStateOf(prefs.getString("virustotal_api_key", "") ?: "") }

    val darkBackground = Color(0xFF0F172A)
    val accentGreen = Color(0xFF10B981)
    val threatRed = Color(0xFFEF4444)
    val cardBackground = Color(0xFF1E293B)

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
                title = { Text("ShieldCore • Antivirus Engine", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
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
