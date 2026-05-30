package com.ghostpeek.app.services

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ghostpeek.app.data.MessageStore

/**
 * Listens for incoming Messenger notifications silently.
 * Does NOT mark messages as read — no read receipt is triggered.
 */
class PeekNotificationService : NotificationListenerService() {

    private lateinit var store: MessageStore

    // Facebook Messenger package names
    private val messengerPackages = setOf(
        "com.facebook.orca",          // Messenger
        "com.facebook.mlite",         // Messenger Lite
        "com.instagram.android",      // Instagram DMs
        "com.facebook.katana"         // Facebook app messages
    )

    override fun onCreate() {
        super.onCreate()
        store = MessageStore(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in messengerPackages) return

        val notification = sbn.notification ?: return
        val extras: Bundle = notification.extras ?: return

        // Extract sender and message text from the notification
        val sender = extractSender(extras)
        val text = extractText(extras)

        if (!sender.isNullOrBlank() && !text.isNullOrBlank()) {
            store.saveIncoming(sender, text)

            // Broadcast so the UI can refresh if open
            val intent = Intent(ACTION_NEW_MESSAGE).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_TEXT, text)
            }
            sendBroadcast(intent)
        }
    }

    private fun extractSender(extras: Bundle): String? {
        // Try modern MessagingStyle first
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg is Bundle) {
                val sender = lastMsg.getCharSequence("sender")?.toString()
                if (!sender.isNullOrBlank()) return sender
            }
        }

        // Fallback to notification title
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?.replace(" sent you a message", "")
            ?.replace(" sent a message", "")
            ?.trim()
    }

    private fun extractText(extras: Bundle): String? {
        // Try MessagingStyle messages first (most accurate)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg is Bundle) {
                val text = lastMsg.getCharSequence("text")?.toString()
                if (!text.isNullOrBlank()) return text
            }
        }

        // Fallback to notification body
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }

    companion object {
        const val ACTION_NEW_MESSAGE = "com.ghostpeek.app.NEW_MESSAGE"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_TEXT = "text"
    }
}
