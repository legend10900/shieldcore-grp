package com.shieldcore.security.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shieldcore.security.domain.repository.ScannerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageInstallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scannerRepository: ScannerRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.encodedSchemeSpecificPart ?: return
        
        if (intent.action == Intent.ACTION_PACKAGE_ADDED || intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            receiverScope.launch {
                val result = scannerRepository.scanPackage(packageName)
                if (result.riskLevel == com.shieldcore.security.domain.model.RiskLevel.MALICIOUS) {
                    Log.e("ShieldCore", "Threat detected in newly installed app: $packageName")
                    // Show warning notification
                }
            }
        }
    }
}
