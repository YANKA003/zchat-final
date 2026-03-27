package com.zchat.app.data.local

import androidx.room.*
import com.zchat.app.data.model.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) ORDER BY timestamp ASC")
    suspend fun getMessagesBetween(userId1: String, userId2: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Update
    suspend fun updateMessage(message: Message)

    @Query("UPDATE messages SET content = :content, isEdited = 1 WHERE id = :messageId")
    suspend fun editMessage(messageId: String, content: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    suspend fun getAllCalls(): List<Call>

    @Query("SELECT * FROM calls WHERE callerId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    suspend fun getCallsForUser(userId: String): List<Call>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call)

    @Delete
    suspend fun deleteCall(call: Call)

    @Query("DELETE FROM calls")
    suspend fun deleteAllCalls()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY subscribersCount DESC")
    suspend fun getAllChannels(): List<Channel>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): Channel?

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%'")
    suspend fun searchChannels(query: String): List<Channel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Update
    suspend fun updateChannel(channel: Channel)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY displayName ASC")
    suspend fun getAllContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE displayName LIKE '%' || :query || '%'")
    suspend fun searchContacts(query: String): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}
