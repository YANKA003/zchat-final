package com.zchat.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.zchat.app.data.local.*
import com.zchat.app.data.model.*
import com.zchat.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow

class Repository(context: Context) {
    private val firebaseService = FirebaseService()
    private val database = AppDatabase.getDatabase(context)
    private val prefsManager = PreferencesManager(context)

    private val userDao = database.userDao()
    private val messageDao = database.messageDao()
    private val callDao = database.callDao()
    private val channelDao = database.channelDao()
    private val contactDao = database.contactDao()

    val currentUser: FirebaseUser?
        get() = firebaseService.currentUser

    val currentUserId: String?
        get() = firebaseService.currentUserId

    // Authentication
    suspend fun login(email: String, password: String) = firebaseService.login(email, password)

    suspend fun register(email: String, password: String, username: String) =
        firebaseService.register(email, password, username)

    fun logout() {
        firebaseService.logout()
        prefsManager.clear()
    }

    // Users
    suspend fun getUser(uid: String) = firebaseService.getUser(uid)

    suspend fun updateUser(user: User) = firebaseService.updateUser(user)

    suspend fun searchUsers(query: String): Result<List<User>> {
        return firebaseService.searchUsers(query)
    }

    fun setOnlineStatus(isOnline: Boolean) {
        currentUserId?.let { firebaseService.setOnlineStatus(it, isOnline) }
    }

    fun observeUserStatus(uid: String): Flow<User> = firebaseService.observeUserStatus(uid)

    // Messages
    suspend fun sendMessage(message: Message) = firebaseService.sendMessage(message)

    suspend fun editMessage(messageId: String, receiverId: String, newContent: String): Result<Unit> {
        return currentUserId?.let {
            firebaseService.editMessage(messageId, it, receiverId, newContent)
        } ?: Result.failure(Exception("Not logged in"))
    }

    suspend fun deleteMessage(messageId: String, receiverId: String): Result<Unit> {
        return currentUserId?.let {
            firebaseService.deleteMessage(messageId, it, receiverId)
        } ?: Result.failure(Exception("Not logged in"))
    }

    fun observeMessages(userId: String): Flow<List<Message>> {
        return currentUserId?.let { firebaseService.observeMessages(it, userId) }
            ?: throw IllegalStateException("Not logged in")
    }

    // Calls
    suspend fun saveCall(call: Call) = firebaseService.saveCall(call)

    fun observeCalls(): Flow<List<Call>> {
        return currentUserId?.let { firebaseService.observeCalls(it) }
            ?: throw IllegalStateException("Not logged in")
    }

    suspend fun getLocalCalls() = callDao.getAllCalls()

    suspend fun saveCallLocal(call: Call) = callDao.insertCall(call)

    // Channels
    suspend fun createChannel(channel: Channel) = firebaseService.createChannel(channel)

    suspend fun searchChannels(query: String) = firebaseService.searchChannels(query)

    suspend fun subscribeToChannel(channelId: String): Result<Unit> {
        return currentUserId?.let { firebaseService.subscribeToChannel(channelId, it) }
            ?: Result.failure(Exception("Not logged in"))
    }

    suspend fun getLocalChannels() = channelDao.getAllChannels()

    suspend fun saveChannelLocal(channel: Channel) = channelDao.insertChannel(channel)

    suspend fun searchChannelsLocal(query: String) = channelDao.searchChannels(query)

    // Contacts
    suspend fun getLocalContacts() = contactDao.getAllContacts()

    suspend fun searchContactsLocal(query: String) = contactDao.searchContacts(query)

    suspend fun saveContact(contact: Contact) = contactDao.insertContact(contact)

    suspend fun saveContacts(contacts: List<Contact>) = contactDao.insertContacts(contacts)

    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)

    // Settings
    fun getSettings() = prefsManager.getSettings()

    fun saveSettings(settings: AppSettings) = prefsManager.saveSettings(settings)

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
}
