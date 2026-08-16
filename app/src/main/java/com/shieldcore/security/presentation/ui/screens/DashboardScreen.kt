package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shieldcore.security.presentation.viewmodel.AntivirusViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    antivirusViewModel: AntivirusViewModel = hiltViewModel()
) {
    val uiState by antivirusViewModel.uiState.collectAsState()
    val lastScan = uiState.scanHistory.firstOrNull() ?: uiState.lastScanSummary

    val lastScanText = if (lastScan != null) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        "Last scanned: ${sdf.format(Date(lastScan.endTime))}"
    } else {
        "No full scan recorded yet"
    }

    val threatsFound = lastScan?.threatsFound ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("ShieldCore Security", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Active On-Device Threat Protection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))

        // Security Status Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (threatsFound > 0)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (threatsFound > 0) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (threatsFound > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (threatsFound > 0) "Malware Threat Alert" else "System Protected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (threatsFound > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (threatsFound > 0) "$threatsFound threat(s) detected!" else lastScanText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (threatsFound > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Security Toolkit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { DashboardToolkitCard("Anti-Virus Scan", "Radar & JNI Engine", Icons.Default.Security, { navController.navigate("scanner") }) }
            item { DashboardToolkitCard("Junk Cleaner", "Storage Optimizer", Icons.Default.CleaningServices, { navController.navigate("cleaner") }) }
            item { DashboardToolkitCard("Wi-Fi & LAN Audit", "Subnet & ARP Probe", Icons.Default.Wifi, { navController.navigate("network") }) }
            item { DashboardToolkitCard("App Locker", "Biometric Lock", Icons.Default.Lock, { navController.navigate("applock") }) }
            item { DashboardToolkitCard("Data Breach", "k-Anonymity Leak Check", Icons.Default.LockReset, { navController.navigate("breach") }) }
            item { DashboardToolkitCard("Web Shield", "DNS & Phishing Filter", Icons.Default.Language, { navController.navigate("phishing") }) }
            item { DashboardToolkitCard("Battery Optimizer", "Live Telemetry", Icons.Default.Bolt, { navController.navigate("battery") }) }
        }
    }
}

@Composable
fun DashboardToolkitCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

