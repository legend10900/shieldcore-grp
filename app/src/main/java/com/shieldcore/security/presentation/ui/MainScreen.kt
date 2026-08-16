package com.shieldcore.security.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shieldcore.security.presentation.ui.screens.*

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Dashboard", "Scanner", "Network", "Battery", "Cleaner")
    val icons = listOf(Icons.Default.Dashboard, Icons.Default.Security, Icons.Default.Wifi, Icons.Default.BatteryChargingFull, Icons.Default.Delete)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item.lowercase())
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(innerPadding)) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("scanner") { ScannerScreen() }
            composable("network") { NetworkScreen() }
            composable("battery") { BatteryScreen() }
            composable("cleaner") { CleanerScreen() }
        }
    }
}
