package com.zchat.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ActivitySettingsBinding
import com.zchat.app.ui.auth.AuthActivity
import com.zchat.app.ui.premium.PremiumActivity
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository
    private var currentUser: User? = null
    private var selectedAvatarUri: Uri? = null

    private val themes = arrayOf("Classic", "Modern", "Neon", "Drawn by a child")
    private val languages = arrayOf(
        "English (US)", "English (UK)", "Français", "Español", "Português",
        "中文", "Беларуская", "Українська", "Русский", "Deutsch"
    )
    private val languageCodes = arrayOf(
        "en", "en-rGB", "fr", "es", "pt", "zh", "be", "uk", "ru", "de"
    )

    private var themeInitialized = false
    private var languageInitialized = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedAvatarUri = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupThemeSpinner()
        setupLanguageSpinner()
        loadSettings()
        loadUserProfile()

        if (intent.getBooleanExtra("show_premium", false)) {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.ivEditAvatar.setOnClickListener { openImagePicker() }
        binding.ivAvatar.setOnClickListener { openImagePicker() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnPremium.setOnClickListener { startActivity(Intent(this, PremiumActivity::class.java)) }
        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error applying language", e)
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                repository.currentUserId?.let { uid ->
                    currentUser = repository.getUser(uid)
                    currentUser?.let { user ->
                        binding.etUsername.setText(user.username)
                        binding.etPhone.setText(user.phoneNumber)

                        if (user.avatarUrl.isNotEmpty()) {
                            Glide.with(this@SettingsActivity)
                                .load(user.avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(binding.ivAvatar)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error loading profile", e)
            }
        }
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val userId = repository.currentUserId ?: return@launch
                val existingUser = currentUser ?: User(uid = userId)

                val updatedUser = existingUser.copy(
                    username = username,
                    phoneNumber = phone,
                    avatarUrl = selectedAvatarUri?.toString() ?: existingUser.avatarUrl
                )

                val result = repository.updateUser(updatedUser)
                result.fold(
                    onSuccess = {
                        repository.saveUserLocally(updatedUser)
                        Toast.makeText(this@SettingsActivity, getString(R.string.success), Toast.LENGTH_SHORT).show()
                        currentUser = updatedUser
                    },
                    onFailure = { e ->
                        Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupThemeSpinner() {
        val displayThemes = arrayOf(
            getString(R.string.theme_classic),
            getString(R.string.theme_modern),
            getString(R.string.theme_neon),
            getString(R.string.theme_childish)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayThemes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapter

        binding.spinnerTheme.setSelection(repository.theme, false)

        binding.spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!themeInitialized) {
                    themeInitialized = true
                    return
                }
                repository.theme = position
                recreate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        val currentLang = repository.language
        val langIndex = languageCodes.indexOf(currentLang)
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
                LanguageHelper.setLanguage(this@SettingsActivity, selectedLang)
                recreate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadSettings() {
        // Premium status
        if (repository.isPremium) {
            binding.btnPremium.text = "${getString(R.string.premium)}: ${repository.premiumType}"
        }
    }

    private fun logout() {
        try {
            repository.logout()
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error during logout", e)
        }
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
