package com.zchat.app.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Message
import com.zchat.app.databinding.ItemMessageBinding

class MessagesAdapter(private val currentUserId: String) : ListAdapter<Message, MessagesAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            val isSent = message.senderId == currentUserId
            if (isSent) {
                binding.tvMessage.setBackgroundResource(R.drawable.bg_message_sent)
                binding.tvMessage.setTextColor(0xFFFFFFFF.toInt())
            } else {
                binding.tvMessage.setBackgroundResource(R.drawable.bg_message_received)
                binding.tvMessage.setTextColor(0xFF1E293B.toInt())
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
