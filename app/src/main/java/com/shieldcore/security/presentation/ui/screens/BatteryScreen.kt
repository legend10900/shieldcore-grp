package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.presentation.viewmodel.BatteryViewModel

@Composable
fun BatteryScreen(viewModel: BatteryViewModel = hiltViewModel()) {
    val batteryInfo by viewModel.batteryInfo.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Battery Optimizer", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        batteryInfo?.let { info ->
            Text("${info.level}%", style = MaterialTheme.typography.displayLarge)
            Text(if (info.isCharging) "Charging" else "Discharging")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Temperature: ${info.temperature}°C")
                    Text("Voltage: ${info.voltage / 1000f}V")
                    Text("Health: ${getHealthString(info.health)}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (info.temperature > 38.0) {
                Text("Warning: Device is heating up!", color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { viewModel.optimize() }) {
                Text("Optimize Now")
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
        else -> "Unknown"
    }
}
