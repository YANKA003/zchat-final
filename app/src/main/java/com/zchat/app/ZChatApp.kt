package com.zchat.app

import android.app.Application
import android.util.Log
import com.zchat.app.util.LanguageHelper

class ZChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply saved language
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val language = prefs.getString("language", "en") ?: "en"
            LanguageHelper.setLanguage(this, language)
            Log.d("ZChatApp", "Language applied: $language")
        } catch (e: Exception) {
            Log.e("ZChatApp", "Error applying language", e)
        }
    }

    override fun attachBaseContext(base: android.content.Context) {
        try {
            val prefs = base.getSharedPreferences("goodok_prefs", android.content.Context.MODE_PRIVATE)
            val language = prefs.getString("language", "en") ?: "en"
            val context = LanguageHelper.setLocale(base, language)
            super.attachBaseContext(context)
        } catch (e: Exception) {
            Log.e("ZChatApp", "Error in attachBaseContext", e)
            super.attachBaseContext(base)
        }
    }
}
