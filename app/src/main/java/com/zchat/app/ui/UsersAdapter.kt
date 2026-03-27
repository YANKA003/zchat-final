package com.zchat.app.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ItemUserBinding
import com.zchat.app.ui.theme.ThemeManager

class UsersAdapter(private val onUserClick: (User) -> Unit) : ListAdapter<User, UsersAdapter.UserViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onUserClick(getItem(pos))
            }
        }

        fun bind(user: User) {
            binding.tvUsername.text = user.username.ifEmpty { "User" }
            binding.tvStatus.text = if (user.isOnline) "В сети" else "Не в сети"
            binding.statusIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            
            // Первая буква имени в аватаре
            binding.tvAvatar.text = user.username.take(1).uppercase()
            
            // Применяем цвета темы
            val colors = ThemeManager.getColors()
            
            // Цвет аватара
            val avatarBg = GradientDrawable().apply {
                setColor(colors.primary.toColorInt())
                cornerRadius = 28f
            }
            binding.tvAvatar.background = avatarBg
            
            // Цвет онлайн индикатора
            val statusBg = GradientDrawable().apply {
                setColor(colors.onlineIndicator.toColorInt())
                cornerRadius = 7f
            }
            binding.statusIndicator.background = statusBg
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
