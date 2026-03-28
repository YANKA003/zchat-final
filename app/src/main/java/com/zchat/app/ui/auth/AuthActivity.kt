package com.zchat.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityAuthBinding
import com.zchat.app.ui.MainActivity
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: Repository
    private var isLoginMode = true

    private var languageInitialized = false
    private var designInitialized = false

    private val themes = arrayOf("Classic", "Modern", "Neon", "Drawn by a child")
    private val languages = arrayOf(
        "English (US)", "English (UK)", "Français", "Español", "Português",
        "中文", "Беларуская", "Українська", "Русский", "Deutsch"
    )
    private val languageCodes = arrayOf(
        "en", "en-rGB", "fr", "es", "pt", "zh", "be", "uk", "ru", "de"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language BEFORE super.onCreate
        applyLanguage()

        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            repository = Repository(applicationContext)
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error initializing repository", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // Check if already logged in
        try {
            if (repository.currentUser != null) {
                Log.d("AuthActivity", "User already logged in, going to MainActivity")
                goToMainActivity()
                return
            }
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error checking current user", e)
        }

        setupSpinners()
        setMode(true)

        binding.btnLogin.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                setMode(true)
            }
        }

        binding.btnRegister.setOnClickListener {
            if (!isLoginMode) {
                performRegister()
            } else {
                setMode(false)
            }
        }
    }

    private fun goToMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error going to MainActivity", e)
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyLanguage() {
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val language = prefs.getString("language", "en") ?: "en"
            LanguageHelper.setLanguage(this, language)
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error applying language", e)
        }
    }

    private fun setupSpinners() {
        // Language spinner
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = langAdapter

        val savedLang = repository.language
        val langIndex = languageCodes.indexOf(savedLang)
        if (langIndex >= 0) {
            binding.spinnerLanguage.setSelection(langIndex, false)
        }

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!languageInitialized) {
                    languageInitialized = true
                    return
                }
                val selectedLang = languageCodes[position]
                repository.language = selectedLang
                // Apply language immediately
                LanguageHelper.setLanguage(this@AuthActivity, selectedLang)
                // Recreate to apply new language
                recreate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Design spinner
        val themeNames = arrayOf(
            getString(R.string.theme_classic),
            getString(R.string.theme_modern),
            getString(R.string.theme_neon),
            getString(R.string.theme_childish)
        )
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeNames)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDesign.adapter = themeAdapter

        binding.spinnerDesign.setSelection(repository.theme, false)

        binding.spinnerDesign.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!designInitialized) {
                    designInitialized = true
                    return
                }
                repository.theme = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setMode(loginMode: Boolean) {
        isLoginMode = loginMode

        binding.tvTitle.text = if (loginMode) getString(R.string.login) else getString(R.string.register)
        binding.btnLogin.text = getString(R.string.login)
        binding.btnRegister.text = getString(R.string.register)

        val visibility = if (loginMode) View.GONE else View.VISIBLE

        binding.tilUsername.visibility = visibility
        binding.tilPhone.visibility = visibility
        binding.tilConfirmPassword.visibility = visibility
        binding.tvDesignLabel.visibility = visibility
        binding.spinnerDesign.visibility = visibility
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = repository.login(email, password)
                showLoading(false)

                result.fold(
                    onSuccess = {
                        Log.d("AuthActivity", "Login successful")
                        try {
                            repository.setOnlineStatus(true)
                        } catch (e: Exception) {
                            Log.e("AuthActivity", "Error setting online status", e)
                        }
                        goToMainActivity()
                    },
                    onFailure = { e ->
                        Log.e("AuthActivity", "Login failed", e)
                        Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Log.e("AuthActivity", "Login exception", e)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        // Validation
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Phone is required for contact matching
        if (phone.isEmpty()) {
            Toast.makeText(this, getString(R.string.phone_number) + " is required", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d("AuthActivity", "Starting registration for $email")
                val result = repository.registerWithPhone(email, password, username, phone)
                showLoading(false)

                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthActivity", "Registration successful: ${user.uid}")
                        try {
                            repository.setOnlineStatus(true)
                        } catch (e: Exception) {
                            Log.e("AuthActivity", "Error setting online status", e)
                        }
                        goToMainActivity()
                    },
                    onFailure = { e ->
                        Log.e("AuthActivity", "Registration failed", e)
                        Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Log.e("AuthActivity", "Registration exception", e)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
        binding.etEmail.isEnabled = !show
        binding.etPassword.isEnabled = !show
        binding.etUsername.isEnabled = !show
        binding.etPhone.isEnabled = !show
        binding.etConfirmPassword.isEnabled = !show
    }
}
