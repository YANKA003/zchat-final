package com.zchat.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "en-rGB" -> Locale("en", "GB")
            "fr" -> Locale("fr")
            "es" -> Locale("es")
            "pt" -> Locale("pt")
            "zh" -> Locale("zh")
            "be" -> Locale("be")
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "de" -> Locale("de")
            else -> Locale("en")
        }

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
