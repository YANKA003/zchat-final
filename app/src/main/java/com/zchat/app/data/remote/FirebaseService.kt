package com.zchat.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.zchat.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Authentication
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            // Create user profile in database
            val userProfile = User(
                uid = user.uid,
                email = email,
                username = username,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            database.child("users").child(user.uid).setValue(userProfile).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.currentUser?.uid?.let { uid ->
            database.child("users").child(uid).child("isOnline").setValue(false)
            database.child("users").child(uid).child("lastSeen").setValue(System.currentTimeMillis())
        }
        auth.signOut()
    }

    // Users
    suspend fun getUser(uid: String): User? {
        val snapshot = database.child("users").child(uid).get().await()
        return snapshot.getValue(User::class.java)
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            database.child("users").child(user.uid).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
            val filtered = if (query.isEmpty()) users else users.filter {
                it.username.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.phoneNumber.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Online status
    fun setOnlineStatus(uid: String, isOnline: Boolean) {
        database.child("users").child(uid).child("isOnline").setValue(isOnline)
        database.child("users").child(uid).child("lastSeen").setValue(System.currentTimeMillis())
    }

    fun observeUserStatus(uid: String): Flow<User> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    trySend(user)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("users").child(uid).addValueEventListener(listener)
        awaitClose { database.child("users").child(uid).removeEventListener(listener) }
    }

    // Messages
    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val chatId = getChatId(message.senderId, message.receiverId)
            database.child("chats").child(chatId).child(message.id).setValue(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, senderId: String, receiverId: String, newContent: String): Result<Unit> {
        return try {
            val chatId = getChatId(senderId, receiverId)
            database.child("chats").child(chatId).child(messageId).child("content").setValue(newContent).await()
            database.child("chats").child(chatId).child(messageId).child("isEdited").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String, senderId: String, receiverId: String): Result<Unit> {
        return try {
            val chatId = getChatId(senderId, receiverId)
            database.child("chats").child(chatId).child(messageId).child("isDeleted").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(userId1: String, userId2: String): Flow<List<Message>> = callbackFlow {
        val chatId = getChatId(userId1, userId2)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("chats").child(chatId).addValueEventListener(listener)
        awaitClose { database.child("chats").child(chatId).removeEventListener(listener) }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    // Calls
    suspend fun saveCall(call: Call): Result<Unit> {
        return try {
            database.child("calls").child(currentUserId!!).child(call.id).setValue(call).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCalls(uid: String): Flow<List<Call>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = snapshot.children.mapNotNull { it.getValue(Call::class.java) }
                    .sortedByDescending { it.timestamp }
                trySend(calls)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("calls").child(uid).addValueEventListener(listener)
        awaitClose { database.child("calls").child(uid).removeEventListener(listener) }
    }

    // Channels
    suspend fun createChannel(channel: Channel): Result<Unit> {
        return try {
            database.child("channels").child(channel.id).setValue(channel).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchChannels(query: String): Result<List<Channel>> {
        return try {
            val snapshot = database.child("channels").get().await()
            val channels = snapshot.children.mapNotNull { it.getValue(Channel::class.java) }
            val filtered = if (query.isEmpty()) channels else channels.filter {
                it.name.contains(query, ignoreCase = true)
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subscribeToChannel(channelId: String, userId: String): Result<Unit> {
        return try {
            database.child("channel_subscribers").child(channelId).child(userId).setValue(true).await()
            database.child("user_channels").child(userId).child(channelId).setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
