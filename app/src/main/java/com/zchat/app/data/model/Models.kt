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
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long = 0
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
    val recordingPath: String = "",
    val status: String = "RINGING",
    val callerName: String = "",
    val receiverName: String = ""
)

@Entity(tableName = "chat_folders")
data class ChatFolder(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "folder",
    val includedChats: String = "",
    val order: Int = 0
)

data class CallSignal(
    val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "offer",
    val sdp: String = "",
    val iceCandidates: String = "",
    val timestamp: Long = 0
)

data class AppSettings(
    val theme: Int = 0,
    val designStyle: Int = 1, // 1 = Classic, 2 = Modern, 3 = Neon
    val chatBackground: String = "default",
    val enableAnimations: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val appLockEnabled: Boolean = false,
    val notificationSound: String = "default",
    val announceCallerName: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val batterySaverMode: Int = 0,
    val batterySaverThreshold: Int = 30,
    val premiumEnabled: Boolean = false,
    val autoTranslate: Boolean = false,
    val targetLanguage: String = "ru",
    val language: String = "ru"
)
