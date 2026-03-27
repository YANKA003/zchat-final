package com.zchat.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter
    private var currentTheme = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)

        if (repository.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        currentTheme = repository.theme

        setupToolbar()
        setupNavigation()
        setupBottomNavigation()
        setupUserList()
        loadUsers()
    }

    private fun applyTheme() {
        repository = Repository(applicationContext)
        when (repository.theme) {
            0 -> setTheme(R.style.Theme_GOODOK_Classic)
            1 -> setTheme(R.style.Theme_GOODOK_Modern)
            2 -> setTheme(R.style.Theme_GOODOK_Neon)
            3 -> setTheme(R.style.Theme_GOODOK_Childish)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_manage)
        }
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_premium -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.putExtra("show_premium", true)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    repository.logout()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
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
                when (item.itemId) {
                    R.id.nav_chats -> {
                        // Already on chats
                        true
                    }
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
        lifecycleScope.launch {
            val result = repository.searchUsers("")
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { users ->
                    val filtered = users.filter { it.uid != repository.currentUser?.uid }
                    adapter.submitList(filtered)
                    binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                },
                onFailure = { e ->
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun openChat(user: User) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userId", user.uid)
        intent.putExtra("username", user.username)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        repository.setOnlineStatus(true)
        // Reload if theme changed
        if (currentTheme != repository.theme) {
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        repository.setOnlineStatus(false)
    }
}
