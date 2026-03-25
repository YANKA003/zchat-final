package com.zchat.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.zchat.app.data.model.Call
import com.zchat.app.data.model.CallSignal
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

    // ============ AUTH ============

    suspend fun register(email: String, password: String, username: String, phone: String = ""): Result<User> {
        return try {
            Log.d("FirebaseService", "Registering user: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to get user ID")

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
            setOnlineStatus(uid, true)
            Log.d("FirebaseService", "User logged in successfully: $uid")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Login failed", e)
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            currentUser?.uid?.let { uid ->
                setOnlineStatus(uid, false)
                database.child("users").child(uid).child("fcmToken").removeValue()
            }
            auth.signOut()
            Log.d("FirebaseService", "User logged out")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Logout failed", e)
        }
    }

    private fun setOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            database.child("users").child(uid).child("isOnline").setValue(isOnline)
            if (!isOnline) {
                database.child("users").child(uid).child("lastSeen").setValue(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to set online status", e)
        }
    }

    suspend fun updateFcmToken(token: String) {
        try {
            val uid = currentUser?.uid ?: return
            database.child("users").child(uid).child("fcmToken").setValue(token).await()
            Log.d("FirebaseService", "FCM token updated")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to update FCM token", e)
        }
    }

    // ============ USER PROFILE ============

    suspend fun updateUserProfile(username: String? = null, bio: String? = null, avatarUrl: String? = null): Result<Unit> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("Not logged in")
            val updates = mutableMapOf<String, Any>()

            username?.let {
                updates["username"] = it
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

    fun observeUserStatus(uid: String): Flow<User> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) trySend(user)
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing user status", e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "User status listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        database.child("users").child(uid).addValueEventListener(listener)
        awaitClose { database.child("users").child(uid).removeEventListener(listener) }
    }

    // ============ MESSAGES ============

    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            // Store message under both users' chat
            val chatId = getChatId(message.senderId, message.receiverId)
            database.child("chats").child(chatId).child(message.id).setValue(message).await()

            // Also store in user-specific chat lists for easy retrieval
            database.child("userChats").child(message.senderId).child(message.receiverId).setValue(message.timestamp)
            database.child("userChats").child(message.receiverId).child(message.senderId).setValue(message.timestamp)

            Log.d("FirebaseService", "Message sent: ${message.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to send message", e)
            Result.failure(e)
        }
    }

    fun observeMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = callbackFlow {
        val chatId = getChatId(currentUserId, otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messages = snapshot.children
                        .mapNotNull { it.getValue(Message::class.java) }
                        .filter { !it.isDeleted }
                        .sortedBy { it.timestamp }
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
        database.child("chats").child(chatId).addValueEventListener(listener)
        awaitClose { database.child("chats").child(chatId).removeEventListener(listener) }
    }

    suspend fun editMessage(messageId: String, currentUserId: String, otherUserId: String, newContent: String): Result<Unit> {
        return try {
            val chatId = getChatId(currentUserId, otherUserId)
            val updates = mapOf(
                "content" to newContent,
                "isEdited" to true,
                "editedAt" to System.currentTimeMillis()
            )
            database.child("chats").child(chatId).child(messageId).updateChildren(updates).await()
            Log.d("FirebaseService", "Message edited: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to edit message", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String, currentUserId: String, otherUserId: String): Result<Unit> {
        return try {
            val chatId = getChatId(currentUserId, otherUserId)
            val updates = mapOf(
                "isDeleted" to true,
                "content" to "Сообщение удалено"
            )
            database.child("chats").child(chatId).child(messageId).updateChildren(updates).await()
            Log.d("FirebaseService", "Message deleted: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to delete message", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(currentUserId: String, otherUserId: String): Result<Unit> {
        return try {
            val chatId = getChatId(currentUserId, otherUserId)
            val snapshot = database.child("chats").child(chatId).get().await()
            snapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(Message::class.java)
                if (message != null && message.receiverId == currentUserId && !message.isRead) {
                    messageSnapshot.ref.child("isRead").setValue(true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to mark messages as read", e)
            Result.failure(e)
        }
    }

    private fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    // ============ USERS SEARCH ============

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

    // ============ CALLS / WEBRTC SIGNALING ============

    suspend fun initiateCall(call: Call): Result<Unit> {
        return try {
            database.child("calls").child(call.id).setValue(call).await()
            Log.d("FirebaseService", "Call initiated: ${call.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to initiate call", e)
            Result.failure(e)
        }
    }

    suspend fun sendCallSignal(signal: CallSignal): Result<Unit> {
        return try {
            database.child("callSignals").child(signal.id).setValue(signal).await()
            Log.d("FirebaseService", "Call signal sent: ${signal.type}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to send call signal", e)
            Result.failure(e)
        }
    }

    fun observeCallSignals(callId: String): Flow<CallSignal> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val signal = snapshot.getValue(CallSignal::class.java)
                    if (signal != null && signal.id.startsWith(callId)) {
                        trySend(signal)
                        // Remove signal after processing
                        snapshot.ref.removeValue()
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing call signal", e)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseService", "Call signals listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        database.child("callSignals").addChildEventListener(listener)
        awaitClose { database.child("callSignals").removeEventListener(listener) }
    }

    fun observeIncomingCalls(userId: String): Flow<Call> = callbackFlow {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val call = snapshot.getValue(Call::class.java)
                    if (call != null && call.receiverId == userId && call.status == "RINGING") {
                        trySend(call)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Error parsing incoming call", e)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.child("calls").addChildEventListener(listener)
        awaitClose { database.child("calls").removeEventListener(listener) }
    }

    suspend fun updateCallStatus(callId: String, status: String): Result<Unit> {
        return try {
            database.child("calls").child(callId).child("status").setValue(status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFcmToken(uid: String): String? {
        return try {
            val snapshot = database.child("users").child(uid).child("fcmToken").get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
