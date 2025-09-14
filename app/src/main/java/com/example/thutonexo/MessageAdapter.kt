package com.example.thutonexo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var items: List<ChatItem>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_DATE = 3
    }

    // Add this function to update items
    fun updateItems(newItems: List<ChatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ChatItem.DateHeader -> VIEW_TYPE_DATE
            is ChatItem.MessageItem -> {
                val msg = (items[position] as ChatItem.MessageItem).message
                if (msg.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> SentViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            )
            VIEW_TYPE_RECEIVED -> ReceivedViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            )
            VIEW_TYPE_DATE -> DateViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is ChatItem.MessageItem -> {
                val msg = item.message
                val timeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                if (holder is SentViewHolder) {
                    holder.messageText.text = msg.text
                    holder.messageTime.text = timeText
                }
                if (holder is ReceivedViewHolder) {
                    holder.messageText.text = msg.text
                    holder.messageTime.text = timeText
                }
            }
        }
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        fun bind(item: ChatItem.DateHeader) {
            dateText.text = item.date
        }
    }
}