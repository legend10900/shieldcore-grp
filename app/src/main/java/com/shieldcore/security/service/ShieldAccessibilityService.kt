package com.shieldcore.security.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ShieldAccessibilityService : AccessibilityService() {

    enum class CleanerState {
        IDLE,
        OPENING_STORAGE,
        CLICKING_CLEAR_CACHE,
        COMPLETED
    }

    private var currentCleanerState = CleanerState.IDLE
    private val safetyBlacklistStrings = setOf("Clear data", "Clear storage", "Delete data", "Storage wipe")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                // 1. Phishing URL Inspection for Mobile Browsers
                if (isBrowserPackage(packageName)) {
                    inspectBrowserAddressBar(rootInActiveWindow)
                }

                // 2. Automated Cache Cleaning State Machine inside Settings App
                if (packageName == "com.android.settings" && currentCleanerState != CleanerState.IDLE) {
                    processCacheCleanerStep(rootInActiveWindow)
                }
            }
        }
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        return pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("browser") || pkg.contains("opera")
    }

    private fun inspectBrowserAddressBar(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        val urlNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
            ?: rootNode.findAccessibilityNodeInfosByText("http")

        for (node in urlNodes) {
            val text = node.text?.toString() ?: continue
            if (text.startsWith("http://") || text.startsWith("https://")) {
                Log.i("AccessibilityURLCheck", "Inspected active browser URL: $text")
                if (isPhishingUrl(text)) {
                    triggerSecurityOverlayWarning(text)
                }
            }
        }
    }

    private fun isPhishingUrl(url: String): Boolean {
        return url.contains("phishing") || url.contains("fake-login") || url.contains("credential-harvest")
    }

    private fun triggerSecurityOverlayWarning(url: String) {
        Log.w("AccessibilityShield", "CRITICAL WARNING: Phishing link identified! $url")
    }

    private fun processCacheCleanerStep(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return

        when (currentCleanerState) {
            CleanerState.OPENING_STORAGE -> {
                val storageNodes = rootNode.findAccessibilityNodeInfosByText("Storage")
                for (node in storageNodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        currentCleanerState = CleanerState.CLICKING_CLEAR_CACHE
                        return
                    }
                }
            }
            CleanerState.CLICKING_CLEAR_CACHE -> {
                // STRICT SAFETY CHECK: Ensure we NEVER touch "Clear Data"
                for (forbidden in safetyBlacklistStrings) {
                    val dangerousNodes = rootNode.findAccessibilityNodeInfosByText(forbidden)
                    if (dangerousNodes.isNotEmpty()) {
                        Log.i("CacheCleanerSafety", "Encountered sensitive storage node. Bypassing data wipe buttons.")
                    }
                }

                val cacheNodes = rootNode.findAccessibilityNodeInfosByText("Clear cache")
                for (node in cacheNodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        currentCleanerState = CleanerState.COMPLETED
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        return
                    }
                }
            }
            else -> {}
        }
    }

    override fun onInterrupt() {
        currentCleanerState = CleanerState.IDLE
    }
}
