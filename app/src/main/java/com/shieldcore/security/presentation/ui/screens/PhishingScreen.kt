package com.shieldcore.security.presentation.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.presentation.viewmodel.PhishingViewModel
import com.shieldcore.security.service.PhishingVpnService

@Composable
fun PhishingScreen(viewModel: PhishingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val startIntent = Intent(context, PhishingVpnService::class.java).apply {
                action = PhishingVpnService.ACTION_START
            }
            context.startService(startIntent)
            viewModel.setVpnActive(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Web & Phishing Shield", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Real-time DNS inspection and heuristic protection against malicious links, credential harvesters, and fake websites.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Web Shield VPN Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isVpnActive)
                    Color(0xFFE8F5E9)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.isVpnActive) Icons.Default.Shield else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (uiState.isVpnActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (uiState.isVpnActive) "DNS Web Shield Active" else "DNS Web Shield Disabled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (uiState.isVpnActive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (uiState.isVpnActive) "Blocking phishing domains locally." else "Enable local VPN filter for live protection.",
                        fontSize = 12.sp,
                        color = if (uiState.isVpnActive) Color(0xFF2E7D32) else Color.Gray
                    )
                }
                Switch(
                    checked = uiState.isVpnActive,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val vpnIntent = VpnService.prepare(context)
                            if (vpnIntent != null) {
                                vpnLauncher.launch(vpnIntent)
                            } else {
                                val startIntent = Intent(context, PhishingVpnService::class.java).apply {
                                    action = PhishingVpnService.ACTION_START
                                }
                                context.startService(startIntent)
                                viewModel.setVpnActive(true)
                            }
                        } else {
                            val stopIntent = Intent(context, PhishingVpnService::class.java).apply {
                                action = PhishingVpnService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                            viewModel.setVpnActive(false)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // URL Inspector Card
        Text("Link Safety Analyzer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = uiState.testUrl,
                    onValueChange = { viewModel.onUrlInputChanged(it) },
                    label = { Text("Enter or paste URL (e.g. login-verify.xyz)") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.testUrlSafety() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing Link...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze URL")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        uiState.checkResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isMalicious)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else
                        Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.isMalicious) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (result.isMalicious) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (result.isMalicious) "⚠️ Dangerous / Phishing Link" else "✅ Safe Website",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (result.isMalicious) MaterialTheme.colorScheme.error else Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("URL: ${result.url}", fontSize = 13.sp)
                    result.threatType?.let {
                        Text("Threat Category: $it", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    result.detectionSource?.let {
                        Text("Engine: $it", fontSize = 12.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.markCurrentUrl(LinkSafetyStatus.SAFE) }) {
                            Text("Trust / Whitelist")
                        }
                        Button(
                            onClick = { viewModel.markCurrentUrl(LinkSafetyStatus.PHISHING) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Block Domain")
                        }
                    }
                }
            }
        }
    }
}
