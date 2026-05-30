package com.ghostpeek.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ghostpeek.app.R
import com.ghostpeek.app.data.MessageStore
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<MessageStore.PeekMessage>,
    private val onReaction: ((MessageStore.PeekMessage, String) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    companion object {
        private const val VIEW_TYPE_MINE = 1
        private const val VIEW_TYPE_THEIRS = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMine) VIEW_TYPE_MINE else VIEW_TYPE_THEIRS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_MINE) R.layout.item_message_mine else R.layout.item_message_theirs
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.bind(msg)
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvMessage: TextView? = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView? = view.findViewById(R.id.tvTime)
        private val tvStatus: TextView? = view.findViewById(R.id.tvStatus)
        private val tvReaction: TextView? = view.findViewById(R.id.tvReaction)
        private val reactionBar: LinearLayout? = view.findViewById(R.id.reactionBar)

        fun bind(msg: MessageStore.PeekMessage) {
            tvMessage?.text = msg.text
            tvTime?.text = timeFormat.format(Date(msg.timestamp))

            // Status (mine only)
            tvStatus?.visibility = if (msg.isMine) View.VISIBLE else View.GONE
            tvStatus?.text = when (msg.status) {
                MessageStore.STATUS_SENT -> "Sent"
                MessageStore.STATUS_DELIVERED -> "Delivered"
                MessageStore.STATUS_READ -> "Read"
                else -> ""
            }

            // Reaction badge
            if (msg.reaction != null) {
                tvReaction?.text = msg.reaction
                tvReaction?.visibility = View.VISIBLE
            } else {
                tvReaction?.visibility = View.GONE
            }

            // Long-press for reactions (iOS Tapback style)
            itemView.setOnLongClickListener {
                showReactionPicker(msg)
                true
            }
        }

        private fun showReactionPicker(msg: MessageStore.PeekMessage) {
            val emojis = listOf("❤️", "👍", "😂", "😮", "😢", "👎")
            val dialog = android.app.AlertDialog.Builder(itemView.context)
                .setTitle("React")
                .setItems(emojis.toTypedArray()) { _, which ->
                    onReaction?.invoke(msg, emojis[which])
                    notifyItemChanged(messages.indexOf(msg))
                }
                .create()
            dialog.show()
        }
    }
}
