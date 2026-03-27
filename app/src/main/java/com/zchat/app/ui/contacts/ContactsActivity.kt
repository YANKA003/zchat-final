package com.zchat.app.ui.contacts

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Contact
import com.zchat.app.databinding.ActivityContactsBinding
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityContactsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = ContactsAdapter(
            onContactClick = { contact -> openContact(contact) },
            onEditClick = { contact -> editContact(contact) },
            onBlockClick = { contact -> toggleBlock(contact) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
        
        loadContacts()
    }
    
    private fun loadContacts() {
        binding.emptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            // Demo данные
            val contacts = createDemoContacts()
            
            if (contacts.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
            } else {
                adapter.submitList(contacts)
            }
        }
    }
    
    private fun createDemoContacts(): List<Contact> {
        val uid = repository.currentUser?.uid ?: ""
        return listOf(
            Contact(
                id = "c1",
                userId = uid,
                contactUserId = "user1",
                displayName = "Анна Иванова",
                phoneNumber = "+375291234567",
                isAllowed = true
            ),
            Contact(
                id = "c2",
                userId = uid,
                contactUserId = "user2",
                displayName = "Петр Петров",
                phoneNumber = "+375292345678",
                isAllowed = true
            ),
            Contact(
                id = "c3",
                userId = uid,
                contactUserId = "user3",
                displayName = "Мария Сидорова",
                phoneNumber = "+375293456789",
                isBlocked = true
            )
        )
    }
    
    private fun openContact(contact: Contact) {
        // Открыть чат с контактом
    }
    
    private fun editContact(contact: Contact) {
        val input = android.widget.EditText(this).apply {
            setText(contact.displayName)
            setSingleLine()
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_contact)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateContactName(contact, newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun updateContactName(contact: Contact, newName: String) {
        lifecycleScope.launch {
            // Обновить имя контакта
            loadContacts()
        }
    }
    
    private fun toggleBlock(contact: Contact) {
        val message = if (contact.isBlocked) {
            R.string.unblock_contact_confirm
        } else {
            R.string.block_contact_confirm
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (contact.isBlocked) R.string.unblock else R.string.block)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ ->
                performBlock(contact, !contact.isBlocked)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    private fun performBlock(contact: Contact, block: Boolean) {
        lifecycleScope.launch {
            // Заблокировать/разблокировать
            loadContacts()
        }
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
    }
}
