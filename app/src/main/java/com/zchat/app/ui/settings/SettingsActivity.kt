package com.zchat.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.zchat.app.data.model.AppSettings
import com.zchat.app.data.model.User
import com.zchat.app.databinding.ActivitySettingsBinding
import com.zchat.app.ui.MainActivity
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
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        setupThemeSpinner()
        setupLanguageSpinner()
        loadSettings()
        loadUserProfile()

        // Check if should show premium
        if (intent.getBooleanExtra("show_premium", false)) {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.btnPremium.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.ivEditAvatar.setOnClickListener {
            openImagePicker()
        }

        binding.ivAvatar.setOnClickListener {
            openImagePicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            repository.currentUserId?.let { uid ->
                currentUser = repository.getUser(uid)
                currentUser?.let { user ->
                    binding.etUsername.setText(user.username)
                    binding.etPhone.setText(user.phoneNumber)

                    // Load avatar
                    if (user.avatarUrl.isNotEmpty()) {
                        Glide.with(this@SettingsActivity)
                            .load(user.avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(binding.ivAvatar)
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
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
                    // Save to local prefs for quick access
                    repository.saveUserLocally(updatedUser)
                    Toast.makeText(this@SettingsActivity, R.string.success, Toast.LENGTH_SHORT).show()
                    currentUser = updatedUser
                },
                onFailure = { e ->
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupThemeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes.map {
            when (it) {
                "Classic" -> getString(R.string.theme_classic)
                "Modern" -> getString(R.string.theme_modern)
                "Neon" -> getString(R.string.theme_neon)
                else -> getString(R.string.theme_childish)
            }
        })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = adapter

        binding.spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                repository.theme = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupLanguageSpinner() {
        val displayLanguages = languages.mapIndexed { index, _ ->
            try {
                getString(resources.getIdentifier("lang_${languageCodes[index].replace("-r", "_")}", "string", packageName))
            } catch (e: Exception) {
                languages[index]
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayLanguages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languageCodes[position]
                if (selectedLang != repository.language) {
                    repository.language = selectedLang
                    LanguageHelper.setLanguage(this@SettingsActivity, selectedLang)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadSettings() {
        binding.spinnerTheme.setSelection(repository.theme)

        val langIndex = languageCodes.indexOf(repository.language)
        if (langIndex >= 0) {
            binding.spinnerLanguage.setSelection(langIndex)
        }

        binding.switchOnlineStatus.isChecked = repository.getSettings().showOnlineStatus
        binding.switchNotifications.isChecked = repository.getSettings().notificationsEnabled

        binding.switchOnlineStatus.setOnCheckedChangeListener { _, isChecked ->
            val settings = repository.getSettings()
            repository.saveSettings(settings.copy(showOnlineStatus = isChecked))
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val settings = repository.getSettings()
            repository.saveSettings(settings.copy(notificationsEnabled = isChecked))
        }

        // Premium status
        if (repository.isPremium) {
            binding.btnPremium.text = "${getString(R.string.premium)}: ${repository.premiumType}"
        }
    }

    private fun logout() {
        repository.logout()
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
