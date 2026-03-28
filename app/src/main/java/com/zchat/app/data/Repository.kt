package com.zchat.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.zchat.app.data.local.*
import com.zchat.app.data.model.*
import com.zchat.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val prefsManager = PreferencesManager(context)

    private val userDao = database.userDao()
    private val messageDao = database.messageDao()
    private val callDao = database.callDao()
    private val channelDao = database.channelDao()
    private val contactDao = database.contactDao()

    // Firebase service
    private var firebaseService: FirebaseService? = null

    init {
        try {
            firebaseService = FirebaseService()
        } catch (e: Exception) {
            Log.e("Repository", "Firebase not initialized", e)
        }
    }

    // Authentication
    val currentUser: FirebaseUser?
        get() = try { firebaseService?.currentUser } catch (e: Exception) { null }

    val currentUserId: String?
        get() = try { firebaseService?.currentUserId } catch (e: Exception) { null }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return firebaseService?.login(email, password)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    suspend fun register(email: String, password: String, username: String): Result<FirebaseUser> {
        return registerWithPhone(email, password, username, "")
    }

    suspend fun registerWithPhone(email: String, password: String, username: String, phone: String): Result<FirebaseUser> {
        return firebaseService?.registerWithPhone(email, password, username, phone)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    fun logout() {
        try {
            firebaseService?.logout()
        } catch (e: Exception) {
            Log.e("Repository", "Error during logout", e)
        }
        prefsManager.clearSession()
    }

    // Users
    suspend fun getUser(uid: String): User? {
        return try { firebaseService?.getUser(uid) } catch (e: Exception) { null }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return firebaseService?.updateUser(user)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return firebaseService?.searchUsers(query)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    // Find users by phone numbers (for contact matching)
    suspend fun findUsersByPhones(phones: List<String>): Result<List<User>> {
        return firebaseService?.findUsersByPhones(phones)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    fun setOnlineStatus(isOnline: Boolean) {
        try {
            currentUserId?.let { firebaseService?.setOnlineStatus(it, isOnline) }
        } catch (e: Exception) {
            Log.e("Repository", "Error setting online status", e)
        }
    }

    fun observeUserStatus(uid: String): Flow<User>? {
        return try { firebaseService?.observeUserStatus(uid) } catch (e: Exception) { null }
    }

    // Save user locally for caching
    fun saveUserLocally(user: User) {
        prefsManager.savedUsername = user.username
        prefsManager.savedPhone = user.phoneNumber
        prefsManager.savedAvatarUrl = user.avatarUrl
    }

    // Messages
    suspend fun sendMessage(message: Message): Result<Unit> {
        return firebaseService?.sendMessage(message)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    suspend fun editMessage(messageId: String, receiverId: String, newContent: String): Result<Unit> {
        return currentUserId?.let {
            firebaseService?.editMessage(messageId, it, receiverId, newContent)
        } ?: Result.failure(Exception("Not logged in"))
    }

    suspend fun deleteMessage(messageId: String, receiverId: String): Result<Unit> {
        return currentUserId?.let {
            firebaseService?.deleteMessage(messageId, it, receiverId)
        } ?: Result.failure(Exception("Not logged in"))
    }

    fun observeMessages(userId: String): Flow<List<Message>>? {
        return currentUserId?.let { firebaseService?.observeMessages(it, userId) }
    }

    // Calls
    suspend fun saveCall(call: Call): Result<Unit> {
        return firebaseService?.saveCall(call)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    fun observeCalls(): Flow<List<Call>>? {
        return currentUserId?.let { firebaseService?.observeCalls(it) }
    }

    suspend fun getLocalCalls(): List<Call> {
        return callDao.getAllCalls()
    }

    suspend fun saveCallLocal(call: Call) {
        callDao.insertCall(call)
    }

    // Channels
    suspend fun createChannel(channel: Channel): Result<Unit> {
        return firebaseService?.createChannel(channel)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    suspend fun searchChannels(query: String): Result<List<Channel>> {
        return firebaseService?.searchChannels(query)
            ?: Result.failure(Exception("Firebase not configured"))
    }

    suspend fun subscribeToChannel(channelId: String): Result<Unit> {
        return currentUserId?.let { firebaseService?.subscribeToChannel(channelId, it) }
            ?: Result.failure(Exception("Not logged in"))
    }

    suspend fun getLocalChannels(): List<Channel> {
        return channelDao.getAllChannels()
    }

    suspend fun saveChannelLocal(channel: Channel) {
        channelDao.insertChannel(channel)
    }

    // Contacts
    suspend fun getLocalContacts(): List<Contact> {
        return contactDao.getAllContacts()
    }

    suspend fun searchContactsLocal(query: String): List<Contact> {
        return contactDao.searchContacts(query)
    }

    suspend fun saveContact(contact: Contact) {
        contactDao.insertContact(contact)
    }

    suspend fun saveContacts(contacts: List<Contact>) {
        contactDao.insertContacts(contacts)
    }

    // Settings
    var theme: Int
        get() = prefsManager.theme
        set(value) { prefsManager.theme = value }

    var language: String
        get() = prefsManager.language
        set(value) { prefsManager.language = value }

    var isPremium: Boolean
        get() = prefsManager.isPremium
        set(value) { prefsManager.isPremium = value }

    var premiumType: String
        get() = prefsManager.premiumType
        set(value) { prefsManager.premiumType = value }

    var premiumExpiry: Long
        get() = prefsManager.premiumExpiry
        set(value) { prefsManager.premiumExpiry = value }
}
