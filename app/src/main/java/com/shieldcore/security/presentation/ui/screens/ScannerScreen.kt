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
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.presentation.viewmodel.ScannerViewModel

@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val progress by viewModel.scanProgress.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Anti-Virus Scanner", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = progress) {
            is ScanProgress.Idle -> {
                Button(onClick = { viewModel.startFullScan() }) {
                    Text("Start Full Device Scan")
                }
            }
            is ScanProgress.Running -> {
                CircularProgressIndicator(progress = { state.scannedCount.toFloat() / state.totalCount })
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scanning: ${state.currentApp}")
                Text("${state.scannedCount} / ${state.totalCount} apps checked")
                if (state.threatsFound.isNotEmpty()) {
                    Text("Threats detected: ${state.threatsFound.size}", color = MaterialTheme.colorScheme.error)
                }
            }
            is ScanProgress.Completed -> {
                Text("Scan Completed", style = MaterialTheme.typography.titleLarge)
                Text("Total scanned: ${state.summary.totalFilesScanned}")
                Text("Threats found: ${state.summary.threatsFound}")
                Button(onClick = { viewModel.startFullScan() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Scan Again")
                }
            }
            is ScanProgress.Error -> {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
