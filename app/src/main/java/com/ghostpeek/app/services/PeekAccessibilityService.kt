package com.ghostpeek.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ghostpeek.app.overlay.PeekOverlayManager

/**
 * Detects long-press events on Messenger chat rows.
 * When a long-press is detected, the PeekOverlay is shown with the conversation.
 */
class PeekAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: PeekOverlayManager

    private val messengerPackages = setOf(
        "com.facebook.orca",
        "com.facebook.mlite"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = PeekOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName?.toString() !in messengerPackages) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                handleLongPress(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Dismiss overlay if user navigated away from Messenger
                if (event.packageName?.toString() !in messengerPackages) {
                    overlayManager.dismiss()
                }
            }
        }
    }

    private fun handleLongPress(event: AccessibilityEvent) {
        val source = event.source ?: return

        // Walk up the node tree to find the conversation row
        val conversationNode = findConversationRow(source)
        val senderName = extractSenderName(conversationNode ?: source)

        if (!senderName.isNullOrBlank()) {
            overlayManager.show(senderName)
        }

        source.recycle()
        conversationNode?.recycle()
    }

    /**
     * Walk up the accessibility node tree to find the containing conversation row.
     * Messenger uses RecyclerView items for each conversation.
     */
    private fun findConversationRow(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0

        while (current != null && depth < 6) {
            // Look for clickable rows that likely contain contact names
            if (current.isClickable && current.childCount > 0) {
                val nameNode = findNameNode(current)
                if (nameNode != null) {
                    nameNode.recycle()
                    return current
                }
            }
            val parent = current.parent
            if (depth > 0) current.recycle()
            current = parent
            depth++
        }
        return null
    }

    /**
     * Try to extract a sender name from accessibility node text.
     */
    private fun extractSenderName(node: AccessibilityNodeInfo): String? {
        // Try direct text first
        val directText = node.text?.toString()
            ?: node.contentDescription?.toString()

        if (!directText.isNullOrBlank() && directText.length < 60) {
            return directText.trim()
        }

        // Search children for a name-like text node
        return findNameNode(node)?.let { nameNode ->
            val name = nameNode.text?.toString()
                ?: nameNode.contentDescription?.toString()
            nameNode.recycle()
            name?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }
        }
    }

    private fun findNameNode(parent: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i) ?: continue
            val text = child.text?.toString() ?: child.contentDescription?.toString()
            // A name-like node: has text, not too long, not a timestamp or message preview
            if (!text.isNullOrBlank() && text.length in 2..50 && !text.contains(":")) {
                return child
            }
            child.recycle()
        }
        return null
    }

    override fun onInterrupt() {
        overlayManager.dismiss()
    }

    override fun onDestroy() {
        overlayManager.dismiss()
        super.onDestroy()
    }
}
