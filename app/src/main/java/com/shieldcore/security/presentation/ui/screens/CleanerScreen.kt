package com.shieldcore.security.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.core.utils.FileUtils
import com.shieldcore.security.domain.repository.JunkScanProgress
import com.shieldcore.security.presentation.viewmodel.CleanerViewModel

@Composable
fun CleanerScreen(viewModel: CleanerViewModel = hiltViewModel()) {
    val progress by viewModel.scanProgress.collectAsState()
    val summary by viewModel.cleanSummary.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Junk Cleaner", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = progress) {
            is JunkScanProgress.Idle -> {
                Button(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan for Junk")
                }
            }
            is JunkScanProgress.Scanning -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Scanning: ${state.currentDir}", modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Found: ${FileUtils.formatSize(state.currentSize)}", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is JunkScanProgress.Completed -> {
                Text("Found ${state.items.size} items (${FileUtils.formatSize(state.items.sumOf { it.sizeBytes })})")
                
                Button(
                    onClick = { viewModel.cleanSelected(state.items) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clean All")
                }

                LazyColumn {
                    items(state.items) { item ->
                        ListItem(
                            headlineContent = { Text(item.label) },
                            supportingContent = { Text(item.type.name) },
                            trailingContent = { Text(FileUtils.formatSize(item.sizeBytes)) }
                        )
                    }
                }
            }
        }

        summary?.let { 
            AlertDialog(
                onDismissRequest = { /* Handle */ },
                title = { Text("Cleaning Complete") },
                text = { Text("Cleaned ${FileUtils.formatSize(it.totalSizeCleaned)} and removed ${it.itemsRemoved} files.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.startScan() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
