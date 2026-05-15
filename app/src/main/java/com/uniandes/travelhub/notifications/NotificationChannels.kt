package com.uniandes.travelhub.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.uniandes.travelhub.R

object NotificationChannels {
    const val RESERVATION_STATUS = "reservation_status"
    const val ARRIVAL_REMINDER = "arrival_reminder"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                RESERVATION_STATUS,
                context.getString(R.string.notification_channel_status_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.notification_channel_status_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ARRIVAL_REMINDER,
                context.getString(R.string.notification_channel_arrival_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notification_channel_arrival_desc) }
        )
    }
}
