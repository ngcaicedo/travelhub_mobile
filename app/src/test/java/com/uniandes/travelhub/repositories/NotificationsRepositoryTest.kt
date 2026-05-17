package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.network.DeviceRegistrationRequest
import com.uniandes.travelhub.network.NotificationItemDto
import com.uniandes.travelhub.network.NotificationListResponse
import com.uniandes.travelhub.network.NotificationPreferencesDto
import com.uniandes.travelhub.network.NotificationPreferencesUpdateRequest
import com.uniandes.travelhub.network.NotificationsApi
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationsRepositoryTest {

    private lateinit var api: NotificationsApi
    private lateinit var repository: NotificationsRepository

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        repository = NotificationsRepository(api)
    }

    @Test
    fun `registerDevice posts platform android with version`() = runTest {
        val captured = slot<DeviceRegistrationRequest>()
        coEvery { api.registerDevice(capture(captured)) } just Runs

        repository.registerDevice("fcm-token-xyz", "1.4.0")

        coVerify { api.registerDevice(any()) }
        assertEquals("fcm-token-xyz", captured.captured.token)
        assertEquals("android", captured.captured.platform)
        assertEquals("1.4.0", captured.captured.app_version)
    }

    @Test
    fun `registerDevice accepts null app_version`() = runTest {
        val captured = slot<DeviceRegistrationRequest>()
        coEvery { api.registerDevice(capture(captured)) } just Runs

        repository.registerDevice("fcm-token-xyz", null)

        assertEquals(null, captured.captured.app_version)
    }

    @Test
    fun `revokeDevice forwards token to api`() = runTest {
        coEvery { api.revokeDevice("tok") } just Runs
        repository.revokeDevice("tok")
        coVerify { api.revokeDevice("tok") }
    }

    @Test
    fun `getPreferences returns dto from api`() = runTest {
        val dto = NotificationPreferencesDto(true, false)
        coEvery { api.getPreferences() } returns dto

        val result = repository.getPreferences()

        assertEquals(dto, result)
    }

    @Test
    fun `updatePreferences forwards both flags`() = runTest {
        val captured = slot<NotificationPreferencesUpdateRequest>()
        val dto = NotificationPreferencesDto(false, true)
        coEvery { api.updatePreferences(capture(captured)) } returns dto

        val result = repository.updatePreferences(statusChanges = false, arrivalReminders = true)

        assertEquals(dto, result)
        assertEquals(false, captured.captured.status_changes_enabled)
        assertEquals(true, captured.captured.arrival_reminders_enabled)
    }

    @Test
    fun `updatePreferences with null leaves field null on request`() = runTest {
        val captured = slot<NotificationPreferencesUpdateRequest>()
        coEvery { api.updatePreferences(capture(captured)) } returns NotificationPreferencesDto(true, true)

        repository.updatePreferences(statusChanges = false, arrivalReminders = null)

        assertEquals(false, captured.captured.status_changes_enabled)
        assertEquals(null, captured.captured.arrival_reminders_enabled)
    }

    @Test
    fun `listNotifications maps unreadOnly true to filter unread`() = runTest {
        coEvery {
            api.listNotifications(filter = "unread", limit = any())
        } returns NotificationListResponse(items = emptyList())

        repository.listNotifications(unreadOnly = true)

        coVerify { api.listNotifications(filter = "unread", limit = any()) }
    }

    @Test
    fun `listNotifications unreadOnly false maps to filter all`() = runTest {
        coEvery {
            api.listNotifications(filter = "all", limit = any())
        } returns NotificationListResponse(items = emptyList())

        repository.listNotifications(unreadOnly = false)

        coVerify { api.listNotifications(filter = "all", limit = any()) }
    }

    @Test
    fun `listNotifications returns items from response`() = runTest {
        val item = NotificationItemDto(
            id = "1",
            notification_id = null,
            title = "Reserva confirmada",
            body = "ok",
            entity_type = "reservation",
            entity_id = "r1",
            delivery_status = "sent",
            created_at = "2026-05-09T10:00:00Z",
            is_read = false,
        )
        coEvery {
            api.listNotifications(filter = any(), limit = any())
        } returns NotificationListResponse(items = listOf(item))

        val result = repository.listNotifications(unreadOnly = false)

        assertEquals(listOf(item), result)
    }

    @Test
    fun `markOpened forwards id to api`() = runTest {
        coEvery { api.markOpened("audit-1") } just Runs
        repository.markOpened("audit-1")
        coVerify { api.markOpened("audit-1") }
    }
}
