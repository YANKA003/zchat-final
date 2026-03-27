package com.zchat.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.zchat.app.data.local.PreferencesManager
import com.zchat.app.utils.LanguageHelper

class GoodokApp : Application() {
    
    companion object {
        lateinit var instance: GoodokApp
            private set
    }
    
    lateinit var preferencesManager: PreferencesManager
        private set
    
    override fun onCreate() {
        instance = this
        super.onCreate()
        
        preferencesManager = PreferencesManager(this)
        
        try {
            FirebaseApp.initializeApp(this)
            Log.d("GOODOK", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("GOODOK", "Firebase initialization failed", e)
        }
    }
    
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("goodok_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "ru") ?: "ru"
        
        val context = LanguageHelper.setLocale(base, language)
        super.attachBaseContext(context)
    }
}
