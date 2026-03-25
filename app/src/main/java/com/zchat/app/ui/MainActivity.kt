package com.zchat.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ActivityMainBinding
import com.zchat.app.ui.auth.AuthActivity
import com.zchat.app.ui.chats.ChatActivity
import com.zchat.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var repository: Repository? = null
    private lateinit var adapter: UsersAdapter
    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            repository = Repository(applicationContext)

            if (repository?.currentUser == null) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                return
            }

            setupUI()
            checkContactsPermissionAndLoad()
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization error", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_manage)
            binding.toolbar.setNavigationOnClickListener {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }

            binding.navView.setNavigationItemSelectedListener { menuItem ->
                try {
                    when (menuItem.itemId) {
                        R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                        R.id.nav_premium -> {
                            val i = Intent(this, SettingsActivity::class.java)
                            i.putExtra("show_premium", true)
                            startActivity(i)
                        }
                        R.id.nav_logout -> {
                            repository?.logout()
                            startActivity(Intent(this, AuthActivity::class.java))
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Navigation error", e)
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                true
            }

            adapter = UsersAdapter { user -> openChat(user) }
            binding.rvUsers.layoutManager = LinearLayoutManager(this)
            binding.rvUsers.adapter = adapter

            binding.btnImportContacts.setOnClickListener {
                checkContactsPermissionAndLoad()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "UI setup error", e)
        }
    }

    private fun checkContactsPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadContactUsers()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
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
                loadContactUsers()
            } else {
                Toast.makeText(this, "Разрешение на контакты необходимо для поиска друзей", Toast.LENGTH_LONG).show()
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun getPhoneContacts(): Set<String> {
        val phones = mutableSetOf<String>()
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    // Normalize phone number (remove all non-digits)
                    val normalized = number.replace(Regex("[^0-9]"), "")
                    if (normalized.isNotEmpty()) {
                        phones.add(normalized)
                        // Also add without country code prefix for matching
                        if (normalized.length > 10) {
                            phones.add(normalized.takeLast(10))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading contacts", e)
        }
        return phones
    }

    private fun loadContactUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Get phone contacts
                val contactPhones = getPhoneContacts()
                Log.d("MainActivity", "Found ${contactPhones.size} phone numbers in contacts")

                // Get all users from Firebase
                val result = repository?.searchUsers("")
                binding.progressBar.visibility = View.GONE

                result?.fold(
                    onSuccess = { users ->
                        val currentUid = repository?.currentUser?.uid

                        // Filter users: show only those in contacts or self
                        val filtered = users.filter { user ->
                            if (user.uid == currentUid) {
                                false // Don't show self
                            } else {
                                // Check if user's phone is in contacts
                                val userPhone = user.phoneNumber.replace(Regex("[^0-9]"), "")
                                val userPhoneShort = if (userPhone.length > 10) userPhone.takeLast(10) else userPhone

                                contactPhones.contains(userPhone) ||
                                contactPhones.contains(userPhoneShort) ||
                                contactPhones.any { it.endsWith(userPhoneShort) || userPhoneShort.endsWith(it.takeLast(10)) }
                            }
                        }

                        Log.d("MainActivity", "Filtered ${filtered.size} users from contacts")
                        adapter.submitList(filtered)
                        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvUsers.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

                        if (filtered.isEmpty()) {
                            binding.tvEmpty.text = "Никто из ваших контактов\nещё не использует ZChat"
                        }
                    },
                    onFailure = {
                        Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                        binding.emptyState.visibility = View.VISIBLE
                    }
                )
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e("MainActivity", "Load users error", e)
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openChat(user: User) {
        try {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("userId", user.uid)
            intent.putExtra("username", user.username)
            intent.putExtra("userPhone", user.phoneNumber)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Open chat error", e)
            Toast.makeText(this, "Ошибка открытия чата", Toast.LENGTH_SHORT).show()
        }
    }
}
