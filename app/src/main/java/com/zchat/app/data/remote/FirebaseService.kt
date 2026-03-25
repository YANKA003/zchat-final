package com.zchat.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.zchat.app.data.model.Message
import com.zchat.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    val currentUser: FirebaseUser?
        get() = try { auth.currentUser } catch (e: Exception) { null }

    suspend fun register(email: String, password: String, username: String, phone: String = ""): Result<User> {
        return try {
            Log.d("FirebaseService", "Registering user: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to get user ID")

            // Update display name in Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            val user = User(
                uid = uid,
                email = email,
                username = username,
                phoneNumber = phone
            )
            database.child("users").child(user.uid).setValue(user).await()
            Log.d("FirebaseService", "User registered successfully: $uid")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Registration failed", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            Log.d("FirebaseService", "Logging in user: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to get user ID")
            val snapshot = database.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: User(uid, email)
            try {
                database.child("users").child(uid).child("isOnline").setValue(true).await()
            } catch (e: Exception) {
                Log.w("FirebaseService", "Failed to set online status", e)
            }
            Log.d("FirebaseService", "User logged in successfully: $uid")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Login failed", e)
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            currentUser?.uid?.let {
                database.child("users").child(it).child("isOnline").setValue(false)
            }
            auth.signOut()
            Log.d("FirebaseService", "User logged out")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Logout failed", e)
        }
    }

    suspend fun updateUserProfile(
        username: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("Not logged in")
            val updates = mutableMapOf<String, Any>()

            username?.let {
                updates["username"] = it
                // Also update Firebase Auth display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(it)
                    .build()
                currentUser?.updateProfile(profileUpdates)?.await()
            }
            bio?.let { updates["bio"] = it }
            avatarUrl?.let { updates["avatarUrl"] = it }

            if (updates.isNotEmpty()) {
                database.child("users").child(uid).updateChildren(updates).await()
            }

            Log.d("FirebaseService", "Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to update profile", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val snapshot = database.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to get user profile", e)
            Result.failure(e)
        }
    }

    fun observeUsers(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                    trySend(users)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing users", e)
                    trySend(emptyList())
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Users listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        database.child("users").addValueEventListener(listener)
        awaitClose { database.child("users").removeEventListener(listener) }
    }

    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            database.child("messages").child(message.id).setValue(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to send message", e)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messages = snapshot.children
                        .mapNotNull { it.getValue(Message::class.java) }
                        .filter { it.senderId == userId || it.receiverId == userId }
                    trySend(messages)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing messages", e)
                    trySend(emptyList())
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Messages listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        database.child("messages").addValueEventListener(listener)
        awaitClose { database.child("messages").removeEventListener(listener) }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val users = snapshot.children
                .mapNotNull { it.getValue(User::class.java) }
                .filter { it.username.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to search users", e)
            Result.failure(e)
        }
    }
}
