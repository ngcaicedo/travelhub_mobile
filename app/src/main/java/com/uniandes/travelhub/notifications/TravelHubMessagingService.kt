package com.uniandes.travelhub.notifications

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uniandes.travelhub.BuildConfig
import com.uniandes.travelhub.MainActivity
import com.uniandes.travelhub.R
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.RetrofitFactory
import com.uniandes.travelhub.repositories.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TravelHubMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val repository by lazy {
        NotificationsRepository(RetrofitFactory.notificationsApi)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        scope.launch {
            val tokenStore = AuthTokenStore.getInstance(applicationContext)
                .also(RetrofitFactory::init)
            val authToken = tokenStore.tokenFlow.firstOrNull()
            if (authToken.isNullOrBlank()) {
                Log.i(TAG, "No auth token yet, skipping device registration")
                return@launch
            }
            runCatching {
                repository.registerDevice(token, BuildConfig.VERSION_NAME)
            }.onFailure { Log.w(TAG, "Failed to register device", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: applicationContext.getString(R.string.notification_fallback_title)
        val body = message.notification?.body
            ?: data["body"]
            ?: ""
        val deepLink = data["deep_link"]
        val channelId = data["channel_id"] ?: NotificationChannels.RESERVATION_STATUS
        val auditId = data["audit_id"] ?: data["notification_id"]

        NotificationChannels.ensureCreated(applicationContext)

        val intent = if (!deepLink.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        if (auditId != null) {
            intent.putExtra(EXTRA_AUDIT_ID, auditId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (auditId ?: System.currentTimeMillis().toString()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (auditId != null) {
            scope.launch { PendingNotificationStore(applicationContext).add(auditId) }
        }

        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(auditId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
        }.onFailure { Log.w(TAG, "Failed to display notification", it) }
    }

    companion object {
        private const val TAG = "TravelHubFcm"
        const val EXTRA_AUDIT_ID = "travelhub_audit_id"
    }
}
