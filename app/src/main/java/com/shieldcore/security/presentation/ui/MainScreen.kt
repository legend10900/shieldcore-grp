package com.shieldcore.security.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shieldcore.security.presentation.ui.screens.*
import com.shieldcore.security.presentation.ui.theme.*

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val bottomNavItems = listOf(
        NavItem("dashboard", "Home", Icons.Default.Dashboard, NeonCyan),
        NavItem("scanner", "Antivirus", Icons.Default.Security, MatrixGreen),
        NavItem("cleaner", "Cleaner", Icons.Default.CleaningServices, RadiantAmber),
        NavItem("network", "Network", Icons.Default.Wifi, SkyBlue),
        NavItem("applock", "Locker", Icons.Default.Lock, ElectricViolet)
    )

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = DarkGlassSurface,
                border = BorderStroke(1.dp, GlassBorderGradient),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val animatedColor by animateColorAsState(
                            targetValue = if (isSelected) item.accentColor else TextMuted,
                            animationSpec = tween(300),
                            label = "NavColorAnim"
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) item.accentColor.copy(alpha = 0.16f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = animatedColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                AnimatedVisibility(visible = isSelected) {
                                    Row {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            color = animatedColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            composable("dashboard") { DashboardScreen(navController) }
            composable("scanner") { AntivirusScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("cleaner") { CleanerScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("network") { NetworkScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("applock") { AppLockScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("breach") { BreachScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("phishing") { PhishingScreen(onNavigateBack = { navController.popBackStack() }) }
            composable("battery") { BatteryScreen(onNavigateBack = { navController.popBackStack() }) }
        }
    }
}
