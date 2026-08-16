package com.shieldcore.security.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shieldcore.security.presentation.ui.screens.*

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val bottomNavItems = listOf(
        Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
        Triple("scanner", "Antivirus", Icons.Default.Security),
        Triple("cleaner", "Cleaner", Icons.Default.CleaningServices),
        Triple("network", "Network", Icons.Default.Wifi),
        Triple("applock", "App Lock", Icons.Default.Lock)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { (route, label, icon) ->
                    val isSelected = currentRoute == route
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("scanner") { AntivirusScreen() }
            composable("cleaner") { CleanerScreen() }
            composable("network") { NetworkScreen() }
            composable("applock") { AppLockScreen() }
            composable("breach") { BreachScreen() }
            composable("phishing") { PhishingScreen() }
            composable("battery") { BatteryScreen() }
        }
    }
}
