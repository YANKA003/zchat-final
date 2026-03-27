package com.zchat.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ItemUserBinding
import java.text.SimpleDateFormat
import java.util.*

class UsersAdapter(private val onUserClick: (User) -> Unit) :
    ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUsername.text = user.username
            binding.tvEmail.text = user.email

            // Online status
            if (user.isOnline) {
                binding.ivOnline.visibility = android.view.View.VISIBLE
                binding.tvStatus.text = binding.root.context.getString(R.string.online)
            } else {
                binding.ivOnline.visibility = android.view.View.GONE
                if (user.lastSeen > 0) {
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    binding.tvStatus.text = binding.root.context.getString(R.string.last_seen) + ": " + sdf.format(Date(user.lastSeen))
                } else {
                    binding.tvStatus.text = binding.root.context.getString(R.string.offline)
                }
            }

            // Avatar
            val firstLetter = user.username.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
            binding.tvAvatar.text = firstLetter

            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
