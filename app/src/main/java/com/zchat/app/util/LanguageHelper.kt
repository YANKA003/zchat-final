package com.zchat.app.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LanguageHelper {

    /**
     * Apply locale to context - returns new context with applied locale
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            config.locale = locale
        }

        return context.createConfigurationContext(config)
    }

    /**
     * Apply language to context - updates resources directly
     */
    fun setLanguage(context: Context, languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            config.locale = locale
        }

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Get Locale from language code
     */
    private fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            "en-rGB", "en_gb" -> Locale("en", "GB")
            "fr" -> Locale("fr")
            "es" -> Locale("es")
            "pt" -> Locale("pt")
            "zh" -> Locale("zh")
            "be" -> Locale("be", "BY")  // Belarusian
            "uk" -> Locale("uk", "UA")  // Ukrainian
            "ru" -> Locale("ru", "RU")  // Russian
            "de" -> Locale("de", "DE")  // German
            else -> Locale("en", "US")  // Default English US
        }
    }

    /**
     * Get language name in its own language
     */
    fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "English (US)"
            "en-rGB" -> "English (UK)"
            "fr" -> "Français"
            "es" -> "Español"
            "pt" -> "Português"
            "zh" -> "中文"
            "be" -> "Беларуская"
            "uk" -> "Українська"
            "ru" -> "Русский"
            "de" -> "Deutsch"
            else -> "English (US)"
        }
    }
}
