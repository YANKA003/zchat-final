package com.zchat.app.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ItemUserBinding

class UsersAdapter(private val onItemClick: (User) -> Unit) :
    ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUsername.text = user.username

            // Show online status
            if (user.isOnline) {
                binding.tvStatus.text = "онлайн"
                binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.primary))
                binding.statusIndicator.setBackgroundResource(R.drawable.status_online)
            } else {
                if (user.lastSeen > 0) {
                    val timeAgo = DateUtils.getRelativeTimeSpanString(
                        user.lastSeen,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    binding.tvStatus.text = "был(а) $timeAgo"
                } else {
                    binding.tvStatus.text = ""
                }
                binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.text_secondary))
                binding.statusIndicator.setBackgroundResource(R.drawable.status_offline)
            }

            // Show bio if available
            if (user.bio.isNotEmpty()) {
                binding.tvBio.text = user.bio
                binding.tvBio.visibility = android.view.View.VISIBLE
            } else {
                binding.tvBio.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
