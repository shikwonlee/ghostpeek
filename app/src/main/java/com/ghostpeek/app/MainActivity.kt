package com.ghostpeek.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ghostpeek.app.data.MessageStore
import com.ghostpeek.app.services.PeekNotificationService
import com.ghostpeek.app.ui.ConversationAdapter
import com.ghostpeek.app.ui.MessageActivity

class MainActivity : AppCompatActivity() {

    private lateinit var store: MessageStore
    private lateinit var adapter: ConversationAdapter
    private lateinit var rvConversations: RecyclerView
    private lateinit var tvEmpty: TextView

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PeekNotificationService.ACTION_NEW_MESSAGE) {
                refreshConversations()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "GhostPeek"

        rvConversations = findViewById(R.id.rvConversations)
        tvEmpty = findViewById(R.id.tvEmpty)

        store = MessageStore(this)
        setupRecyclerView()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(emptyList()) { conv ->
            val intent = Intent(this, MessageActivity::class.java).apply {
                putExtra(MessageActivity.EXTRA_SENDER, conv.sender)
            }
            startActivity(intent)
        }
        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = adapter
    }

    private fun refreshConversations() {
        val conversations = store.getAllConversations()
        adapter.updateList(conversations)
        tvEmpty.isVisible = conversations.isEmpty()
    }

    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        if (!isNotificationListenerEnabled()) missing.add("Notification Access")
        if (!isAccessibilityEnabled()) missing.add("Accessibility Service")
        if (!Settings.canDrawOverlays(this)) missing.add("Display over other apps")
        if (missing.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("GhostPeek needs:\n\n" + missing.joinToString("\n") { "• $it" })
            .setPositiveButton("Grant") { _, _ -> requestNextPermission() }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun requestNextPermission() {
        when {
            !isNotificationListenerEnabled() ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            !isAccessibilityEnabled() ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            !Settings.canDrawOverlays(this) ->
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled == 0) return false
        val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return services.contains(packageName)
    }

    override fun onResume() {
        super.onResume()
        refreshConversations()
        @Suppress("DEPRECATION")
        registerReceiver(messageReceiver, IntentFilter(PeekNotificationService.ACTION_NEW_MESSAGE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(messageReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear All Messages")
                    .setMessage("Delete all captured messages?")
                    .setPositiveButton("Clear") { _, _ -> store.clearAll(); refreshConversations() }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            R.id.action_settings -> { checkPermissions(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
