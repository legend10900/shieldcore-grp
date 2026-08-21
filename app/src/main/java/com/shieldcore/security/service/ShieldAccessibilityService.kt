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

    private var lastActiveLockedPkg: String? = null
    private var lastCheckedPkg: String? = null
    private var lastCheckTime: Long = 0L
    private var lastAlertTime: Long = 0L
    private var lastAlertedContent: String? = null

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sec.android.inputmethod",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                checkAppLock(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                inspectScreenContent()
            }
        }
    }

    private fun checkAppLock(packageName: String) {
        if (packageName == this.packageName) return

        // When switching away from a previously active locked app, immediately clear its session unlock!
        val prev = lastActiveLockedPkg
        if (prev != null && prev != packageName && !ignoredPackages.contains(packageName)) {
            appLockRepository.clearSessionUnlock(prev)
            lastActiveLockedPkg = null
        }

        if (ignoredPackages.contains(packageName)) return

        val now = System.currentTimeMillis()
        if (packageName == lastCheckedPkg && now - lastCheckTime < 500) return
        lastCheckedPkg = packageName
        lastCheckTime = now

        serviceScope.launch {
            if (appLockRepository.isAppLocked(packageName)) {
                if (!appLockRepository.isSessionUnlocked(packageName)) {
                    Log.i("ShieldAccessibility", "Triggering AppLock for: $packageName")
                    val lockIntent = Intent(applicationContext, com.shieldcore.security.presentation.ui.AppLockActivity::class.java).apply {
                        putExtra("target_package", packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    }
                    startActivity(lockIntent)
                } else {
                    lastActiveLockedPkg = packageName
                }
            }
        }
    }

    private fun inspectScreenContent() {
        val rootNode = rootInActiveWindow ?: return
        val textNodes = findTextNodesRecursively(rootNode)

        textNodes.forEach { node ->
            val text = node.text?.toString() ?: return@forEach
            if (text.length < 10) return@forEach

            val now = System.currentTimeMillis()
            if (text == lastAlertedContent && now - lastAlertTime < 5000) return@forEach

            // Check if text has URLs or contains high-risk scam triggers
            if (text.contains("http://") || text.contains("https://") || text.contains("upi://pay") || text.contains("disconnected tonight") || text.contains("held due to")) {
                serviceScope.launch(Dispatchers.IO) {
                    val report = phishingRepository.analyzeMessage(text)
                    if (report.isScam) {
                        lastAlertTime = now
                        lastAlertedContent = text
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                applicationContext,
                                "⚠️ ShieldCore Warning: ${report.category.displayName} detected on screen!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun findTextNodesRecursively(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        if (!node.text.isNullOrBlank()) {
            found.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { found.addAll(findTextNodesRecursively(it)) }
        }
        return found
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
