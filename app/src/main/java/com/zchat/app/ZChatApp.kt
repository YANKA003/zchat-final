package com.zchat.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ZChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase FIRST
        try {
            FirebaseApp.initializeApp(this)
            Log.d("ZChatApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("ZChatApp", "Firebase initialization failed", e)
        }

        // Global exception handler to prevent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ZChatApp", "Uncaught exception in ${thread.name}", throwable)
        }
    }
}
