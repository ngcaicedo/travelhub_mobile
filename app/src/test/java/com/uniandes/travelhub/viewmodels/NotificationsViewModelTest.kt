package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
import com.uniandes.travelhub.network.NotificationItemDto
import com.uniandes.travelhub.repositories.NotificationsRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun item(id: String, isRead: Boolean = false): NotificationItemDto =
        NotificationItemDto(
            id = id,
            notification_id = id,
            title = "Reserva confirmada",
            body = "ok",
            entity_type = "reservation",
            entity_id = "res-$id",
            delivery_status = "sent",
            created_at = "2026-05-09T10:00:00+00:00",
            is_read = isRead,
        )

    @Test
    fun `init loads notifications and emits Success`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } returns listOf(item("1"), item("2"))

        val vm = NotificationsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is NotificationsUiState.Success)
        assertEquals(2, (state as NotificationsUiState.Success).items.size)
    }

    @Test
    fun `repository error transitions state to Error`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } throws IOException("network down")

        val vm = NotificationsViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is NotificationsUiState.Error)
        assertEquals("network down", (state as NotificationsUiState.Error).message)
    }

    @Test
    fun `selectFilter UNREAD reloads with unreadOnly true`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } returns listOf(item("1"))
        coEvery { repository.listNotifications(unreadOnly = true) } returns listOf(item("2"))

        val vm = NotificationsViewModel(repository)
        advanceUntilIdle()

        vm.selectFilter(NotificationsFilter.UNREAD)
        advanceUntilIdle()

        assertEquals(NotificationsFilter.UNREAD, vm.filter.value)
        coVerify(exactly = 1) { repository.listNotifications(unreadOnly = true) }
    }

    @Test
    fun `markOpened calls repository and reloads list`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } returns listOf(item("1"))
        coEvery { repository.markOpened("1") } just Runs

        val vm = NotificationsViewModel(repository)
        advanceUntilIdle()

        vm.markOpened("1")
        advanceUntilIdle()

        coVerify { repository.markOpened("1") }
        coVerify(atLeast = 2) { repository.listNotifications(unreadOnly = false) }
    }

    @Test
    fun `markOpened reloads even if API call fails`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } returns listOf(item("1"))
        coEvery { repository.markOpened("1") } throws IOException("offline")

        val vm = NotificationsViewModel(repository)
        advanceUntilIdle()
        vm.markOpened("1")
        advanceUntilIdle()

        coVerify(atLeast = 2) { repository.listNotifications(unreadOnly = false) }
    }

    @Test
    fun `state flow emits Loading then Success`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.listNotifications(unreadOnly = false) } returns emptyList()

        val vm = NotificationsViewModel(repository)
        vm.uiState.test {
            // initial value expuesto antes del primer launch
            assertEquals(NotificationsUiState.Loading, awaitItem())
            advanceUntilIdle()
            val emitted = awaitItem()
            assertTrue(emitted is NotificationsUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
