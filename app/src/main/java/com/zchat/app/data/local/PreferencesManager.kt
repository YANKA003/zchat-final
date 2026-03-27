package com.zchat.app.data.local

import android.content.Context
import com.zchat.app.data.model.AppSettings

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("goodok_prefs", Context.MODE_PRIVATE)

    var currentUserId: String?
        get() = prefs.getString("current_user_id", null)
        set(value) = prefs.edit().putString("current_user_id", value).apply()

    var currentUserEmail: String?
        get() = prefs.getString("current_user_email", null)
        set(value) = prefs.edit().putString("current_user_email", value).apply()

    var currentUsername: String?
        get() = prefs.getString("current_username", null)
        set(value) = prefs.edit().putString("current_username", value).apply()

    var theme: Int
        get() = prefs.getInt("theme", 0)
        set(value) = prefs.edit().putInt("theme", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var showOnlineStatus: Boolean
        get() = prefs.getBoolean("show_online_status", true)
        set(value) = prefs.edit().putBoolean("show_online_status", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var autoTranslate: Boolean
        get() = prefs.getBoolean("auto_translate", false)
        set(value) = prefs.edit().putBoolean("auto_translate", value).apply()

    var targetLanguage: String
        get() = prefs.getString("target_language", "en") ?: "en"
        set(value) = prefs.edit().putString("target_language", value).apply()

    var isPremium: Boolean
        get() = prefs.getBoolean("is_premium", false)
        set(value) = prefs.edit().putBoolean("is_premium", value).apply()

    var premiumType: String
        get() = prefs.getString("premium_type", "") ?: ""
        set(value) = prefs.edit().putString("premium_type", value).apply()

    var premiumExpiry: Long
        get() = prefs.getLong("premium_expiry", 0)
        set(value) = prefs.edit().putLong("premium_expiry", value).apply()

    fun getSettings(): AppSettings {
        return AppSettings(
            theme = theme,
            language = language,
            showOnlineStatus = showOnlineStatus,
            notificationsEnabled = notificationsEnabled,
            autoTranslate = autoTranslate,
            targetLanguage = targetLanguage
        )
    }

    fun saveSettings(settings: AppSettings) {
        theme = settings.theme
        language = settings.language
        showOnlineStatus = settings.showOnlineStatus
        notificationsEnabled = settings.notificationsEnabled
        autoTranslate = settings.autoTranslate
        targetLanguage = settings.targetLanguage
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
