package com.zchat.app.utils

import android.content.Context
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
    
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    private fun getLocale(code: String): Locale {
        return when (code) {
            "en-gb" -> Locale("en", "GB")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "be" -> Locale("be")
            "uk" -> Locale("uk")
            else -> Locale(code)
        }
    }
    
    fun getLanguageName(code: String): String {
        return SUPPORTED_LANGUAGES[code] ?: "Русский"
    }
    
    fun getCurrentLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
        
        val code = locale.language
        val country = locale.country
        
        return if (code == "en" && country == "GB") {
            "en-gb"
        } else {
            SUPPORTED_LANGUAGES.keys.find { it == code } ?: "ru"
        }
    }
}
