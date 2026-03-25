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
    var isPremium: Boolean = false,
    var fcmToken: String = ""
) {
    constructor() : this("", "", "", "", "", "", false, 0, false, "")
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var content: String = "",
    var timestamp: Long = 0,
    var isRead: Boolean = false,
    var isSynced: Boolean = false,
    var isEdited: Boolean = false,
    var isDeleted: Boolean = false,
    var editedAt: Long = 0
) {
    constructor() : this("", "", "", "", 0, false, false, false, false, 0)
}

@Entity(tableName = "calls")
data class Call(
    @PrimaryKey var id: String = "",
    var callerId: String = "",
    var callerName: String = "",
    var receiverId: String = "",
    var receiverName: String = "",
    var timestamp: Long = 0,
    var duration: Long = 0,
    var type: String = "VOICE",
    var status: String = "MISSED",
    var isRecorded: Boolean = false,
    var recordingPath: String = ""
) {
    constructor() : this("", "", "", "", "", 0, 0, "VOICE", "MISSED", false, "")
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

// For WebRTC signaling
data class CallSignal(
    var id: String = "",
    var callerId: String = "",
    var receiverId: String = "",
    var type: String = "", // "offer", "answer", "ice-candidate"
    var sdp: String = "",
    var iceCandidates: String = "",
    var timestamp: Long = 0
) {
    constructor() : this("", "", "", "", "", "", 0)
}
