package com.zchat.app.data

import android.content.Context
import androidx.room.Room
import com.zchat.app.data.local.AppDatabase
import com.zchat.app.data.local.PreferencesManager
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import com.zchat.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class Repository(context: Context) {
    private val firebaseService = FirebaseService()
    private val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zchat_db")
        .fallbackToDestructiveMigration().build()
    private val dao = database.chatDao()
    val preferencesManager = PreferencesManager(context)
    
    val currentUser get() = firebaseService.currentUser
    
    suspend fun register(email: String, password: String, username: String, phone: String = "") = 
        firebaseService.register(email, password, username, phone)
    
    suspend fun login(email: String, password: String) = firebaseService.login(email, password)
    
    fun logout() = firebaseService.logout()
    
    fun getUsers(currentUserId: String): Flow<List<User>> = dao.getAllUsers(currentUserId)
    
    suspend fun searchUsers(query: String) = firebaseService.searchUsers(query)
    
    fun getMessages(userId: String, currentUserId: String): Flow<List<Message>> = 
        dao.getMessagesWithUser(userId, currentUserId)
    
    suspend fun sendMessage(content: String, receiverId: String) {
        val senderId = currentUser?.uid ?: return
        val message = Message(UUID.randomUUID().toString(), senderId, receiverId, content, System.currentTimeMillis())
        dao.insertMessage(message)
        firebaseService.sendMessage(message).onSuccess { dao.markAsSynced(message.id) }
    }
    
    suspend fun initiateCall(receiverId: String, type: String) = firebaseService.initiateCall(receiverId, type)
    suspend fun endCall(callId: String, duration: Long) = firebaseService.endCall(callId, duration)
}
