package com.zchat.app.ui.chats

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Message
import com.zchat.app.databinding.ItemMessageBinding
import com.zchat.app.ui.theme.ThemeManager

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
            val colors = ThemeManager.getColors()
            
            if (isSent) {
                val sentBg = GradientDrawable().apply {
                    setColor(colors.sentMessage.toColorInt())
                    cornerRadius = 32f
                }
                binding.tvMessage.background = sentBg
                binding.tvMessage.setTextColor(colors.sentMessageText.toColorInt())
            } else {
                val receivedBg = GradientDrawable().apply {
                    setColor(colors.receivedMessage.toColorInt())
                    cornerRadius = 32f
                }
                binding.tvMessage.background = receivedBg
                binding.tvMessage.setTextColor(colors.receivedMessageText.toColorInt())
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
