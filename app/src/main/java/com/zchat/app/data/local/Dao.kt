package com.zchat.app.data.local

import androidx.room.*
import com.zchat.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Query("SELECT * FROM users WHERE uid != :currentUserId")
    fun getAllUsers(currentUserId: String): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Query("SELECT * FROM messages WHERE (senderId = :userId AND receiverId = :currentUserId) OR (senderId = :currentUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getMessagesWithUser(userId: String, currentUserId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<Message>
    
    @Query("UPDATE messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markAsSynced(messageId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call)
    
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<Call>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ChatFolder)
    
    @Query("SELECT * FROM chat_folders ORDER BY `order` ASC")
    fun getAllFolders(): Flow<List<ChatFolder>>
}

@Database(entities = [User::class, Message::class, Call::class, ChatFolder::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
