package com.zchat.app.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zchat_prefs", Context.MODE_PRIVATE)

    var theme: Int
        get() = prefs.getInt("theme", 0)
        set(value) = prefs.edit().putInt("theme", value).apply()

    var chatBackground: String
        get() = prefs.getString("chat_background", "default") ?: "default"
        set(value) = prefs.edit().putString("chat_background", value).apply()

    var enableAnimations: Boolean
        get() = prefs.getBoolean("enable_animations", true)
        set(value) = prefs.edit().putBoolean("enable_animations", value).apply()

    var showOnlineStatus: Boolean
        get() = prefs.getBoolean("show_online_status", true)
        set(value) = prefs.edit().putBoolean("show_online_status", value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("app_lock_enabled", value).apply()

    fun setFingerprintEnabled(enabled: Boolean) {
        appLockEnabled = enabled
    }

    fun isFingerprintEnabled(): Boolean = appLockEnabled

    var notificationSound: String
        get() = prefs.getString("notification_sound", "default") ?: "default"
        set(value) = prefs.edit().putString("notification_sound", value).apply()

    var announceCallerName: Boolean
        get() = prefs.getBoolean("announce_caller_name", false)
        set(value) = prefs.edit().putBoolean("announce_caller_name", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var batterySaverMode: Int
        get() = prefs.getInt("battery_saver_mode", 0)
        set(value) = prefs.edit().putInt("battery_saver_mode", value).apply()

    var batterySaverThreshold: Int
        get() = prefs.getInt("battery_saver_threshold", 30)
        set(value) = prefs.edit().putInt("battery_saver_threshold", value).apply()

    var isPremiumEnabled: Boolean
        get() = prefs.getBoolean("premium_enabled", false)
        set(value) = prefs.edit().putBoolean("premium_enabled", value).apply()

    var autoTranslate: Boolean
        get() = prefs.getBoolean("auto_translate", false)
        set(value) = prefs.edit().putBoolean("auto_translate", value).apply()

    var targetLanguage: String
        get() = prefs.getString("target_language", "ru") ?: "ru"
        set(value) = prefs.edit().putString("target_language", value).apply()
}
