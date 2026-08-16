package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.presentation.viewmodel.NetworkViewModel

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = hiltViewModel()) {
    val wifiDetails by viewModel.wifiDetails.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val dnsIntact by viewModel.dnsIntact.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Wi-Fi & Network Security", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Scan active connected subnet devices, inspect ARP MAC tables, open ports, and DNS integrity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Wi-Fi Connection Card
        wifiDetails?.let { wifi ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(wifi.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${wifi.signalStrength} dBm",
                            fontSize = 12.sp,
                            color = if (wifi.signalStrength > -65) Color(0xFF2E7D32) else Color(0xFFF57F17),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BSSID: ${wifi.bssid}", fontSize = 12.sp, color = Color.Gray)
                    Text("Security: ${wifi.securityProtocol}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Frequency: ${wifi.frequency} MHz", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (dnsIntact == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (dnsIntact == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (dnsIntact == true) "DNS Server Integrity: Protected" else "DNS Integrity: Check recommended",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dnsIntact == true) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } ?: Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Not connected to Wi-Fi. (Cellular / Offline)", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.startNetworkScan() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Radar, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Connected Subnet Devices")
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = scanProgress) {
            is NetworkScanProgress.Discovery -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { state.percentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Probing subnet hosts in parallel... ${state.percentage}%", fontSize = 12.sp, color = Color.Gray)
                }
            }
            is NetworkScanProgress.Completed -> {
                Text(
                    "Connected Devices (${state.devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.devices.isEmpty()) {
                    Text("No reachable LAN hosts identified.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.devices) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.isVulnerable)
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (device.hostname?.contains("This Device") == true)
                                            Icons.Default.Smartphone
                                        else if (device.ipAddress.endsWith(".1"))
                                            Icons.Default.Router
                                        else
                                            Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(device.ipAddress, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            device.hostname?.let {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("($it)", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        Text("MAC: ${device.macAddress ?: "Unknown"}", fontSize = 11.sp, color = Color.Gray)
                                        if (device.openPorts.isNotEmpty()) {
                                            Text("Open Ports: ${device.openPorts.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

