package com.uniandes.travelhub.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {

    @POST("api/v1/me/devices")
    suspend fun registerDevice(@Body body: DeviceRegistrationRequest)

    @DELETE("api/v1/me/devices/{token}")
    suspend fun revokeDevice(@Path("token") token: String)

    @GET("api/v1/me/notification-preferences")
    suspend fun getPreferences(): NotificationPreferencesDto

    @PATCH("api/v1/me/notification-preferences")
    suspend fun updatePreferences(
        @Body body: NotificationPreferencesUpdateRequest,
    ): NotificationPreferencesDto

    @GET("api/v1/me/notifications")
    suspend fun listNotifications(
        @Query("filter") filter: String = "all",
        @Query("limit") limit: Int = 50,
    ): NotificationListResponse

    @POST("api/v1/me/notifications/{auditId}/opened")
    suspend fun markOpened(@Path("auditId") auditId: String)
}

@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    val token: String,
    val platform: String = "android",
    val app_version: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesDto(
    val status_changes_enabled: Boolean,
    val arrival_reminders_enabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class NotificationPreferencesUpdateRequest(
    val status_changes_enabled: Boolean? = null,
    val arrival_reminders_enabled: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationListResponse(
    val items: List<NotificationItemDto>,
    val next_cursor: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationItemDto(
    val id: String,
    val notification_id: String?,
    val title: String,
    val body: String,
    val entity_type: String,
    val entity_id: String,
    val delivery_status: String,
    val created_at: String,
    val is_read: Boolean,
)
