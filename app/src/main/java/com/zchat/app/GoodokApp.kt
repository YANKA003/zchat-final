package com.zchat.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class GoodokApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("GOODOK", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("GOODOK", "Firebase initialization failed", e)
        }
    }
}
