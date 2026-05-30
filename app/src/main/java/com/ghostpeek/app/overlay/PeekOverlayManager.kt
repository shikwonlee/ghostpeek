package com.ghostpeek.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ghostpeek.app.R
import com.ghostpeek.app.data.MessageStore
import com.ghostpeek.app.ui.MessageAdapter

/**
 * Manages the iOS-style peek overlay window.
 * Shows a floating sheet with message bubbles when triggered by long-press.
 */
class PeekOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val store = MessageStore(context)

    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
    }

    fun show(senderName: String) {
        dismiss() // Remove any existing overlay first

        val messages = store.getMessages(senderName)
        if (messages.isEmpty()) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_peek, null)

        // Set sender name
        view.findViewById<TextView>(R.id.tvSenderName)?.text = senderName

        // Set avatar initials
        val initials = senderName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
        view.findViewById<TextView>(R.id.tvAvatarInitials)?.text = initials

        // Set up message list
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvMessages)
        val adapter = MessageAdapter(messages.takeLast(10), onReaction = { msg, emoji ->
            store.setReaction(senderName, msg.id, emoji)
        })
        recyclerView?.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            this.adapter = adapter
        }

        // Close button
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dismiss() }

        // Dismiss on background tap
        view.setOnClickListener { dismiss() }

        try {
            windowManager.addView(view, overlayParams)
            overlayView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already removed
            }
            overlayView = null
        }
    }

    fun isShowing() = overlayView != null
}
