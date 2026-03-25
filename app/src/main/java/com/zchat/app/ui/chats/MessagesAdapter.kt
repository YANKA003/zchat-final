package com.zchat.app.ui.chats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.data.model.Message
import com.zchat.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(private val currentUserId: String) :
    ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(message: Message) {
            val isMine = message.senderId == currentUserId
            binding.tvMessage.text = message.content
            binding.tvTime.text = timeFormat.format(Date(message.timestamp))

            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if (isMine) {
                binding.root.setBackgroundResource(com.zchat.app.R.drawable.bg_message_sent)
                params.marginStart = 100
                params.marginEnd = 16
            } else {
                binding.root.setBackgroundResource(com.zchat.app.R.drawable.bg_message_received)
                params.marginStart = 16
                params.marginEnd = 100
            }
            binding.root.layoutParams = params
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
