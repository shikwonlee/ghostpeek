package com.ghostpeek.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.ghostpeek.app.R
import com.ghostpeek.app.data.MessageStore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ConversationAdapter(
    private var conversations: List<MessageStore.ConversationSummary>,
    private val onClick: (MessageStore.ConversationSummary) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConvViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConvViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConvViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    fun updateList(newList: List<MessageStore.ConversationSummary>) {
        conversations = newList
        notifyDataSetChanged()
    }

    inner class ConvViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvPreview: TextView = view.findViewById(R.id.tvPreview)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)

        fun bind(conv: MessageStore.ConversationSummary) {
            val initials = conv.sender.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
            tvInitials.text = initials
            tvName.text = conv.sender
            tvPreview.text = conv.lastMessage
            tvTime.text = formatTime(conv.lastTimestamp)

            if (conv.unreadCount > 0) {
                tvBadge.isVisible = true
                tvBadge.text = if (conv.unreadCount > 99) "99+" else conv.unreadCount.toString()
            } else {
                tvBadge.isVisible = false
            }

            itemView.setOnClickListener { onClick(conv) }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
                diff < TimeUnit.DAYS.toMillis(7) -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
