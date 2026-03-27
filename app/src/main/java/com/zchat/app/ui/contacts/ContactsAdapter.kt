package com.zchat.app.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zchat.app.R
import com.zchat.app.data.model.Contact
import com.zchat.app.databinding.ItemContactBinding

class ContactsAdapter(
    private val onEdit: (Contact) -> Unit,
    private val onDelete: (Contact) -> Unit
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvName.text = contact.displayName

            if (contact.phoneNumber.isNotEmpty()) {
                binding.tvPhone.text = contact.phoneNumber
                binding.tvPhone.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPhone.visibility = android.view.View.GONE
            }

            // Avatar first letter
            val firstLetter = contact.displayName.takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"
            binding.tvAvatar.text = firstLetter

            // Registered badge
            if (contact.isRegistered) {
                binding.tvRegistered.visibility = android.view.View.VISIBLE
            } else {
                binding.tvRegistered.visibility = android.view.View.GONE
            }

            binding.btnEdit.setOnClickListener { onEdit(contact) }
            binding.btnDelete.setOnClickListener { onDelete(contact) }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Contact, newItem: Contact) = oldItem == newItem
    }
}
