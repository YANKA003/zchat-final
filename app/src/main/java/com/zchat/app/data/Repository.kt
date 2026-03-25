package com.zchat.app.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.zchat.app.data.local.AppDatabase
import com.zchat.app.data.local.ChatDao
import com.zchat.app.data.local.PreferencesManager
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import com.zchat.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class Repository(context: Context) {
    private val firebaseService = FirebaseService()
    private var database: AppDatabase? = null
    private var dao: ChatDao? = null
    val preferencesManager: PreferencesManager

    val currentUser get() = firebaseService.currentUser

    init {
        try {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "zchat_db"
            ).fallbackToDestructiveMigration().build()
            dao = database?.chatDao()
            Log.d("Repository", "Database initialized successfully")
        } catch (e: Exception) {
            Log.e("Repository", "Failed to initialize database", e)
        }
        preferencesManager = PreferencesManager(context)
    }

    suspend fun register(email: String, password: String, username: String, phone: String = "") =
        firebaseService.register(email, password, username, phone)

    suspend fun login(email: String, password: String) =
        firebaseService.login(email, password)

    fun logout() = firebaseService.logout()

    suspend fun updateUserProfile(username: String? = null, bio: String? = null, avatarUrl: String? = null) =
        firebaseService.updateUserProfile(username, bio, avatarUrl)

    suspend fun getUserProfile(uid: String) = firebaseService.getUserProfile(uid)

    fun getUsers(currentUserId: String): Flow<List<User>>? = try {
        dao?.getAllUsers(currentUserId)
    } catch (e: Exception) {
        Log.e("Repository", "Failed to get users", e)
        null
    }

    suspend fun searchUsers(query: String) = firebaseService.searchUsers(query)

    fun getMessages(userId: String, currentUserId: String): Flow<List<Message>>? = try {
        dao?.getMessagesWithUser(userId, currentUserId)
    } catch (e: Exception) {
        Log.e("Repository", "Failed to get messages", e)
        null
    }

    suspend fun sendMessage(content: String, receiverId: String) {
        val senderId = currentUser?.uid ?: return
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        try {
            dao?.insertMessage(message)
            firebaseService.sendMessage(message)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to send message", e)
        }
    }
}
