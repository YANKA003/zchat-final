package com.zchat.app.ui.settings

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivitySettingsBinding
import com.zchat.app.ui.theme.DesignSelectorDialog
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository
    private var needsRecreate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        binding.toolbar.setNavigationOnClickListener { 
            if (needsRecreate) {
                // Возвращаем результат для перезапуска MainActivity
                setResult(RESULT_OK)
            }
            finish() 
        }
        
        lifecycleScope.launch {
            val isVpn = checkVpnStatus()
            binding.tvVpnStatus.text = if (isVpn) "VPN: Подключён" else "VPN: Не подключён"
        }
        
        binding.cardPremium.setOnClickListener { showPremiumDialog() }
        binding.btnGetPremium.setOnClickListener { showPremiumDialog() }
        binding.btnAccount.setOnClickListener { showAccountDialog() }
        binding.btnPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.btnChatSettings.setOnClickListener { showThemeDialog() }
        binding.btnNotifications.setOnClickListener { showNotificationsDialog() }
        binding.btnBattery.setOnClickListener { showBatteryDialog() }
        binding.btnDesign.setOnClickListener { showDesignSelector() }
        
        binding.switchCallRecording.setOnCheckedChangeListener { _, isChecked ->
            repository.preferencesManager.setCallRecordingEnabled(isChecked)
        }
        binding.switchCallRecording.isChecked = repository.preferencesManager.isCallRecordingEnabled()
    }
    
    override fun onResume() {
        super.onResume()
        applyThemeColors()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (::binding.isInitialized) {
            binding.toolbar.setBackgroundColor(colors.primary.toColorInt())
        }
    }
    
    private fun showDesignSelector() {
        DesignSelectorDialog(this) {
            needsRecreate = true
            applyThemeColors()
            repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
            // Показываем сообщение о необходимости перезапуска
            Toast.makeText(this, "Дизайн изменён. Перезапустите приложение для полного применения.", Toast.LENGTH_LONG).show()
        }.show()
    }

    private suspend fun checkVpnStatus(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun showPremiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("⭐ ZChat Premium")
            .setMessage("Автоперевод сообщений\nСмена иконки приложения\nЭксклюзивные функции\n\n299 ₽/месяц")
            .setPositiveButton("Оформить") { _, _ ->
                repository.preferencesManager.updatePremium(true)
                Toast.makeText(this, "Premium активирован! (демо)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Учётная запись")
            .setMessage("Email: ${repository.currentUser?.email}\n\nВы можете изменить имя и описание в профиле.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyDialog() {
        val settings = repository.preferencesManager.settings.value
        val items = arrayOf("Показывать статус онлайн", "Блокировка приложения")
        val checked = booleanArrayOf(settings.showOnlineStatus, settings.appLockEnabled)
        AlertDialog.Builder(this)
            .setTitle("Приватность")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                when (which) {
                    0 -> repository.preferencesManager.updateOnlineStatus(isChecked)
                    1 -> repository.preferencesManager.updateAppLock(isChecked)
                }
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showThemeDialog() {
        val items = arrayOf("Светлая тема", "Тёмная тема", "Системная")
        AlertDialog.Builder(this)
            .setTitle("Тема оформления")
            .setSingleChoiceItems(items, repository.preferencesManager.settings.value.theme) { _, which ->
                repository.preferencesManager.updateTheme(which)
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showNotificationsDialog() {
        val settings = repository.preferencesManager.settings.value
        val items = arrayOf("Уведомления", "Озвучивать имя звонящего")
        val checked = booleanArrayOf(settings.notificationsEnabled, settings.announceCallerName)
        AlertDialog.Builder(this)
            .setTitle("Уведомления")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                when (which) {
                    0 -> repository.preferencesManager.updateNotifications(isChecked)
                    1 -> repository.preferencesManager.updateAnnounceCaller(isChecked)
                }
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBatteryDialog() {
        val items = arrayOf("Выключен", "Всегда включён", "При заряде < 30%")
        AlertDialog.Builder(this)
            .setTitle("Экономия энергии")
            .setSingleChoiceItems(items, repository.preferencesManager.settings.value.batterySaverMode) { _, which ->
                repository.preferencesManager.updateBatterySaverMode(which)
            }
            .setMessage("В режиме экономии отключаются анимации.")
            .setPositiveButton("OK", null)
            .show()
    }
}
