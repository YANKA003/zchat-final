package com.zchat.app.ui.contacts

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Contact
import com.zchat.app.databinding.ActivityContactsBinding
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter
    private val CONTACTS_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.contacts)

        setupRecyclerView()
        setupSearch()
        loadContacts()

        binding.fabImport.setOnClickListener { checkContactsPermission() }
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(
            onEdit = { contact -> showEditDialog(contact) },
            onDelete = { contact -> deleteContact(contact) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            searchContacts(text?.toString() ?: "")
        }
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val contacts = repository.getLocalContacts()
            binding.progressBar.visibility = View.GONE

            adapter.submitList(contacts)
            binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun searchContacts(query: String) {
        lifecycleScope.launch {
            val contacts = repository.searchContactsLocal(query)
            adapter.submitList(contacts)
        }
    }

    private fun checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION
            )
        } else {
            importPhoneContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            importPhoneContacts()
        }
    }

    private fun importPhoneContacts() {
        binding.progressBar.visibility = View.VISIBLE

        val contactsList = mutableListOf<Contact>()

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val phone = it.getString(phoneIndex)

                    val contact = Contact(
                        id = id,
                        userId = "",
                        displayName = name,
                        phoneNumber = phone.replace("\\s".toRegex(), ""),
                        isRegistered = false
                    )
                    contactsList.add(contact)
                }
            }

            lifecycleScope.launch {
                repository.saveContacts(contactsList)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ContactsActivity,
                    getString(R.string.contacts_imported) + ": ${contactsList.size}",
                    Toast.LENGTH_SHORT
                ).show()
                loadContacts()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(contact: Contact) {
        val input = EditText(this).apply {
            setText(contact.displayName)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_contact)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updated = contact.copy(displayName = newName)
                    lifecycleScope.launch {
                        repository.updateContact(updated)
                        loadContacts()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteContact(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_contact)
            .setMessage("Are you sure?")
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteContact(contact)
                    loadContacts()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
