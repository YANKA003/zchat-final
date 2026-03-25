package com.zchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val email: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val isPremium: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isSynced: Boolean = false
)

// Add missing fields to Message
data class MessageWithSync(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "calls")
data class Call(
    @PrimaryKey val id: String,
    val callerId: String,
    val receiverId: String,
    val timestamp: Long,
    val duration: Long = 0,
    val type: String = "VOICE",
    val isRecorded: Boolean = false,
    val recordingPath: String = ""
)

@Entity(tableName = "chat_folders")
data class ChatFolder(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "folder",
    val includedChats: String = "",
    val order: Int = 0
)
