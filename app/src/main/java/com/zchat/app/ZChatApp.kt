package com.zchat.app

import android.app.Application
import com.zchat.app.util.LanguageHelper

class ZChatApp : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        val prefs = base.getSharedPreferences("goodok_prefs", android.content.Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        val context = LanguageHelper.setLocale(base, language)
        super.attachBaseContext(context)
    }
}
