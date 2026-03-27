package com.zchat.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
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
import com.zchat.app.ui.theme.ThemeManager
import com.zchat.app.utils.ContactsHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter
    private var currentDesign = ThemeManager.DESIGN_CLASSIC
    private val CONTACTS_PERMISSION_REQUEST = 100
    
    // For alternative layouts
    private var alternativeLayout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация ThemeManager
        ThemeManager.init(applicationContext)
        currentDesign = ThemeManager.getDesign()
        
        super.onCreate(savedInstanceState)
        repository = Repository(applicationContext)
        
        if (repository.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        
        // Выбираем макет в зависимости от дизайна
        when (currentDesign) {
            ThemeManager.DESIGN_NEON -> setupNeonLayout()
            ThemeManager.DESIGN_CHILD -> setupChildLayout()
            else -> setupClassicLayout()
        }
        
        adapter = UsersAdapter { user -> openChat(user) }
        
        // Находим RecyclerView в зависимости от макета
        val rvUsers = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvUsers)
        rvUsers?.layoutManager = LinearLayoutManager(this)
        rvUsers?.adapter = adapter
        
        applyThemeColors()
        
        // Автоматически проверяем контакты при запуске
        checkContactsPermissionAndLoad()
    }
    
    private fun setupClassicLayout() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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
    }
    
    private fun setupNeonLayout() {
        alternativeLayout = layoutInflater.inflate(R.layout.activity_main_neon, null)
        setContentView(alternativeLayout)
        
        // Settings button in header (circle)
        findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Bottom navigation
        setupBottomNavigation()
    }
    
    private fun setupChildLayout() {
        alternativeLayout = layoutInflater.inflate(R.layout.activity_main_child, null)
        setContentView(alternativeLayout)
        
        // Settings button (crayon style)
        findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Bottom navigation
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navChats)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.chats), Toast.LENGTH_SHORT).show()
        }
        
        findViewById<LinearLayout>(R.id.navCalls)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.calls), Toast.LENGTH_SHORT).show()
        }
        
        findViewById<LinearLayout>(R.id.navChannels)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.channels), Toast.LENGTH_SHORT).show()
        }
        
        findViewById<LinearLayout>(R.id.navContacts)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.contacts), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Проверяем, изменился ли дизайн
        if (currentDesign != ThemeManager.getDesign()) {
            recreate()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadUsersFromContacts()
            } else {
                loadAllUsers()
            }
        }
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (currentDesign == ThemeManager.DESIGN_CLASSIC && ::binding.isInitialized) {
            binding.toolbar.setBackgroundColor(colors.primary.toColorInt())
            binding.toolbar.setTitleTextColor(colors.sentMessageText.toColorInt())
        }
    }
    
    private fun checkContactsPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadUsersFromContacts()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }
    
    private fun getPhoneContacts(): List<String> {
        val phones = mutableListOf<String>()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIndex)
                    val normalized = number.replace(Regex("[^0-9]"), "")
                    if (normalized.length >= 10) {
                        phones.add(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return phones.distinct()
    }

    private fun loadUsersFromContacts() {
        showLoading(true)
        
        val contactPhones = getPhoneContacts()
        
        lifecycleScope.launch {
            val result = repository.searchUsers("")
            showLoading(false)
            
            result.fold(
                onSuccess = { allUsers ->
                    val currentUid = repository.currentUser?.uid
                    
                    val contactsInApp = allUsers.filter { user ->
                        user.uid != currentUid && 
                        user.phoneNumber.isNotEmpty() &&
                        contactPhones.any { contactPhone ->
                            val userPhone = user.phoneNumber.replace(Regex("[^0-9]"), "")
                            userPhone.length >= 10 && contactPhone.length >= 10 &&
                            userPhone.takeLast(10) == contactPhone.takeLast(10)
                        }
                    }
                    
                    if (contactsInApp.isEmpty()) {
                        showEmpty(getString(R.string.searching_friends))
                        adapter.submitList(allUsers.filter { it.uid != currentUid })
                    } else {
                        adapter.submitList(contactsInApp)
                        showEmpty(false)
                    }
                },
                onFailure = { 
                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                    showEmpty("Ошибка загрузки")
                }
            )
        }
    }
    
    private fun loadAllUsers() {
        showLoading(true)
        
        lifecycleScope.launch {
            val result = repository.searchUsers("")
            showLoading(false)
            result.fold(
                onSuccess = { users ->
                    val filtered = users.filter { it.uid != repository.currentUser?.uid }
                    adapter.submitList(filtered)
                    showEmpty(if (filtered.isEmpty()) "Пока нет других пользователей" else false)
                },
                onFailure = { 
                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                    showEmpty("Ошибка загрузки")
                }
            )
        }
    }
    
    private fun showLoading(show: Boolean) {
        findViewById<ProgressBar>(R.id.progressBar)?.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showEmpty(message: Any) {
        val emptyState = findViewById<LinearLayout>(R.id.emptyState)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        
        when (message) {
            is String -> {
                tvEmpty?.text = message
                emptyState?.visibility = View.VISIBLE
            }
            is Boolean -> {
                emptyState?.visibility = if (message) View.VISIBLE else View.GONE
            }
        }
    }

    private fun openChat(user: User) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("userId", user.uid)
        intent.putExtra("username", user.username)
        startActivity(intent)
    }
}
