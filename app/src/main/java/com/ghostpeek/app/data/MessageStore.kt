package com.ghostpeek.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores messages captured from notifications locally on the device.
 * Uses SharedPreferences with JSON serialization — no internet required.
 * Messages are keyed by sender name and capped at MAX_PER_SENDER.
 */
class MessageStore(context: Context) {

    private val prefs = context.getSharedPreferences("ghost_peek_store", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_PER_SENDER = 50
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"
    }

    data class PeekMessage(
        val id: String,
        val text: String,
        val timestamp: Long,
        val isMine: Boolean,
        val status: String = STATUS_DELIVERED,
        val reaction: String? = null,
        val replyToId: String? = null,
        val replyPreview: String? = null
    )

    data class ConversationSummary(
        val sender: String,
        val lastMessage: String,
        val lastTimestamp: Long,
        val unreadCount: Int
    )

    fun saveIncoming(sender: String, text: String) {
        saveMessage(sender, text, isMine = false)
    }

    fun saveOutgoing(sender: String, text: String) {
        saveMessage(sender, text, isMine = true)
    }

    private fun saveMessage(
        sender: String,
        text: String,
        isMine: Boolean,
        status: String = STATUS_DELIVERED
    ) {
        if (text.isBlank() || sender.isBlank()) return

        val key = toKey(sender)
        val existing = loadRaw(key).toMutableList()

        val msgId = System.currentTimeMillis().toString()
        existing.add(
            JSONObject().apply {
                put("id", msgId)
                put("text", text.trim())
                put("ts", System.currentTimeMillis())
                put("mine", isMine)
                put("status", status)
                put("reaction", JSONObject.NULL)
                put("replyToId", JSONObject.NULL)
                put("replyPreview", JSONObject.NULL)
                put("unread", !isMine)
            }
        )

        val trimmed = if (existing.size > MAX_PER_SENDER) existing.takeLast(MAX_PER_SENDER) else existing
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun setReaction(sender: String, messageId: String, reaction: String?) {
        val key = toKey(sender)
        val messages = loadRaw(key).toMutableList()
        val updated = messages.map { obj ->
            if (obj.optString("id") == messageId) {
                obj.put("reaction", reaction ?: JSONObject.NULL)
            }
            obj
        }
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun getMessages(sender: String): List<PeekMessage> {
        val key = toKey(sender)
        return loadRaw(key).mapNotNull { obj ->
            try {
                PeekMessage(
                    id = obj.optString("id", obj.getLong("ts").toString()),
                    text = obj.getString("text"),
                    timestamp = obj.getLong("ts"),
                    isMine = obj.getBoolean("mine"),
                    status = obj.optString("status", STATUS_DELIVERED),
                    reaction = if (obj.isNull("reaction")) null else obj.optString("reaction"),
                    replyToId = if (obj.isNull("replyToId")) null else obj.optString("replyToId"),
                    replyPreview = if (obj.isNull("replyPreview")) null else obj.optString("replyPreview")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getUnreadCount(sender: String): Int {
        val key = toKey(sender)
        return loadRaw(key).count { it.optBoolean("unread", false) }
    }

    fun markAsRead(sender: String) {
        val key = toKey(sender)
        val messages = loadRaw(key).map { obj ->
            obj.put("unread", false)
            obj
        }
        val arr = JSONArray()
        messages.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun getAllConversations(): List<ConversationSummary> {
        return prefs.all.keys.mapNotNull { key ->
            val msgs = loadRaw(key)
            if (msgs.isEmpty()) return@mapNotNull null
            val last = msgs.last()
            val senderName = key.replace("_", " ").trim()
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            ConversationSummary(
                sender = senderName,
                lastMessage = last.optString("text", ""),
                lastTimestamp = last.optLong("ts", 0L),
                unreadCount = msgs.count { it.optBoolean("unread", false) }
            )
        }.sortedByDescending { it.lastTimestamp }
    }

    fun getAllSenders(): List<String> {
        return prefs.all.keys.map { key ->
            key.replace("_", " ").trim()
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    fun clearSender(sender: String) {
        prefs.edit().remove(toKey(sender)).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun loadRaw(key: String): List<JSONObject> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun toKey(sender: String): String {
        return sender.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .take(80)
    }
}
