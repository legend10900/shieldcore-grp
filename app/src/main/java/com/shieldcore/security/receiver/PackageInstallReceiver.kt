package com.shieldcore.security.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.shieldcore.security.domain.repository.AntivirusRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageInstallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var antivirusRepository: AntivirusRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            
            // Trigger asynchronous background inspection
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val threat = antivirusRepository.scanSinglePackage(packageName)
                    if (threat != null) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "THREAT DETECTED in installed app: ${threat.appName} (${threat.threatName})",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
