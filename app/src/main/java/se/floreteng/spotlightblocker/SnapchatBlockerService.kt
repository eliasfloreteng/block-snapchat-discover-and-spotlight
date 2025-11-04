package se.floreteng.spotlightblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class SnapchatBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "SnapchatBlockerService"
        private const val DEBOUNCE_DELAY_MS = 2000L // 2 seconds
        private const val SETTINGS_RELOAD_INTERVAL_MS = 5000L // Reload settings every 5 seconds
    }

    // Track last time we performed the back action to prevent multiple triggers
    private var lastActionTime = 0L

    // Track last time we reloaded settings
    private var lastSettingsReloadTime = 0L

    // Blocked apps configuration loaded from settings
    private var blockedApps: List<BlockedApp> = emptyList()
    private var monitoredPackages: Array<String> = emptyArray()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // Load blocked apps from settings
        loadBlockedApps()

        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            // Listen to events from all blocked apps
            packageNames = monitoredPackages

            // Event types to monitor
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED

            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Flags
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            notificationTimeout = 100
        }

        serviceInfo = info

        Toast.makeText(this, "Blocker Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun loadBlockedApps() {
        val newBlockedApps = AppSettings.getBlockedApps(this)
        val newPackages = newBlockedApps.map { it.packageName }.toTypedArray()

        // Check if packages changed
        if (!monitoredPackages.contentEquals(newPackages)) {
            Log.d(TAG, "Package list changed, reconfiguring service")
            blockedApps = newBlockedApps
            monitoredPackages = newPackages

            // Reconfigure the service with new packages
            val info = AccessibilityServiceInfo().apply {
                packageNames = monitoredPackages
                eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_SCROLLED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
            }
            serviceInfo = info
        } else {
            // Just update blocked strings
            blockedApps = newBlockedApps
        }

        Log.d(TAG, "Loaded ${blockedApps.size} blocked apps: ${monitoredPackages.joinToString()}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Periodically reload settings to pick up changes
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSettingsReloadTime > SETTINGS_RELOAD_INTERVAL_MS) {
            loadBlockedApps()
            lastSettingsReloadTime = currentTime
        }

        val packageName = event.packageName?.toString() ?: return

        // Find the blocked app config for this package
        val blockedApp = blockedApps.find { it.packageName == packageName } ?: return

        try {
            // Get the root node in the current window
            val rootNode = rootInActiveWindow ?: return

            // Check if we're on a blocked page
            if (isBlockedPage(rootNode, blockedApp.blockedStrings)) {
                // Check if enough time has passed since last action (debouncing)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastActionTime < DEBOUNCE_DELAY_MS) {
                    Log.d(TAG, "Debouncing - ignoring detection")
                    rootNode.recycle()
                    return
                }

                lastActionTime = currentTime
                Log.d(TAG, "Blocked page detected in ${blockedApp.appName}!")
                Toast.makeText(this, "${blockedApp.appName} blocked", Toast.LENGTH_SHORT).show()

                // Press back button to exit
                performGlobalAction(GLOBAL_ACTION_BACK)

                // Then go home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun isBlockedPage(
        node: android.view.accessibility.AccessibilityNodeInfo,
        blockedStrings: List<String>
    ): Boolean {
        // Check current node
        val nodeText = node.text?.toString()
        val nodeContentDesc = node.contentDescription?.toString()
        val nodeViewId = node.viewIdResourceName

        // Check if this node contains any blocked strings
        for (blockedString in blockedStrings) {
            if (nodeText?.contains(blockedString, ignoreCase = false) == true ||
                nodeContentDesc?.contains(blockedString, ignoreCase = false) == true ||
                nodeViewId?.contains(blockedString, ignoreCase = false) == true) {
                Log.d(TAG, "Found blocked string: $blockedString")
                return true
            }
        }

        // Recursively check child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (isBlockedPage(childNode, blockedStrings)) {
                    childNode.recycle()
                    return true
                }
                childNode.recycle()
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        Toast.makeText(this, "Snapchat Blocker Service Stopped", Toast.LENGTH_SHORT).show()
    }
}
