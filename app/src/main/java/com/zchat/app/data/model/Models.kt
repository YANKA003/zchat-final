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
    val isPremium: Boolean = false,
    val premiumType: String = "", // "BASIC" or "GOODPLAN"
    val premiumExpiry: Long = 0
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", "", "", false, 0, false, "", 0)
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isSynced: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    constructor() : this("", "", "", "", 0, false, false, false, false)
}

@Entity(tableName = "calls")
data class Call(
    @PrimaryKey val id: String,
    val callerId: String,
    val callerName: String = "",
    val callerAvatar: String = "",
    val receiverId: String,
    val timestamp: Long,
    val duration: Long = 0,
    val type: String = "VOICE", // "VOICE" or "VIDEO"
    val status: String = "ENDED", // "INCOMING", "OUTGOING", "MISSED", "ENDED"
    val isRecorded: Boolean = false,
    val recordingPath: String = ""
) {
    constructor() : this("", "", "", "", "", 0, 0, "VOICE", "ENDED", false, "")
}

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val avatarUrl: String = "",
    val ownerId: String,
    val subscribersCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0, 0)
}

@Entity(tableName = "channel_messages")
data class ChannelMessage(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String = "",
    val senderAvatar: String = "",
    val content: String,
    val timestamp: Long,
    val isEdited: Boolean = false
) {
    constructor() : this("", "", "", "", "", "", 0, false)
}

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val userId: String, // Link to User in Firebase
    val displayName: String,
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val isRegistered: Boolean = false
) {
    constructor() : this("", "", "", "", "", false)
}

data class AppSettings(
    val theme: Int = 0, // 0=Classic, 1=Modern, 2=Neon, 3=Childish
    val language: String = "en",
    val chatBackground: String = "default",
    val enableAnimations: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val appLockEnabled: Boolean = false,
    val notificationSound: String = "default",
    val announceCallerName: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val batterySaverMode: Int = 0,
    val batterySaverThreshold: Int = 30,
    val autoTranslate: Boolean = false,
    val targetLanguage: String = "en"
)
