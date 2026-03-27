package com.zchat.app.ui.chats

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Message
import com.zchat.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val currentUserId: String,
    private val onMessageLongClick: (Message) -> Unit
) : ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isSentByMe = message.senderId == currentUserId

            // Message content
            if (message.isDeleted) {
                binding.tvMessage.text = binding.root.context.getString(R.string.message_deleted)
                binding.tvMessage.alpha = 0.5f
            } else {
                binding.tvMessage.text = message.content
                binding.tvMessage.alpha = 1f
            }

            // Edited indicator
            if (message.isEdited && !message.isDeleted) {
                binding.tvEdited.visibility = android.view.View.VISIBLE
            } else {
                binding.tvEdited.visibility = android.view.View.GONE
            }

            // Timestamp
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(message.timestamp))

            // Alignment based on sender
            val params = binding.root.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

            if (isSentByMe) {
                // Sent message - align right
                binding.cardView.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.message_sent)
                )
                binding.tvMessage.setTextColor(binding.root.context.getColor(android.R.color.white))
                binding.tvTime.setTextColor(binding.root.context.getColor(android.R.color.white))
                binding.tvEdited.setTextColor(binding.root.context.getColor(android.R.color.white))
            } else {
                // Received message - align left
                binding.cardView.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.message_received)
                )
                binding.tvMessage.setTextColor(binding.root.context.getColor(android.R.color.black))
                binding.tvTime.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))
                binding.tvEdited.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))
            }

            // Long click for options
            binding.root.setOnLongClickListener {
                if (!message.isDeleted) {
                    onMessageLongClick(message)
                }
                true
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
}
