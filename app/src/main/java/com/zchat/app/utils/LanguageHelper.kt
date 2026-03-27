package com.zchat.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageHelper {
    
    val SUPPORTED_LANGUAGES = mapOf(
        "en" to "English (US)",
        "en-gb" to "English (UK)",
        "fr" to "Français",
        "es" to "Español",
        "pt" to "Português",
        "zh" to "中文",
        "be" to "Беларуская",
        "uk" to "Українська",
        "ru" to "Русский",
        "de" to "Deutsch"
    )
    
    val LANGUAGE_CODES = arrayOf(
        "en", "en-gb", "fr", "es", "pt", "zh", "be", "uk", "ru", "de"
    )
    
    private const val PREFS_NAME = "goodok_prefs"
    private const val KEY_LANGUAGE = "language"
    
    fun setLocale(context: Context, languageCode: String): Context {
        // Сохраняем выбор языка
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun applyLanguage(context: Context): Context {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
        return setLocale(context, savedLanguage)
    }
    
    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
    }
    
    private fun getLocale(code: String): Locale {
        return when (code) {
            "en-gb" -> Locale("en", "GB")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "be" -> Locale("be", "BY")
            "uk" -> Locale("uk", "UA")
            "ru" -> Locale("ru", "RU")
            "de" -> Locale("de", "DE")
            "fr" -> Locale("fr", "FR")
            "es" -> Locale("es", "ES")
            "pt" -> Locale("pt", "PT")
            else -> Locale(code)
        }
    }
    
    fun getLanguageName(code: String): String {
        return SUPPORTED_LANGUAGES[code] ?: "Русский"
    }
    
    fun getCurrentLanguageCode(context: Context): String {
        return getSavedLanguage(context)
    }
}
