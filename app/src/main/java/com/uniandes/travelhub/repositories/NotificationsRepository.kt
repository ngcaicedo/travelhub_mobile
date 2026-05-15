package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.network.DeviceRegistrationRequest
import com.uniandes.travelhub.network.NotificationItemDto
import com.uniandes.travelhub.network.NotificationPreferencesDto
import com.uniandes.travelhub.network.NotificationPreferencesUpdateRequest
import com.uniandes.travelhub.network.NotificationsApi

class NotificationsRepository(private val api: NotificationsApi) {

    suspend fun registerDevice(token: String, appVersion: String?) {
        api.registerDevice(
            DeviceRegistrationRequest(
                token = token,
                platform = "android",
                app_version = appVersion,
            )
        )
    }

    suspend fun revokeDevice(token: String) = api.revokeDevice(token)

    suspend fun getPreferences(): NotificationPreferencesDto = api.getPreferences()

    suspend fun updatePreferences(
        statusChanges: Boolean? = null,
        arrivalReminders: Boolean? = null,
    ): NotificationPreferencesDto = api.updatePreferences(
        NotificationPreferencesUpdateRequest(
            status_changes_enabled = statusChanges,
            arrival_reminders_enabled = arrivalReminders,
        )
    )

    suspend fun listNotifications(unreadOnly: Boolean): List<NotificationItemDto> =
        api.listNotifications(filter = if (unreadOnly) "unread" else "all").items

    suspend fun markOpened(auditId: String) = api.markOpened(auditId)
}
