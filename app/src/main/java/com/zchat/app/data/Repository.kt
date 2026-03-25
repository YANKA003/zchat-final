package com.zchat.app.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.zchat.app.data.local.AppDatabase
import com.zchat.app.data.local.ChatDao
import com.zchat.app.data.local.PreferencesManager
import com.zchat.app.data.model.Call
import com.zchat.app.data.model.CallSignal
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import com.zchat.app.data.remote.FirebaseService
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class Repository(context: Context) {
    private val firebaseService = FirebaseService()
    private var database: AppDatabase? = null
    private var dao: ChatDao? = null
    val preferencesManager: PreferencesManager
    private val scope = CoroutineScope(Dispatchers.IO)

    val currentUser: FirebaseUser?
        get() = firebaseService.currentUser

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

        // Get FCM token asynchronously
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                firebaseService.updateFcmToken(token)
                Log.d("Repository", "FCM token saved")
            } catch (e: Exception) {
                Log.e("Repository", "Failed to get FCM token", e)
            }
        }
    }

    // ============ AUTH ============

    suspend fun register(email: String, password: String, username: String, phone: String = "") =
        firebaseService.register(email, password, username, phone)

    suspend fun login(email: String, password: String): Result<User> {
        val result = firebaseService.login(email, password)
        if (result.isSuccess) {
            scope.launch {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    firebaseService.updateFcmToken(token)
                } catch (e: Exception) {
                    Log.e("Repository", "Failed to update FCM token on login", e)
                }
            }
        }
        return result
    }

    fun logout() = firebaseService.logout()

    // ============ USER PROFILE ============

    suspend fun updateUserProfile(username: String? = null, bio: String? = null, avatarUrl: String? = null) =
        firebaseService.updateUserProfile(username, bio, avatarUrl)

    suspend fun getUserProfile(uid: String) = firebaseService.getUserProfile(uid)

    fun observeUserStatus(uid: String): Flow<User> = firebaseService.observeUserStatus(uid)

    // ============ MESSAGES ============

    suspend fun sendMessage(content: String, receiverId: String): Result<Message> {
        val senderId = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        val result = firebaseService.sendMessage(message)
        return if (result.isSuccess) {
            Result.success(message)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to send message"))
        }
    }

    fun observeMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> =
        firebaseService.observeMessages(currentUserId, otherUserId)

    suspend fun editMessage(messageId: String, currentUserId: String, otherUserId: String, newContent: String) =
        firebaseService.editMessage(messageId, currentUserId, otherUserId, newContent)

    suspend fun deleteMessage(messageId: String, currentUserId: String, otherUserId: String) =
        firebaseService.deleteMessage(messageId, currentUserId, otherUserId)

    suspend fun markMessagesAsRead(currentUserId: String, otherUserId: String) =
        firebaseService.markMessagesAsRead(currentUserId, otherUserId)

    // ============ USERS ============

    suspend fun searchUsers(query: String) = firebaseService.searchUsers(query)

    // ============ CALLS ============

    suspend fun initiateCall(call: Call) = firebaseService.initiateCall(call)

    suspend fun sendCallSignal(signal: CallSignal) = firebaseService.sendCallSignal(signal)

    fun observeCallSignals(callId: String): Flow<CallSignal> = firebaseService.observeCallSignals(callId)

    fun observeIncomingCalls(userId: String): Flow<Call> = firebaseService.observeIncomingCalls(userId)

    suspend fun updateCallStatus(callId: String, status: String) = firebaseService.updateCallStatus(callId, status)

    // ============ LOCAL DATABASE (for offline) ============

    fun getMessages(userId: String, currentUserId: String): Flow<List<Message>>? = try {
        dao?.getMessagesWithUser(userId, currentUserId)
    } catch (e: Exception) {
        Log.e("Repository", "Failed to get messages", e)
        null
    }

    fun getUsers(currentUserId: String): Flow<List<User>>? = try {
        dao?.getAllUsers(currentUserId)
    } catch (e: Exception) {
        Log.e("Repository", "Failed to get users", e)
        null
    }
}
