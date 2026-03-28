package com.zchat.app.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.Contact
import com.zchat.app.databinding.ActivityContactsBinding
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private lateinit var repository: Repository
    private lateinit var adapter: ContactsAdapter

    private val CONTACTS_PERMISSION_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupRecyclerView()
        loadContacts()
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("ContactsActivity", "Error applying language", e)
        }
    }

    private fun applyTheme() {
        try {
            val repo = Repository(applicationContext)
            when (repo.theme) {
                0 -> setTheme(R.style.Theme_GOODOK_Classic)
                1 -> setTheme(R.style.Theme_GOODOK_Modern)
                2 -> setTheme(R.style.Theme_GOODOK_Neon)
                3 -> setTheme(R.style.Theme_GOODOK_Childish)
            }
        } catch (e: Exception) {
            setTheme(R.style.Theme_GOODOK_Classic)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.contacts)
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter(
            onEdit = { contact ->
                // Handle contact edit
                onContactClick(contact)
            },
            onDelete = { contact ->
                // Handle contact delete
                onDeleteContact(contact)
            }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun onContactClick(contact: Contact) {
        // TODO: Open chat with contact if registered, or invite them
        android.widget.Toast.makeText(this, contact.displayName, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun onDeleteContact(contact: Contact) {
        // Delete contact from local database
        lifecycleScope.launch {
            try {
                repository.deleteContact(contact.id)
                // Refresh the list
                loadContacts()
            } catch (e: Exception) {
                Log.e("ContactsActivity", "Error deleting contact", e)
            }
        }
    }

    private fun loadContacts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            importDeviceContacts()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun importDeviceContacts() {
        lifecycleScope.launch {
            try {
                val contacts = mutableListOf<Contact>()

                contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME
                    ),
                    null,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME + " ASC"
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val name = cursor.getString(nameIndex) ?: continue

                        // Get phone numbers for this contact
                        val phones = mutableListOf<String>()
                        contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id.toString()),
                            null
                        )?.use { phoneCursor ->
                            val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            while (phoneCursor.moveToNext()) {
                                val number = phoneCursor.getString(numberIndex)
                                phones.add(number.filter { it.isDigit() })
                            }
                        }

                        if (phones.isNotEmpty()) {
                            contacts.add(Contact(
                                id = id.toString(),
                                userId = "",
                                displayName = name,
                                phoneNumber = phones.first(),
                                isRegistered = false
                            ))
                        }
                    }
                }

                binding.progressBar.visibility = View.GONE
                adapter.submitList(contacts)
                binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE

                // Save to local database
                if (contacts.isNotEmpty()) {
                    repository.saveContacts(contacts)
                }

            } catch (e: Exception) {
                Log.e("ContactsActivity", "Error loading contacts", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                importDeviceContacts()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Permission denied"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
