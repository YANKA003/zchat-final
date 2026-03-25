package com.zchat.app.ui

import.Manifest
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
import com.zchat.app.data.model.Call
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ActivityMainBinding
import com.zchat.app.ui.auth.AuthActivity
import com.zchat.app.ui.calls.CallActivity
import com.zchat.app.ui.chats.ChatActivity
import com.zchat.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var repository: Repository? = null
    private lateinit var adapter: UsersAdapter
    private val CONTACTS_PERMISSION_REQUEST = 100
    private val NOTIFICATION_PERMISSION_REQUEST = 101

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
            checkPermissions()
            checkIntentExtras()

            // Observe incoming calls
            observeIncomingCalls()
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
                checkPermissions()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "UI setup error", e)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                CONTACTS_PERMISSION_REQUEST
            )
        } else {
            loadContactUsers()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
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
                    val normalized = number.replace(Regex("[^0-9]"), "")
                    if (normalized.isNotEmpty()) {
                        phones.add(normalized)
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
                val contactPhones = getPhoneContacts()
                Log.d("MainActivity", "Found ${contactPhones.size} phone numbers in contacts")

                val result = repository?.searchUsers("")
                binding.progressBar.visibility = View.GONE

                result?.fold(
                    onSuccess = { users ->
                        val currentUid = repository?.currentUser?.uid

                        val filtered = users.filter { user ->
                            if (user.uid == currentUid) {
                                false
                            } else {
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

    private fun observeIncomingCalls() {
        val uid = repository?.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                repository?.observeIncomingCalls(uid)?.collect { call ->
                    showIncomingCall(call)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Observe calls error", e)
            }
        }
    }

    private fun showIncomingCall(call: Call) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("callId", call.id)
            putExtra("callerId", call.callerId)
            putExtra("callerName", call.callerName)
            putExtra("isCaller", false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun checkIntentExtras() {
        // Handle notification tap
        if (intent.getBooleanExtra("openChat", false)) {
            val userId = intent.getStringExtra("userId")
            val username = intent.getStringExtra("username")
            if (userId != null && username != null) {
                val chatIntent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("username", username)
                }
                startActivity(chatIntent)
            }
        }

        // Handle incoming call notification
        if (intent.getBooleanExtra("incomingCall", false)) {
            val callerId = intent.getStringExtra("callerId")
            val callerName = intent.getStringExtra("callerName")
            if (callerId != null && callerName != null) {
                val callIntent = Intent(this, CallActivity::class.java).apply {
                    putExtra("callerId", callerId)
                    putExtra("callerName", callerName)
                    putExtra("isCaller", false)
                }
                startActivity(callIntent)
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
