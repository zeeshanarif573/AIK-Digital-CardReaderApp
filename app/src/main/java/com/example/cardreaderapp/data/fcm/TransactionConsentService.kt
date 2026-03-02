package com.example.cardreaderapp.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cardreaderapp.CardReaderApplication
import com.example.cardreaderapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives transaction consent push notifications. Shows notification with Approve/Reject actions
 * and sends user decision to the mock backend (via repository).
 */
class TransactionConsentService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val transactionId = message.data["transactionId"] ?: message.data["nonce"] ?: return
        val title = message.notification?.title ?: "Transaction consent"
        val body = message.notification?.body ?: "Approve or reject this transaction."
        createNotificationChannel()
        showConsentNotification(transactionId, title, body)
    }

    override fun onNewToken(token: String) {
        // Send token to server if needed
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction consent",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Transaction approval requests" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun showConsentNotification(transactionId: String, title: String, body: String) {
        val approveIntent = Intent(this, ConsentActionReceiver::class.java).apply {
            action = ACTION_APPROVE
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val rejectIntent = Intent(this, ConsentActionReceiver::class.java).apply {
            action = ACTION_REJECT
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val approvePending = PendingIntent.getBroadcast(this, transactionId.hashCode(), approveIntent, flags)
        val rejectPending = PendingIntent.getBroadcast(this, transactionId.hashCode() + 1, rejectIntent, flags)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
            )
            .addAction(android.R.drawable.ic_menu_upload, "Approve", approvePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_BASE + transactionId.hashCode().and(0x7FFF), notification)
    }

    companion object {
        const val CHANNEL_ID = "transaction_consent"
        private const val NOTIFICATION_ID_BASE = 2000
        const val ACTION_APPROVE = "com.example.cardreaderapp.ACTION_APPROVE"
        const val ACTION_REJECT = "com.example.cardreaderapp.ACTION_REJECT"
        const val EXTRA_TRANSACTION_ID = "transactionId"
    }
}
