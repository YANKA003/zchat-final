package com.zchat.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var adapter: UsersAdapter
    private var currentDesign = ThemeManager.DESIGN_CLASSIC
    private val CONTACTS_PERMISSION_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация ThemeManager
        ThemeManager.init(applicationContext)
        currentDesign = ThemeManager.getDesign()
        applyThemeColors()
        
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
        
        // Автоматически проверяем контакты при запуске
        checkContactsPermissionAndLoad()
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
                // Если разрешение не дано, загружаем всех пользователей
                loadAllUsers()
            }
        }
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (::binding.isInitialized) {
            binding.toolbar.setBackgroundColor(colors.primary.toColorInt())
            binding.toolbar.setTitleTextColor(colors.sentMessageText.toColorInt())
        }
    }
    
    private fun checkContactsPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            loadUsersFromContacts()
        } else {
            // Запрашиваем разрешение автоматически
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }
    
    /**
     * Получает номера телефонов из телефонной книги устройства
     */
    private fun getPhoneContacts(): List<String> {
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
                    // Нормализуем номер: оставляем только цифры
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
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        
        // Получаем контакты из телефонной книги
        val contactPhones = getPhoneContacts()
        
        lifecycleScope.launch {
            val result = repository.searchUsers("")
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { allUsers ->
                    val currentUid = repository.currentUser?.uid
                    
                    // Фильтруем пользователей: показываем тех, чей номер есть в контактах
                    val contactsInZChat = allUsers.filter { user ->
                        user.uid != currentUid && 
                        user.phoneNumber.isNotEmpty() &&
                        contactPhones.any { contactPhone ->
                            // Сравниваем последние 10 цифр (без кода страны)
                            val userPhone = user.phoneNumber.replace(Regex("[^0-9]"), "")
                            userPhone.length >= 10 && contactPhone.length >= 10 &&
                            userPhone.takeLast(10) == contactPhone.takeLast(10)
                        }
                    }
                    
                    if (contactsInZChat.isEmpty()) {
                        // Если никто из контактов не в ZChat
                        binding.tvEmpty.text = "Никто из ваших контактов\nещё не зарегистрирован в ZChat\n\nПригласите друзей!"
                        binding.emptyState.visibility = View.VISIBLE
                        // Показываем всех пользователей как fallback
                        adapter.submitList(allUsers.filter { it.uid != currentUid })
                    } else {
                        adapter.submitList(contactsInZChat)
                        binding.emptyState.visibility = View.GONE
                    }
                },
                onFailure = { 
                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                    binding.tvEmpty.text = "Ошибка загрузки"
                    binding.emptyState.visibility = View.VISIBLE
                }
            )
        }
    }
    
    private fun loadAllUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = repository.searchUsers("")
            binding.progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { users ->
                    val filtered = users.filter { it.uid != repository.currentUser?.uid }
                    adapter.submitList(filtered)
                    if (filtered.isEmpty()) {
                        binding.tvEmpty.text = "Пока нет других пользователей"
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.emptyState.visibility = View.GONE
                    }
                },
                onFailure = { 
                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                    binding.tvEmpty.text = "Ошибка загрузки"
                    binding.emptyState.visibility = View.VISIBLE
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
}
