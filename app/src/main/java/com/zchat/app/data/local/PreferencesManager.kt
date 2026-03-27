package com.zchat.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.zchat.app.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("goodok_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings
    
    private fun loadSettings(): AppSettings {
        val json = prefs.getString("app_settings", null) ?: return AppSettings()
        return try { gson.fromJson(json, AppSettings::class.java) } catch (e: Exception) { AppSettings() }
    }
    
    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().putString("app_settings", gson.toJson(newSettings)).apply()
        _settings.value = newSettings
    }
    
    fun updateTheme(theme: Int) { updateSettings(_settings.value.copy(theme = theme)) }
    fun updateDesignStyle(style: Int) { updateSettings(_settings.value.copy(designStyle = style)) }
    fun updateAnimations(enabled: Boolean) { updateSettings(_settings.value.copy(enableAnimations = enabled)) }
    fun updateOnlineStatus(show: Boolean) { updateSettings(_settings.value.copy(showOnlineStatus = show)) }
    fun updateAppLock(enabled: Boolean) { updateSettings(_settings.value.copy(appLockEnabled = enabled)) }
    fun updateAnnounceCaller(enabled: Boolean) { updateSettings(_settings.value.copy(announceCallerName = enabled)) }
    fun updateNotifications(enabled: Boolean) { updateSettings(_settings.value.copy(notificationsEnabled = enabled)) }
    fun updateBatterySaverMode(mode: Int) { updateSettings(_settings.value.copy(batterySaverMode = mode)) }
    fun updatePremium(enabled: Boolean) { updateSettings(_settings.value.copy(premiumEnabled = enabled)) }
    fun updateAutoTranslate(enabled: Boolean) { updateSettings(_settings.value.copy(autoTranslate = enabled)) }
    fun updateLanguage(lang: String) { updateSettings(_settings.value.copy(language = lang)) }
    
    // Draft system for messages
    fun saveDraft(chatId: String, draft: String) { prefs.edit().putString("draft_$chatId", draft).apply() }
    fun getDraft(chatId: String): String = prefs.getString("draft_$chatId", "") ?: ""
    fun clearDraft(chatId: String) { prefs.edit().remove("draft_$chatId").apply() }
    
    fun isCallRecordingEnabled(): Boolean = prefs.getBoolean("call_recording_enabled", true)
    fun setCallRecordingEnabled(enabled: Boolean) { prefs.edit().putBoolean("call_recording_enabled", enabled).apply() }
}
