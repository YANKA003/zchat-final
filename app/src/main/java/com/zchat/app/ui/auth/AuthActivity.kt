package com.zchat.app.ui.auth

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityAuthBinding
import com.zchat.app.ui.MainActivity
import com.zchat.app.ui.theme.DesignSelectorDialog
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.util.Locale

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true
    private var selectedLanguage = "ru"

    private val languages = listOf(
        "en" to "English (US)",
        "en-gb" to "English (UK)",
        "fr" to "Français",
        "es" to "Español",
        "pt" to "Português",
        "zh" to "中文",
        "be" to "Беларуская",
        "uk" to "Українська",
        "ru" to "Русский",
        "de" to "Deutsch"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        if (repository.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setupLanguageSelection()
        
        binding.btnLogin.setOnClickListener { if (isLoginMode) login() else toggleMode() }
        binding.btnRegister.setOnClickListener { if (isLoginMode) toggleMode() else register() }
        binding.tvToggleMode.setOnClickListener { toggleMode() }
        binding.btnSelectDesign.setOnClickListener { showDesignSelector() }
    }
    
    private fun setupLanguageSelection() {
        // Load saved language
        selectedLanguage = repository.preferencesManager.settings.value.language
        updateLanguageDisplay()
        
        binding.llLanguage.setOnClickListener {
            showLanguageSelector()
        }
    }
    
    private fun showLanguageSelector() {
        val languageNames = languages.map { it.second }.toTypedArray()
        val currentIndex = languages.indexOfFirst { it.first == selectedLanguage }.coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle("Выберите язык / Select Language")
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                selectedLanguage = languages[which].first
                updateLanguageDisplay()
                repository.preferencesManager.updateLanguage(selectedLanguage)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun updateLanguageDisplay() {
        val langName = languages.find { it.first == selectedLanguage }?.second ?: "Русский"
        binding.tvSelectedLanguage.text = langName
    }
    
    override fun onResume() {
        super.onResume()
        applyThemeColors()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (::binding.isInitialized) {
            binding.tvLogo.setTextColor(colors.primary.toColorInt())
            
            val loginBg = GradientDrawable().apply {
                setColor(colors.primary.toColorInt())
                cornerRadius = 28f
            }
            binding.btnLogin.background = loginBg
            
            binding.btnRegister.setTextColor(colors.primary.toColorInt())
            binding.tvToggleMode.setTextColor(colors.primary.toColorInt())
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        binding.tilUsername.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.tilPhone.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.btnLogin.text = if (isLoginMode) "Войти" else "Назад"
        binding.btnRegister.text = if (isLoginMode) "Регистрация" else "Создать аккаунт"
    }
    
    private fun showDesignSelector() {
        DesignSelectorDialog(this) {
            applyThemeColors()
            repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
        }.show()
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) { 
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return 
        }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.login(email, password)
            binding.progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { 
                    repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
                    repository.preferencesManager.updateLanguage(selectedLanguage)
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish() 
                },
                onFailure = { Toast.makeText(this@AuthActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun register() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) { 
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return 
        }
        if (password.length < 6) { 
            Toast.makeText(this, "Пароль минимум 6 символов", Toast.LENGTH_SHORT).show()
            return 
        }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.register(email, password, username, phone)
            binding.progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { 
                    repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
                    repository.preferencesManager.updateLanguage(selectedLanguage)
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                    finish() 
                },
                onFailure = { Toast.makeText(this@AuthActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }
}
