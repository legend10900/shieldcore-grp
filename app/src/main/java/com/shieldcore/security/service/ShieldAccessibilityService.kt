package com.shieldcore.security.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.shieldcore.security.domain.repository.AppLockRepository
import com.shieldcore.security.domain.repository.PhishingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Collections
import java.util.LinkedHashMap
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
    private var lastContentInspectTime: Long = 0L
    private var lastAlertTime: Long = 0L
    private var lastAlertCategory: String? = null

    // LRU Cache for recently analyzed texts: textHash -> timestamp
    private val analyzedTextCache = Collections.synchronizedMap(
        object : LinkedHashMap<Int, Long>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Long>?): Boolean {
                return size > 100
            }
        }
    )

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sec.android.inputmethod",
        "com.touchtype.swiftkey",
        "com.google.android.inputmethod.korean",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.android.settings",
        "com.google.android.dialer",
        "com.samsung.android.dialer"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName || ignoredPackages.contains(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                checkAppLock(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = System.currentTimeMillis()
                // Throttle screen inspection to at most once per 1500ms
                if (now - lastContentInspectTime >= 1500L) {
                    lastContentInspectTime = now
                    inspectScreenContent(packageName)
                }
            }
        }
    }

    private fun checkAppLock(packageName: String) {
        // When switching away from a previously active locked app, immediately clear its session unlock
        val prev = lastActiveLockedPkg
        if (prev != null && prev != packageName && !ignoredPackages.contains(packageName)) {
            appLockRepository.clearSessionUnlock(prev)
            lastActiveLockedPkg = null
        }

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

    private fun inspectScreenContent(packageName: String) {
        val rootNode = rootInActiveWindow ?: return

        // Extract text elements safely on a background worker to avoid any UI thread jank
        serviceScope.launch(Dispatchers.Default) {
            val extractedTexts = mutableListOf<String>()
            try {
                collectTextNodes(rootNode, extractedTexts, depth = 0, maxNodes = 25)
            } finally {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }

            if (extractedTexts.isEmpty()) return@launch

            val now = System.currentTimeMillis()

            for (text in extractedTexts) {
                if (text.length < 15) continue

                // Check triggers: URLs, UPI links, or suspicious keywords
                val hasLink = text.contains("http://", ignoreCase = true) ||
                              text.contains("https://", ignoreCase = true) ||
                              text.contains("upi://pay", ignoreCase = true) ||
                              text.contains("t.me/", ignoreCase = true) ||
                              text.contains("bit.ly/", ignoreCase = true)

                val hasUrgentThreat = text.contains("disconnected tonight", ignoreCase = true) ||
                                     text.contains("account blocked", ignoreCase = true) ||
                                     text.contains("kyc expired", ignoreCase = true) ||
                                     text.contains("held at facility", ignoreCase = true)

                if (!hasLink && !hasUrgentThreat) continue

                val textHash = text.hashCode()
                val lastSeenTime = analyzedTextCache[textHash]
                // Skip if this exact text was inspected within the last 60 seconds
                if (lastSeenTime != null && (now - lastSeenTime) < 60_000L) {
                    continue
                }
                analyzedTextCache[textHash] = now

                val report = phishingRepository.analyzeMessage(text)
                if (report.isScam) {
                    // Suppress duplicate alert toasts within 10 seconds for the same category
                    if (now - lastAlertTime > 10_000L || lastAlertCategory != report.category.displayName) {
                        lastAlertTime = now
                        lastAlertCategory = report.category.displayName
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "⚠️ ShieldCore Warning: ${report.category.displayName} detected on screen!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    break // Don't process further nodes in this tick once a high-risk scam is detected
                }
            }
        }
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo?, outList: MutableList<String>, depth: Int, maxNodes: Int) {
        if (node == null || depth > 6 || outList.size >= maxNodes) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && text.length >= 10) {
            outList.add(text)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (outList.size >= maxNodes) break
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            }
            if (child != null) {
                collectTextNodes(child, outList, depth + 1, maxNodes)
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
