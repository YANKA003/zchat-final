package com.zchat.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ZChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("ZChat", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("ZChat", "Firebase initialization failed", e)
        }
    }
}
