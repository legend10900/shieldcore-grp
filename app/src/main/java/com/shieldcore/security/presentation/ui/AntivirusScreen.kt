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

    val darkBackground = Color(0xFF0F172A)
    val accentGreen = Color(0xFF10B981)
    val threatRed = Color(0xFFEF4444)
    val cardBackground = Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShieldCore • Antivirus Engine", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    .height(240.dp)
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
                            .size(140.dp)
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
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isScanning) "Scanning Installed Packages..." else if (uiState.detectedThreats.isNotEmpty()) "Threats Detected!" else "System Protected",
                        color = Color.White,
                        fontSize = 18.sp,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = { viewModel.onEvent(AntivirusUiEvent.StartScan) },
                enabled = !uiState.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = if (uiState.isScanning) "Scanning Device..." else "Run Whole Device Scan",
                    fontSize = 16.sp,
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
