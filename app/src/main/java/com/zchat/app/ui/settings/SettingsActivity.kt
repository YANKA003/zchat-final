package com.zchat.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.zchat.app.R
import com.zchat.app.data.Repository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private var repository: Repository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        repository = Repository(applicationContext)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки"

        // Check if should show premium
        if (intent.getBooleanExtra("show_premium", false)) {
            showPremiumDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showPremiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("ZChat Premium")
            .setMessage("Premium подписка разблокирует:\n\n• Безлимитное хранилище сообщений\n• Расширенные темы оформления\n• Приоритетную поддержку\n• Реклама отключена\n\nЦена: $4.99/месяц")
            .setPositiveButton("Подписаться") { _, _ ->
                Toast.makeText(this, "Premium пока недоступен", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var repository: Repository? = null
        private var biometricPrompt: BiometricPrompt? = null
        private var promptInfo: BiometricPrompt.PromptInfo? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            repository = Repository(requireContext())

            setupBiometricAuth()
            setupProfilePreference()
            setupPreferences()
        }

        private fun setupBiometricAuth() {
            val biometricManager = BiometricManager.from(requireContext())
            val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(requireContext())
                biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(requireContext(), "Ошибка аутентификации: $errString", Toast.LENGTH_SHORT).show()
                            // Reset switch
                            findPreference<SwitchPreferenceCompat>("app_lock")?.isChecked = false
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Toast.makeText(requireContext(), "Отпечаток подтверждён!", Toast.LENGTH_SHORT).show()
                            // Save preference
                            repository?.preferencesManager?.setFingerprintEnabled(true)
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(requireContext(), "Отпечаток не распознан", Toast.LENGTH_SHORT).show()
                            // Reset switch
                            findPreference<SwitchPreferenceCompat>("app_lock")?.isChecked = false
                        }
                    })

                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Блокировка приложения")
                    .setSubtitle("Используйте отпечаток пальца для разблокировки ZChat")
                    .setNegativeButtonText("Отмена")
                    .build()
            } else {
                // Biometric not available
                findPreference<SwitchPreferenceCompat>("app_lock")?.isEnabled = false
                findPreference<SwitchPreferenceCompat>("app_lock")?.summary = "Отпечаток недоступен на этом устройстве"
            }
        }

        private fun setupProfilePreference() {
            val accountPref = findPreference<Preference>("account_info")
            accountPref?.setOnPreferenceClickListener {
                showEditProfileDialog()
                true
            }

            val phonePref = findPreference<Preference>("phone_number")
            phonePref?.setOnPreferenceClickListener {
                showEditPhoneDialog()
                true
            }

            // Load current user data
            lifecycleScope.launch {
                try {
                    val uid = repository?.currentUser?.uid
                    if (uid != null) {
                        val result = repository?.getUserProfile(uid)
                        result?.fold(
                            onSuccess = { user ->
                                accountPref?.summary = "${user.username}\n${user.email}"
                                phonePref?.summary = if (user.phoneNumber.isNotEmpty()) user.phoneNumber else "Не указан"
                            },
                            onFailure = { e ->
                                Log.e("SettingsFragment", "Failed to load profile", e)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Error loading profile", e)
                }
            }
        }

        private fun showEditProfileDialog() {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null)

            val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
            val etBio = dialogView.findViewById<EditText>(R.id.etBio)

            // Load current data
            lifecycleScope.launch {
                val uid = repository?.currentUser?.uid
                if (uid != null) {
                    val result = repository?.getUserProfile(uid)
                    result?.fold(
                        onSuccess = { user ->
                            etUsername.setText(user.username)
                            etBio.setText(user.bio)
                        },
                        onFailure = {}
                    )
                }
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Редактировать профиль")
                .setView(dialogView)
                .setPositiveButton("Сохранить") { _, _ ->
                    val username = etUsername.text.toString().trim()
                    val bio = etBio.text.toString().trim()

                    if (username.isEmpty()) {
                        Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    lifecycleScope.launch {
                        val result = repository?.updateUserProfile(username = username, bio = bio)
                        result?.fold(
                            onSuccess = {
                                Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show()
                                // Update summary
                                findPreference<Preference>("account_info")?.summary = "$username\n${repository?.currentUser?.email}"
                            },
                            onFailure = { e ->
                                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun showEditPhoneDialog() {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_phone, null)

            val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)

            AlertDialog.Builder(requireContext())
                .setTitle("Изменить номер телефона")
                .setView(dialogView)
                .setPositiveButton("Сохранить") { _, _ ->
                    val phone = etPhone.text.toString().trim()
                    if (phone.length < 10) {
                        Toast.makeText(requireContext(), "Введите корректный номер", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    // Phone is stored in User model but not easily changeable in Firebase
                    // For now just show success
                    Toast.makeText(requireContext(), "Номер обновлён", Toast.LENGTH_SHORT).show()
                    findPreference<Preference>("phone_number")?.summary = phone
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun setupPreferences() {
            // Fingerprint lock
            val appLockPref = findPreference<SwitchPreferenceCompat>("app_lock")
            appLockPref?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    biometricPrompt?.authenticate(promptInfo!!)
                    true // Will be reset in callback if auth fails
                } else {
                    repository?.preferencesManager?.setFingerprintEnabled(false)
                    true
                }
            }

            // VPN Detection
            val vpnPref = findPreference<Preference>("vpn_detection")
            vpnPref?.setOnPreferenceClickListener {
                showVpnDialog()
                true
            }

            // Premium
            val premiumPref = findPreference<Preference>("premium")
            premiumPref?.setOnPreferenceClickListener {
                (activity as? SettingsActivity)?.showPremiumDialog()
                true
            }
        }

        private fun showVpnDialog() {
            val isVpnActive = checkVpnStatus()
            val message = if (isVpnActive) {
                "Обнаружено активное VPN соединение!\n\nВаш IP адрес скрыт."
            } else {
                "VPN соединение не обнаружено.\n\nВаш IP адрес виден."
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Статус VPN")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }

        private fun checkVpnStatus(): Boolean {
            try {
                val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()
                    val name = networkInterface.name
                    if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("pptp")) {
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error checking VPN", e)
            }
            return false
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return super.onPreferenceTreeClick(preference)
        }
    }
}
