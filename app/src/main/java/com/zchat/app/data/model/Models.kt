package com.zchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ============ USER ============
@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val isPremium: Boolean = false,
    val premiumType: String = "", // "BASIC" or "GOODPLAN"
    val premiumExpiry: Long = 0,
    val hasStarBadge: Boolean = false,
    val allowContactsView: Boolean = true, // разрешить просмотр контактов
    val allowedContactsOnly: Boolean = false, // только разрешённые контакты
    val isAnonymous: Boolean = false, // анонимность для поиска
    val customStarIcon: String = "" // путь к кастомной звезде
) {
    constructor() : this("", "", "", "", "", "", false, 0, false, "", 0, false, true, false, false, "")
}

// ============ MESSAGE ============
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long = 0
) {
    constructor() : this("", "", "", "", 0, false, false, false, false, 0)
}

// ============ CALL ============
@Entity(tableName = "calls")
data class Call(
    @PrimaryKey val id: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0,
    val duration: Long = 0,
    val type: String = "VOICE", // VOICE or VIDEO
    val isRecorded: Boolean = false,
    val recordingPath: String = "",
    val status: String = "RINGING", // RINGING, ACCEPTED, MISSED, ENDED
    val callerName: String = "",
    val receiverName: String = "",
    val callerAvatar: String = "",
    val receiverAvatar: String = ""
) {
    constructor() : this("", "", "", 0, 0, "VOICE", false, "", "RINGING", "", "", "", "")
    
    fun isMissed(): Boolean = status == "MISSED"
    fun isIncoming(currentUserId: String): Boolean = receiverId == currentUserId
    fun isOutgoing(currentUserId: String): Boolean = callerId == currentUserId
}

// ============ CHANNEL ============
@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val avatarUrl: String = "",
    val subscribersCount: Long = 0,
    val createdAt: Long = 0,
    val isVerified: Boolean = false,
    val category: String = "" // entertainment, news, education, etc.
) {
    constructor() : this("", "", "", "", "", "", 0, 0, false, "")
}

@Entity(tableName = "channel_posts")
data class ChannelPost(
    @PrimaryKey val id: String = "",
    val channelId: String = "",
    val authorId: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0,
    val viewsCount: Long = 0,
    val likesCount: Long = 0
) {
    constructor() : this("", "", "", "", "", 0, 0, 0)
}

@Entity(tableName = "channel_subscriptions")
data class ChannelSubscription(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val channelId: String = "",
    val subscribedAt: Long = 0
) {
    constructor() : this("", "", "", 0)
}

// ============ CONTACT ============
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String = "",
    val userId: String = "", // владелец контакта (текущий пользователь)
    val contactUserId: String = "", // uid пользователя в приложении
    val displayName: String = "", // отображаемое имя
    val phoneNumber: String = "",
    val customAvatarUrl: String = "", // кастомный аватар
    val isBlocked: Boolean = false,
    val isAllowed: Boolean = true, // разрешено видеть профиль
    val addedAt: Long = 0
) {
    constructor() : this("", "", "", "", "", "", false, true, 0)
}

// ============ PREMIUM SUBSCRIPTION ============
@Entity(tableName = "premium_subscriptions")
data class PremiumSubscription(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val type: String = "", // BASIC or GOODPLAN
    val startDate: Long = 0,
    val endDate: Long = 0, // 0 = навсегда
    val price: Double = 0.0,
    val currency: String = "USD",
    val isActive: Boolean = true,
    val paymentMethod: String = ""
) {
    constructor() : this("", "", "", 0, 0, 0.0, "USD", true, "")
    
    fun isLifetime(): Boolean = endDate == 0L
    fun isExpired(): Boolean = !isActive || (endDate > 0 && System.currentTimeMillis() > endDate)
}

// ============ OTHER MODELS ============
@Entity(tableName = "chat_folders")
data class ChatFolder(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val icon: String = "folder",
    val includedChats: String = "",
    val order: Int = 0
) {
    constructor() : this("", "", "folder", "", 0)
}

data class CallSignal(
    val id: String = "",
    val callId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "offer",
    val sdp: String = "",
    val iceCandidates: String = "",
    val timestamp: Long = 0
) {
    constructor() : this("", "", "", "", "offer", "", "", 0)
}

data class AppSettings(
    val theme: Int = 0,
    val designStyle: Int = 1,
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
    val premiumType: String = "", // BASIC or GOODPLAN
    val autoTranslate: Boolean = false,
    val targetLanguage: String = "ru",
    val language: String = "ru",
    val hasStarBadge: Boolean = false,
    val allowedContactsOnly: Boolean = false,
    val isAnonymous: Boolean = false
)

// ============ PREMIUM PRICING ============
object PremiumPricing {
    const val BASIC_MONTHLY = 2.0 // $2/месяц
    const val BASIC_LIFETIME = 6.0 // $6 навсегда
    const val GOODPLAN_MONTHLY = 5.0 // $5/месяц
    const val GOODPLAN_LIFETIME = 15.0 // $15 навсегда
    const val CURRENCY = "USD"
}
