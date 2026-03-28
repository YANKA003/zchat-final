package com.zchat.app.ui.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Channel
import com.zchat.app.databinding.ItemChannelBinding

class ChannelsAdapter(
    private val currentUserId: String,
    private val onSubscribe: (Channel) -> Unit
) : ListAdapter<Channel, ChannelsAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            val context = binding.root.context

            // Channel name
            binding.tvChannelName.text = channel.name

            // Description
            if (channel.description.isNotEmpty()) {
                binding.tvDescription.text = channel.description
                binding.tvDescription.visibility = android.view.View.VISIBLE
            } else {
                binding.tvDescription.visibility = android.view.View.GONE
            }

            // Avatar first letter
            val firstLetter = channel.name.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
            binding.tvAvatar.text = firstLetter

            // Subscribers count
            binding.tvSubscribers.text = context.getString(R.string.subscribers, channel.subscribersCount)

            // Subscribe button - check if user is owner
            val isOwner = channel.ownerId == currentUserId
            if (isOwner) {
                binding.btnSubscribe.text = context.getString(R.string.edit_channel)
                binding.btnSubscribe.isEnabled = false
            } else {
                binding.btnSubscribe.text = context.getString(R.string.subscribe)
                binding.btnSubscribe.isEnabled = true
                binding.btnSubscribe.setOnClickListener { onSubscribe(channel) }
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
