package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.ScannerViewModel

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val progress by viewModel.scanProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Anti-Virus Scanner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(24.dp))

        when (val state = progress) {
            is ScanProgress.Idle -> {
                AnimatedPulseOrb(
                    accentColor = NeonCyan,
                    icon = Icons.Default.Radar,
                    size = 120.dp,
                    iconSize = 56.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Ready to scan device APKs and signatures", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                GlowGradientButton(
                    text = "Start Full Device Scan",
                    onClick = { viewModel.startFullScan() },
                    icon = Icons.Default.Radar
                )
            }
            is ScanProgress.Running -> {
                val progressFraction = state.scannedCount.toFloat() / state.totalCount.coerceAtLeast(1)
                CircularProgressIndicator(
                    progress = { progressFraction },
                    color = NeonCyan,
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("Scanning: ${state.currentApp}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("${state.scannedCount} / ${state.totalCount} apps inspected", color = TextSecondary, fontSize = 13.sp)
                if (state.threatsFound.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Threats detected: ${state.threatsFound.size}", color = LaserRed, fontWeight = FontWeight.Bold)
                }
            }
            is ScanProgress.Completed -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scan Completed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total scanned: ${state.summary.totalFilesScanned}", color = TextSecondary)
                Text("Threats found: ${state.summary.threatsFound}", color = if (state.summary.threatsFound > 0) LaserRed else MatrixGreen, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                GlowGradientButton(
                    text = "Scan Again",
                    onClick = { viewModel.startFullScan() }
                )
            }
            is ScanProgress.Error -> {
                Icon(Icons.Default.Warning, contentDescription = null, tint = LaserRed, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error: ${state.message}", color = LaserRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
