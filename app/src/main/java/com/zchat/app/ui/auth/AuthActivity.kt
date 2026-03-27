package com.zchat.app.ui.auth

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
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

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Инициализация ThemeManager
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
        
        binding.btnLogin.setOnClickListener { if (isLoginMode) login() else toggleMode() }
        binding.btnRegister.setOnClickListener { if (isLoginMode) toggleMode() else register() }
        binding.tvToggleMode.setOnClickListener { toggleMode() }
        
        // Кнопка выбора дизайна
        binding.btnSelectDesign.setOnClickListener { showDesignSelector() }
    }
    
    override fun onResume() {
        super.onResume()
        applyThemeColors()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        
        // Установка цвета status bar
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        // Обновляем UI если binding уже создан
        if (::binding.isInitialized) {
            // Цвет логотипа
            binding.tvLogo.setTextColor(colors.primary.toColorInt())
            
            // Цвет кнопки входа
            val loginBg = GradientDrawable().apply {
                setColor(colors.primary.toColorInt())
                cornerRadius = 28f
            }
            binding.btnLogin.background = loginBg
            
            // Цвет кнопки регистрации (outline)
            binding.btnRegister.setTextColor(colors.primary.toColorInt())
            
            // Цвет текста переключения режима
            binding.tvToggleMode.setTextColor(colors.primary.toColorInt())
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        binding.tilUsername.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.btnLogin.text = if (isLoginMode) "Войти" else "Назад"
        binding.btnRegister.text = if (isLoginMode) "Регистрация" else "Создать аккаунт"
    }
    
    private fun showDesignSelector() {
        DesignSelectorDialog(this) {
            // После выбора дизайна - обновляем UI
            applyThemeColors()
            // Также сохраняем в настройки
            repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
        }.show()
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) { Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show(); return }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.login(email, password)
            binding.progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { 
                    // Сохраняем выбранный дизайн
                    repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java)); 
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
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) { Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show(); return }
        if (password.length < 6) { Toast.makeText(this, "Пароль минимум 6 символов", Toast.LENGTH_SHORT).show(); return }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.register(email, password, username)
            binding.progressBar.visibility = View.GONE
            result.fold(
                onSuccess = { 
                    // Сохраняем выбранный дизайн
                    repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
                    startActivity(Intent(this@AuthActivity, MainActivity::class.java)); 
                    finish() 
                },
                onFailure = { Toast.makeText(this@AuthActivity, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }
}
