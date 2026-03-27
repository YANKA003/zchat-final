package com.zchat.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: Repository
    private var needsRecreate = false
    private var currentPhotoUri: Uri? = null
    
    private val REQUEST_CAMERA = 1001
    private val REQUEST_GALLERY = 1002
    private val REQUEST_CAMERA_PERMISSION = 1003

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        binding.toolbar.setNavigationOnClickListener { 
            if (needsRecreate) {
                setResult(RESULT_OK)
            }
            finish() 
        }
        
        setupProfileCard()
        setupClickListeners()
        
        binding.switchCallRecording.setOnCheckedChangeListener { _, isChecked ->
            repository.preferencesManager.setCallRecordingEnabled(isChecked)
        }
        binding.switchCallRecording.isChecked = repository.preferencesManager.isCallRecordingEnabled()
    }
    
    override fun onResume() {
        super.onResume()
        applyThemeColors()
        updateProfileInfo()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
        
        if (::binding.isInitialized) {
            binding.toolbar.setBackgroundColor(colors.primary.toColorInt())
            
            // Применяем градиент к заголовку профиля
            val gradientBg = when (ThemeManager.getDesign()) {
                ThemeManager.DESIGN_NEON -> R.drawable.bg_profile_header
                ThemeManager.DESIGN_MODERN -> R.drawable.bg_profile_header
                else -> R.drawable.bg_profile_header
            }
            binding.profileHeader.setBackgroundResource(gradientBg)
        }
    }
    
    private fun setupProfileCard() {
        updateProfileInfo()
        
        binding.btnEditPhoto.setOnClickListener {
            showPhotoOptionsDialog()
        }
        
        binding.ivAvatar.setOnClickListener {
            showPhotoOptionsDialog()
        }
        
        binding.btnSetStatus.setOnClickListener {
            showStatusDialog()
        }
    }
    
    private fun updateProfileInfo() {
        val user = repository.currentUser
        binding.tvProfileName.text = user?.displayName ?: "Пользователь"
        
        // Получаем телефон из базы
        lifecycleScope.launch {
            val profile = repository.getUserProfile(user?.uid ?: "")
            profile.fold(
                onSuccess = { u ->
                    binding.tvProfilePhone.text = u.phoneNumber.ifEmpty { "Не указан" }
                },
                onFailure = {
                    binding.tvProfilePhone.text = "Не указан"
                }
            )
        }
        
        // Обновляем отображение текущего дизайна
        val designName = when (ThemeManager.getDesign()) {
            ThemeManager.DESIGN_MODERN -> "Современный"
            ThemeManager.DESIGN_NEON -> "Neon"
            ThemeManager.DESIGN_CHILD -> "Drawn by a child"
            else -> "Классический"
        }
        binding.tvCurrentDesign.text = designName
        
        // Обновляем язык
        val lang = repository.preferencesManager.settings.value.language
        binding.tvCurrentLanguage.text = getLanguageName(lang)
    }
    
    private fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "English (US)"
            "en-gb" -> "English (UK)"
            "fr" -> "Français"
            "es" -> "Español"
            "pt" -> "Português"
            "zh" -> "中文"
            "be" -> "Беларуская"
            "uk" -> "Українська"
            "ru" -> "Русский"
            "de" -> "Deutsch"
            else -> "Русский"
        }
    }
    
    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Сфотографировать", "Выбрать из галереи", "Удалить фото")
        AlertDialog.Builder(this)
            .setTitle("Фото профиля")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> removePhoto()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }
        
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Profile Photo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "GOODOK Profile Photo")
        currentPhotoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
        startActivityForResult(intent, REQUEST_CAMERA)
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY)
    }
    
    private fun removePhoto() {
        binding.ivAvatar.setImageResource(android.R.drawable.ic_menu_camera)
        Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    currentPhotoUri?.let { uri ->
                        val bitmap = loadBitmap(uri)
                        bitmap?.let {
                            binding.ivAvatar.setImageBitmap(it)
                            Toast.makeText(this, "Фото обновлено", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                REQUEST_GALLERY -> {
                    data?.data?.let { uri ->
                        val bitmap = loadBitmap(uri)
                        bitmap?.let {
                            binding.ivAvatar.setImageBitmap(it)
                            Toast.makeText(this, "Фото обновлено", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    
    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun showStatusDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Ваш статус..."
        input.setSingleLine()
        
        AlertDialog.Builder(this)
            .setTitle("Установить статус")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val status = input.text.toString()
                Toast.makeText(this, "Статус: $status", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun setupClickListeners() {
        binding.cardPremium.setOnClickListener { showPremiumDialog() }
        binding.btnGetPremium.setOnClickListener { showPremiumDialog() }
        binding.btnAccount.setOnClickListener { showAccountDialog() }
        binding.btnPrivacy.setOnClickListener { showPrivacyDialog() }
        binding.btnChatSettings.setOnClickListener { showThemeDialog() }
        binding.btnNotifications.setOnClickListener { showNotificationsDialog() }
        binding.btnDesign.setOnClickListener { showDesignSelector() }
        binding.btnLanguage.setOnClickListener { showLanguageSelector() }
    }
    
    private fun showDesignSelector() {
        DesignSelectorDialog(this) {
            needsRecreate = true
            applyThemeColors()
            repository.preferencesManager.updateDesignStyle(ThemeManager.getDesign())
            updateProfileInfo()
            Toast.makeText(this, "Дизайн изменён", Toast.LENGTH_SHORT).show()
        }.show()
    }
    
    private fun showLanguageSelector() {
        val languages = arrayOf(
            "English (US)", "English (UK)", "Français", "Español", "Português",
            "中文", "Беларуская", "Українська", "Русский", "Deutsch"
        )
        val codes = arrayOf("en", "en-gb", "fr", "es", "pt", "zh", "be", "uk", "ru", "de")
        val currentIndex = codes.indexOf(repository.preferencesManager.settings.value.language).coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle("Выберите язык")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                repository.preferencesManager.updateLanguage(codes[which])
                binding.tvCurrentLanguage.text = languages[which]
                dialog.dismiss()
                Toast.makeText(this, "Язык изменён. Перезагрузите приложение.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPremiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("⭐ GOODOK Premium")
            .setMessage("Автоперевод сообщений\nСмена иконки приложения\nЭксклюзивные функции\n\n299 ₽/месяц")
            .setPositiveButton("Оформить") { _, _ ->
                repository.preferencesManager.updatePremium(true)
                Toast.makeText(this, "Premium активирован! (демо)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showAccountDialog() {
        val user = repository.currentUser
        AlertDialog.Builder(this)
            .setTitle("Учётная запись")
            .setMessage("Email: ${user?.email}\n\nВы можете изменить имя и описание в профиле.")
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
}
