package com.zchat.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.zchat.app.ui.calls.CallsActivity
import com.zchat.app.ui.channels.ChannelsActivity
import com.zchat.app.ui.chats.ChatActivity
import com.zchat.app.ui.contacts.ContactsActivity
import com.zchat.app.ui.settings.SettingsActivity
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter
    private var currentTheme = 0

    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language first
        applyLanguage()

        // Apply theme
        applyTheme()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            repository = Repository(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing repository", e)
            Toast.makeText(this, "Initialization error", Toast.LENGTH_LONG).show()
            goToAuth()
            return
        }

        // Check if logged in
        try {
            if (repository.currentUser == null) {
                Log.d("MainActivity", "No user logged in, going to Auth")
                goToAuth()
                return
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking user", e)
            goToAuth()
            return
        }

        currentTheme = repository.theme

        setupToolbar()
        setupNavigation()
        setupBottomNavigation()
        setupUserList()
        loadUsers()
    }

    private fun goToAuth() {
        try {
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error going to auth", e)
        }
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error applying language", e)
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
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_manage)
            title = getString(R.string.app_name)
        }
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            try {
                when (menuItem.itemId) {
                    R.id.nav_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    R.id.nav_premium -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        intent.putExtra("show_premium", true)
                        startActivity(intent)
                    }
                    R.id.nav_logout -> {
                        repository.logout()
                        goToAuth()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNavigation() {
        // Show bottom navigation for designs 3 and 4 (Neon and Childish)
        if (currentTheme >= 2) {
            binding.bottomNavigation.visibility = View.VISIBLE
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.nav_chats -> true
                        R.id.nav_calls -> {
                            startActivity(Intent(this, CallsActivity::class.java))
                            true
                        }
                        R.id.nav_channels -> {
                            startActivity(Intent(this, ChannelsActivity::class.java))
                            true
                        }
                        R.id.nav_contacts -> {
                            startActivity(Intent(this, ContactsActivity::class.java))
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Bottom nav error", e)
                    false
                }
            }
        } else {
            binding.bottomNavigation.visibility = View.GONE
        }
    }

    private fun setupUserList() {
        adapter = UsersAdapter { user -> openChat(user) }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        // Check contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadUsersFromContacts()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }

    private fun loadUsersFromContacts() {
        lifecycleScope.launch {
            try {
                // Get phone numbers from device contacts
                val phoneNumbers = getPhoneNumbersFromContacts()

                if (phoneNumbers.isEmpty()) {
                    // No contacts found, show empty
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = getString(R.string.no_contacts)
                    return@launch
                }

                // Find registered users with these phone numbers
                val result = repository.findUsersByPhones(phoneNumbers)
                binding.progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { users ->
                        // Filter out current user
                        val filtered = users.filter { it.uid != repository.currentUserId }
                        adapter.submitList(filtered)
                        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvEmpty.text = getString(R.string.no_contacts)
                    },
                    onFailure = { e ->
                        Log.e("MainActivity", "Error finding users", e)
                        binding.progressBar.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "Error: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading users", e)
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Error: ${e.message}"
            }
        }
    }

    private fun getPhoneNumbersFromContacts(): List<String> {
        val phones = mutableListOf<String>()

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIndex)
                    // Normalize phone number (remove all non-digits)
                    val normalized = number.filter { it.isDigit() }
                    if (normalized.isNotEmpty()) {
                        phones.add(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading contacts", e)
        }

        return phones.distinct()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadUsersFromContacts()
            } else {
                // Permission denied - show message
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Grant contacts permission to see friends"
            }
        }
    }

    private fun openChat(user: User) {
        try {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("userId", user.uid)
            intent.putExtra("username", user.username)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening chat", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            repository.setOnlineStatus(true)
            // Reload if theme changed
            if (currentTheme != repository.theme) {
                recreate()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            repository.setOnlineStatus(false)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onPause", e)
        }
    }
}
