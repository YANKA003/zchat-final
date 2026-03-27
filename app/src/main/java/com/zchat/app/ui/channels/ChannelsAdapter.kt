package com.zchat.app.ui.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.data.model.Channel
import com.zchat.app.databinding.ItemChannelBinding

class ChannelsAdapter(
    private val currentUserId: String,
    private val onChannelClick: (Channel) -> Unit,
    private val onSubscribeClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelsAdapter.ChannelViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ChannelViewHolder(private val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(channel: Channel) {
            binding.tvName.text = channel.name
            binding.tvDescription.text = channel.description
            binding.tvSubscribers.text = formatSubscribers(channel.subscribersCount)
            
            binding.root.setOnClickListener { onChannelClick(channel) }
            binding.btnSubscribe.setOnClickListener { onSubscribeClick(channel) }
        }
        
        private fun formatSubscribers(count: Long): String {
            return when {
                count >= 1000000 -> "${count / 1000000}M"
                count >= 1000 -> "${count / 1000}K"
                else -> count.toString()
            } + " подписчиков"
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
