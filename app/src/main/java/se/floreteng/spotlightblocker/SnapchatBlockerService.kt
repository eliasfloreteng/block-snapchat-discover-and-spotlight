package se.floreteng.spotlightblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class SnapchatBlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "SnapchatBlockerService"
        private const val SNAPCHAT_PACKAGE = "com.snapchat.android"

        // Known Discover page identifiers
        private val BLOCKED_PAGE_INDICATORS = listOf(
            "View Profile",  // Appears at bottom of Discover content
            "For You",  // On top of Spotlight page
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            // Listen to events from Snapchat
            packageNames = arrayOf(SNAPCHAT_PACKAGE)

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

        Toast.makeText(this, "Snapchat Blocker Service Started", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only process events from Snapchat
        if (event.packageName != SNAPCHAT_PACKAGE) return

        try {
            // Get the root node in the current window
            val rootNode = rootInActiveWindow ?: return

            // Check if we're on a blocked page (Discover)
            if (isBlockedPage(rootNode)) {
                Log.d(TAG, "Blocked page detected (Discover)!")
                Toast.makeText(this, "Discover blocked - going home", Toast.LENGTH_SHORT).show()

                // Go home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun isBlockedPage(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        // Check current node
        val nodeText = node.text?.toString()
        val nodeContentDesc = node.contentDescription?.toString()
        val nodeViewId = node.viewIdResourceName

        // Check if this node contains blocked page indicators
        for (indicator in BLOCKED_PAGE_INDICATORS) {
            if (nodeText?.contains(indicator, ignoreCase = false) == true ||
                nodeContentDesc?.contains(indicator, ignoreCase = false) == true ||
                nodeViewId?.contains(indicator, ignoreCase = false) == true) {
                Log.d(TAG, "Found blocked page indicator: $indicator")
                return true
            }
        }

        // Recursively check child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (isBlockedPage(childNode)) {
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
