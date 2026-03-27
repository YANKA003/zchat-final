package com.zchat.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.AppSettings
import com.zchat.app.databinding.ActivitySettingsBinding
import com.zchat.app.ui.MainActivity
import com.zchat.app.ui.premium.PremiumActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository

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
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        setupThemeSpinner()
        setupLanguageSpinner()
        loadSettings()

        // Check if should show premium
        if (intent.getBooleanExtra("show_premium", false)) {
            startActivity(Intent(this, PremiumActivity::class.java))
        }

        binding.btnPremium.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
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
            getString(resources.getIdentifier("lang_${languageCodes[index].replace("-r", "_")}", "string", packageName))
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayLanguages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                repository.language = languageCodes[position]
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
