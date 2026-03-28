package com.zchat.app.ui.auth

import android.content.Intent
import android.os.Bundle
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
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        // Check if already logged in
        if (repository.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupSpinners()
        setMode(true) // Start in login mode

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
                repository.language = languageCodes[position]
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

        // Update title
        binding.tvTitle.text = if (loginMode) getString(R.string.login) else getString(R.string.register)

        // Update buttons
        binding.btnLogin.text = getString(R.string.login)
        binding.btnRegister.text = getString(R.string.register)

        // Show/hide registration fields
        val visibility = if (loginMode) View.GONE else View.VISIBLE

        binding.tilUsername.visibility = visibility
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
                        repository.setOnlineStatus(true)
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = repository.register(email, password, username)
                showLoading(false)

                result.fold(
                    onSuccess = {
                        repository.setOnlineStatus(true)
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@AuthActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        binding.etConfirmPassword.isEnabled = !show
    }
}
