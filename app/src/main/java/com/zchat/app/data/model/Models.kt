package com.zchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey var uid: String = "",
    var email: String = "",
    var username: String = "",
    var phoneNumber: String = "",
    var avatarUrl: String = "",
    var bio: String = "",
    var isOnline: Boolean = false,
    var lastSeen: Long = 0,
    var isPremium: Boolean = false
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", "", "", false, 0, false)
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var content: String = "",
    var timestamp: Long = 0,
    var isRead: Boolean = false,
    var isSynced: Boolean = false
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", 0, false, false)
}

data class MessageWithSync(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var content: String = "",
    var timestamp: Long = 0,
    var isRead: Boolean = false,
    var isSynced: Boolean = false
)

@Entity(tableName = "calls")
data class Call(
    @PrimaryKey var id: String = "",
    var callerId: String = "",
    var receiverId: String = "",
    var timestamp: Long = 0,
    var duration: Long = 0,
    var type: String = "VOICE",
    var isRecorded: Boolean = false,
    var recordingPath: String = ""
) {
    constructor() : this("", "", "", 0, 0, "VOICE", false, "")
}

@Entity(tableName = "chat_folders")
data class ChatFolder(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var icon: String = "folder",
    var includedChats: String = "",
    var order: Int = 0
) {
    constructor() : this("", "", "folder", "", 0)
}
