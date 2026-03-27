package com.zchat.app.ui.contacts

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Contact
import com.zchat.app.databinding.ItemContactBinding
import com.zchat.app.ui.theme.ThemeManager

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
    private val onEditClick: (Contact) -> Unit,
    private val onBlockClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ContactViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(contact: Contact) {
            val colors = ThemeManager.getColors()
            
            binding.tvName.text = contact.displayName
            binding.tvPhone.text = contact.phoneNumber
            
            // Статус блокировки
            if (contact.isBlocked) {
                binding.tvStatus.text = "🚫 " + binding.root.context.getString(R.string.blocked)
                binding.tvStatus.setTextColor("#EF4444".toColorInt())
            } else if (!contact.isAllowed) {
                binding.tvStatus.text = "⚠️ " + binding.root.context.getString(R.string.not_allowed)
                binding.tvStatus.setTextColor("#F59E0B".toColorInt())
            } else {
                binding.tvStatus.visibility = android.view.View.GONE
            }
            
            // Аватар
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colors.primary.toColorInt())
            }
            binding.ivAvatar.background = avatarBg
            binding.tvAvatar.text = contact.displayName.take(1).uppercase()
            binding.tvAvatar.setTextColor(colors.sentMessageText.toColorInt())
            
            // Кнопки
            binding.root.setOnClickListener { onContactClick(contact) }
            binding.btnEdit.setOnClickListener { onEditClick(contact) }
            binding.btnBlock.setOnClickListener { onBlockClick(contact) }
            binding.btnBlock.text = if (contact.isBlocked) {
                binding.root.context.getString(R.string.unblock)
            } else {
                binding.root.context.getString(R.string.block)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Contact, newItem: Contact) = oldItem == newItem
    }
}
