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
import com.zchat.app.ui.chats.ChatActivity
import com.zchat.app.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        if (repository.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_manage)
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_premium -> { val i = Intent(this, SettingsActivity::class.java); i.putExtra("show_premium", true); startActivity(i) }
                R.id.nav_logout -> { repository.logout(); startActivity(Intent(this, AuthActivity::class.java)); finish() }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        
        adapter = UsersAdapter { user -> openChat(user) }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
        
        loadUsers()
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
                onFailure = { Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun openChat(user: User) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userId", user.uid)
        intent.putExtra("username", user.username)
        startActivity(intent)
    }
}
