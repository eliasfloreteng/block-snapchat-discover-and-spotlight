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

        // Known Spotlight identifiers (we'll start with text matching for MVP)
        private val SPOTLIGHT_INDICATORS = listOf(
            "spotlight",
            "Spotlight",
            "SPOTLIGHT"
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

            // Check if we're on the Spotlight page
            if (isSpotlightPage(rootNode)) {
                Log.d(TAG, "Spotlight page detected! Triggering home action")
                Toast.makeText(this, "Spotlight detected - going home", Toast.LENGTH_SHORT).show()

                // Go home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun isSpotlightPage(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        // Check current node
        val nodeText = node.text?.toString()
        val nodeContentDesc = node.contentDescription?.toString()
        val nodeViewId = node.viewIdResourceName

        // Log for debugging purposes
        if (nodeText != null || nodeContentDesc != null || nodeViewId != null) {
            Log.v(TAG, "Node - Text: $nodeText, ContentDesc: $nodeContentDesc, ViewId: $nodeViewId")
        }

        // Check if this node contains Spotlight indicators
        for (indicator in SPOTLIGHT_INDICATORS) {
            if (nodeText?.contains(indicator, ignoreCase = true) == true ||
                nodeContentDesc?.contains(indicator, ignoreCase = true) == true ||
                nodeViewId?.contains(indicator, ignoreCase = true) == true) {
                Log.d(TAG, "Found Spotlight indicator: $indicator")
                return true
            }
        }

        // Recursively check child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if (isSpotlightPage(childNode)) {
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
