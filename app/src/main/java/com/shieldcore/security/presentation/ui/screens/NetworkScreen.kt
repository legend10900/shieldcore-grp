package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.presentation.viewmodel.NetworkViewModel

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = hiltViewModel()) {
    val wifiDetails by viewModel.wifiDetails.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Network Security", style = MaterialTheme.typography.headlineMedium)
        
        wifiDetails?.let { wifi ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connected to: ${wifi.ssid}", style = MaterialTheme.typography.titleMedium)
                    Text("BSSID: ${wifi.bssid}")
                    Text("Security: ${wifi.securityProtocol}")
                    Text("Signal Strength: ${wifi.signalStrength} dBm")
                }
            }
        } ?: Text("Not connected to Wi-Fi", modifier = Modifier.padding(vertical = 8.dp))

        Button(
            onClick = { viewModel.startNetworkScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Local Network")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = scanProgress) {
            is NetworkScanProgress.Discovery -> {
                LinearProgressIndicator(
                    progress = { state.percentage / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Scanning subnet... ${state.percentage}%")
            }
            is NetworkScanProgress.Completed -> {
                Text("Devices Found: ${state.devices.size}", style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(state.devices) { device ->
                        ListItem(
                            headlineContent = { Text(device.ipAddress) },
                            supportingContent = { Text("MAC: ${device.macAddress ?: "Unknown"}") }
                        )
                    }
                }
            }
            else -> {}
        }
    }
}
