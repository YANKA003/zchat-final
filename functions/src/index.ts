import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin
admin.initializeApp();

const db = admin.database();
const messaging = admin.messaging();

// ========================================
// 1. SEND NOTIFICATION ON NEW MESSAGE
// ========================================
export const onMessageCreated = functions.database
  .ref('chats/{chatId}/{messageId}')
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    const chatId = context.params.chatId;
    const messageId = context.params.messageId;

    console.log(`New message in chat ${chatId}: ${messageId}`);

    // Skip if message is deleted
    if (message.isDeleted) {
      return null;
    }

    const senderId = message.senderId;
    const receiverId = message.receiverId;

    // Get sender info
    const senderSnapshot = await db.ref(`users/${senderId}`).once('value');
    const sender = senderSnapshot.val();

    if (!sender) {
      console.log('Sender not found');
      return null;
    }

    // Get receiver's FCM token
    const receiverSnapshot = await db.ref(`users/${receiverId}`).once('value');
    const receiver = receiverSnapshot.val();

    if (!receiver || !receiver.fcmToken) {
      console.log('Receiver has no FCM token');
      return null;
    }

    // Don't notify if receiver is online (they'll see it in realtime)
    if (receiver.isOnline) {
      console.log('Receiver is online, skipping notification');
      return null;
    }

    // Send push notification
    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: sender.username || 'ZChat',
        body: message.content,
        icon: 'ic_notification',
        sound: 'default',
        click_action: 'OPEN_CHAT',
        tag: `chat_${senderId}`,
      },
      data: {
        type: 'message',
        senderId: senderId,
        senderName: sender.username || 'Unknown',
        messageId: messageId,
        chatId: chatId,
        click_action: 'FLUTTER_NOTIFICATION_CLICK',
      },
    };

    try {
      await messaging.sendToDevice(receiver.fcmToken, payload);
      console.log(`Notification sent to ${receiverId}`);

      // Save notification to database for history
      await db.ref(`notifications/${receiverId}`).push({
        type: 'message',
        title: sender.username,
        body: message.content,
        senderId: senderId,
        timestamp: admin.database.ServerValue.TIMESTAMP,
        read: false,
      });

      return null;
    } catch (error) {
      console.error('Error sending notification:', error);
      return null;
    }
  });

// ========================================
// 2. SEND NOTIFICATION ON INCOMING CALL
// ========================================
export const onCallCreated = functions.database
  .ref('calls/{callId}')
  .onCreate(async (snapshot, context) => {
    const call = snapshot.val();
    const callId = context.params.callId;

    console.log(`New call: ${callId}`);

    // Only notify for ringing calls
    if (call.status !== 'RINGING') {
      return null;
    }

    const callerId = call.callerId;
    const receiverId = call.receiverId;

    // Get caller info
    const callerSnapshot = await db.ref(`users/${callerId}`).once('value');
    const caller = callerSnapshot.val();

    if (!caller) {
      console.log('Caller not found');
      return null;
    }

    // Get receiver's FCM token
    const receiverSnapshot = await db.ref(`users/${receiverId}`).once('value');
    const receiver = receiverSnapshot.val();

    if (!receiver || !receiver.fcmToken) {
      console.log('Receiver has no FCM token');
      return null;
    }

    const callType = call.type === 'VIDEO' ? '📹 Видеозвонок' : '📞 Голосовой звонок';

    // Send high-priority notification for call
    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: `${caller.username || 'Unknown'}`,
        body: callType,
        icon: 'ic_call',
        sound: 'ringtone',
        click_action: 'INCOMING_CALL',
        tag: 'call',
        priority: 'high',
      },
      data: {
        type: 'call',
        callId: callId,
        callerId: callerId,
        callerName: caller.username || 'Unknown',
        callType: call.type,
        click_action: 'FLUTTER_NOTIFICATION_CLICK',
      },
    };

    const options: admin.messaging.MessagingOptions = {
      priority: 'high',
      timeToLive: 60, // 1 minute for call
    };

    try {
      await messaging.sendToDevice(receiver.fcmToken, payload, options);
      console.log(`Call notification sent to ${receiverId}`);
      return null;
    } catch (error) {
      console.error('Error sending call notification:', error);
      return null;
    }
  });

// ========================================
// 3. CLEAN UP ENDED CALLS
// ========================================
export const onCallStatusChanged = functions.database
  .ref('calls/{callId}/status')
  .onUpdate(async (change, context) => {
    const newStatus = change.after.val();
    const callId = context.params.callId;

    console.log(`Call ${callId} status changed to: ${newStatus}`);

    // If call ended, clean up call signals
    if (['ENDED', 'DECLINED', 'MISSED'].includes(newStatus)) {
      try {
        // Remove call signals
        const signalsSnapshot = await db.ref('callSignals')
          .orderByKey()
          .startAt(callId)
          .endAt(`${callId}\uf8ff`)
          .once('value');

        const updates: { [key: string]: null } = {};

        signalsSnapshot.forEach((child) => {
          updates[`callSignals/${child.key}`] = null;
        });

        if (Object.keys(updates).length > 0) {
          await db.ref().update(updates);
          console.log(`Cleaned up ${Object.keys(updates).length} call signals`);
        }

        return null;
      } catch (error) {
        console.error('Error cleaning up call signals:', error);
        return null;
      }
    }

    return null;
  });

// ========================================
// 4. UPDATE USER LAST SEEN
// ========================================
export const onUserDisconnect = functions.database
  .ref('users/{userId}/isOnline')
  .onUpdate(async (change, context) => {
    const isOnline = change.after.val();
    const userId = context.params.userId;

    if (!isOnline) {
      // User went offline, update lastSeen
      await db.ref(`users/${userId}/lastSeen`).set(admin.database.ServerValue.TIMESTAMP);
      console.log(`User ${userId} went offline`);
    }

    return null;
  });

// ========================================
// 5. CHECK PREMIUM STATUS (scheduled)
// ========================================
export const checkPremiumStatus = functions.pubsub
  .schedule('0 0 * * *') // Run daily at midnight
  .timeZone('Europe/Moscow')
  .onRun(async (context) => {
    const now = Date.now();

    // Find expired premium subscriptions
    const usersSnapshot = await db.ref('users')
      .orderByChild('premiumExpiry')
      .endAt(now)
      .once('value');

    const updates: { [key: string]: any } = {};

    usersSnapshot.forEach((child) => {
      updates[`users/${child.key}/isPremium`] = false;
      updates[`users/${child.key}/premiumExpiry`] = null;
    });

    if (Object.keys(updates).length > 0) {
      await db.ref().update(updates);
      console.log(`Updated ${Object.keys(updates).length / 2} expired premium users`);
    }

    return null;
  });

// ========================================
// 6. CLEAN OLD NOTIFICATIONS (scheduled)
// ========================================
export const cleanOldNotifications = functions.pubsub
  .schedule('0 3 * * *') // Run daily at 3 AM
  .timeZone('Europe/Moscow')
  .onRun(async (context) => {
    const oneWeekAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);

    // Find old notifications
    const notificationsSnapshot = await db.ref('notifications').once('value');

    const updates: { [key: string]: null } = {};

    notificationsSnapshot.forEach((userNotifications) => {
      userNotifications.forEach((notification) => {
        const data = notification.val();
        if (data.timestamp < oneWeekAgo) {
          updates[`notifications/${userNotifications.key}/${notification.key}`] = null;
        }
      });
    });

    if (Object.keys(updates).length > 0) {
      await db.ref().update(updates);
      console.log(`Deleted ${Object.keys(updates).length} old notifications`);
    }

    return null;
  });
