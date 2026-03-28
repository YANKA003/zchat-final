package com.zchat.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    
    val currentUser: FirebaseUser? get() = auth.currentUser
    
    suspend fun register(email: String, password: String, username: String, phone: String = ""): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = User(uid = result.user?.uid ?: "", email = email, username = username, phoneNumber = phone)
            database.child("users").child(user.uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: ""
            val snapshot = database.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: User(uid, email)
            database.child("users").child(uid).child("isOnline").setValue(true).await()
            Result.success(user)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    fun logout() {
        currentUser?.uid?.let { database.child("users").child(it).child("isOnline").setValue(false) }
        auth.signOut()
    }
    
    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                trySend(users)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("users").addValueEventListener(listener)
        awaitClose { database.child("users").removeEventListener(listener) }
    }
    
    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            database.child("messages").child(message.id).setValue(message).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                .filter { it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
            Result.success(users)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    suspend fun initiateCall(receiverId: String, type: String): Result<String> {
        return try {
            val callId = java.util.UUID.randomUUID().toString()
            val call = mapOf(
                "id" to callId, "callerId" to currentUser?.uid, "receiverId" to receiverId,
                "timestamp" to System.currentTimeMillis(), "type" to type
            )
            database.child("calls").child(callId).setValue(call).await()
            Result.success(callId)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    suspend fun endCall(callId: String, duration: Long): Result<Unit> {
        return try {
            database.child("calls").child(callId).child("duration").setValue(duration).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
