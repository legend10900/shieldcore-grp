package com.shieldcore.security.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shieldcore.security.domain.repository.PhishingRepository
import com.shieldcore.security.domain.repository.AppLockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ShieldAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    @Inject
    lateinit var appLockRepository: AppLockRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                checkAppLock(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                inspectUrlBar()
            }
        }
    }

    private fun checkAppLock(packageName: String) {
        if (packageName == this.packageName || packageName == "com.android.systemui") return

        serviceScope.launch {
            if (appLockRepository.isAppLocked(packageName) && !appLockRepository.isSessionUnlocked(packageName)) {
                Log.i("ShieldAccessibility", "Locking app: $packageName")
                val lockIntent = Intent(applicationContext, com.shieldcore.security.presentation.ui.AppLockActivity::class.java).apply {
                    putExtra("target_package", packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                startActivity(lockIntent)
            }
        }
    }

    private fun inspectUrlBar() {
        val rootNode = rootInActiveWindow ?: return
        val urlNodes = findUrlNodesRecursively(rootNode)

        urlNodes.forEach { node ->
            node.text?.toString()?.let { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    serviceScope.launch {
                        val result = phishingRepository.checkUrl(url)
                        if (result.isMalicious) {
                            Log.w("ShieldAccessibility", "Malicious URL detected: $url")
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "⚠️ ShieldCore Security Warning: Malicious Phishing link detected ($url)",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findUrlNodesRecursively(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        if (node.className?.contains("EditText", ignoreCase = true) == true || 
            node.contentDescription?.contains("url", ignoreCase = true) == true) {
            found.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { found.addAll(findUrlNodesRecursively(it)) }
        }
        return found
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
