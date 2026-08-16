package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.shieldcore.security.presentation.viewmodel.BatteryViewModel

@Composable
fun BatteryScreen(viewModel: BatteryViewModel = hiltViewModel()) {
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val cpuApps by viewModel.cpuApps.collectAsState()
    val isOptimized by viewModel.isOptimized.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Battery & Power Optimizer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        batteryInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (info.level > 20)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${info.level}%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (info.isCharging) Icons.Default.Bolt else Icons.Default.BatteryStd,
                            contentDescription = null,
                            tint = if (info.isCharging) Color(0xFF2E7D32) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (info.isCharging) "Charging" else "Discharging",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature", color = Color.Gray)
                        Text(
                            "${info.temperature}°C",
                            fontWeight = FontWeight.Bold,
                            color = if (info.temperature > 40.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voltage", color = Color.Gray)
                        Text("${info.voltage / 1000f} V", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Health Condition", color = Color.Gray)
                        Text(getHealthString(info.health), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cpuApps.isNotEmpty()) {
                Text(
                    "Active Foreground & Background Apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cpuApps) { appName ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(appName, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (info.temperature > 39.0) {
                Text("⚠️ Device is running hot! Cool down recommended.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = { viewModel.optimize() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isOptimized) "Optimized (Memory Cleaned)" else "Optimize Power & Memory")
            }
        }
    }
}

private fun getHealthString(health: Int): String {
    return when (health) {
        2 -> "Good"
        3 -> "Overheat"
        4 -> "Dead"
        5 -> "Over Voltage"
        7 -> "Cold"
        else -> "Good / Normal"
    }
}

