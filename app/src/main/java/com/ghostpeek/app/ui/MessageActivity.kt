package com.ghostpeek.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ghostpeek.app.R
import com.ghostpeek.app.data.MessageStore
import com.ghostpeek.app.services.PeekNotificationService

class MessageActivity : AppCompatActivity() {

    private lateinit var store: MessageStore
    private lateinit var adapter: MessageAdapter
    private lateinit var senderName: String
    private lateinit var rvMessages: RecyclerView

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sender = intent.getStringExtra(PeekNotificationService.EXTRA_SENDER)
            if (sender.equals(senderName, ignoreCase = true)) refreshMessages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        senderName = intent.getStringExtra(EXTRA_SENDER) ?: run { finish(); return }

        store = MessageStore(this)
        rvMessages = findViewById(R.id.rvMessages)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = senderName
            setDisplayHomeAsUpEnabled(true)
        }

        val initials = senderName.split(" ").take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        findViewById<TextView>(R.id.tvAvatarInitials).text = initials

        setupRecyclerView()
        store.markAsRead(senderName)
    }

    private fun setupRecyclerView() {
        val messages = store.getMessages(senderName)
        adapter = MessageAdapter(messages.toMutableList()) { msg, emoji ->
            store.setReaction(senderName, msg.id, emoji)
        }
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
        if (adapter.itemCount > 0) rvMessages.scrollToPosition(adapter.itemCount - 1)
    }

    private fun refreshMessages() {
        val messages = store.getMessages(senderName)
        adapter = MessageAdapter(messages) { msg, emoji ->
            store.setReaction(senderName, msg.id, emoji)
        }
        rvMessages.adapter = adapter
        if (adapter.itemCount > 0) rvMessages.scrollToPosition(adapter.itemCount - 1)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        registerReceiver(messageReceiver, IntentFilter(PeekNotificationService.ACTION_NEW_MESSAGE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(messageReceiver)
    }

    companion object {
        const val EXTRA_SENDER = "extra_sender"
    }
}
