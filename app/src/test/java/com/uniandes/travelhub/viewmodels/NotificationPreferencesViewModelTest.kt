package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.network.NotificationPreferencesDto
import com.uniandes.travelhub.repositories.NotificationsRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads preferences from repository`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.getPreferences() } returns NotificationPreferencesDto(
            status_changes_enabled = true,
            arrival_reminders_enabled = false,
        )

        val vm = NotificationPreferencesViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.statusChanges)
        assertFalse(state.arrivalReminders)
    }

    @Test
    fun `setStatusChanges optimistically toggles state and persists`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.getPreferences() } returns NotificationPreferencesDto(
            status_changes_enabled = true,
            arrival_reminders_enabled = true,
        )
        coEvery {
            repository.updatePreferences(statusChanges = false, arrivalReminders = null)
        } returns NotificationPreferencesDto(
            status_changes_enabled = false,
            arrival_reminders_enabled = true,
        )

        val vm = NotificationPreferencesViewModel(repository)
        advanceUntilIdle()

        vm.setStatusChanges(false)
        // optimistic update sin esperar
        assertFalse(vm.state.value.statusChanges)
        advanceUntilIdle()

        coVerify { repository.updatePreferences(statusChanges = false, arrivalReminders = null) }
        assertFalse(vm.state.value.statusChanges)
    }

    @Test
    fun `setArrivalReminders rolls back to previous value when API fails`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.getPreferences() } returns NotificationPreferencesDto(
            status_changes_enabled = true,
            arrival_reminders_enabled = true,
        )
        coEvery {
            repository.updatePreferences(statusChanges = null, arrivalReminders = false)
        } throws IOException("offline")

        val vm = NotificationPreferencesViewModel(repository)
        advanceUntilIdle()

        vm.setArrivalReminders(false)
        advanceUntilIdle()

        // rollback al valor anterior
        assertTrue(vm.state.value.arrivalReminders)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `getPreferences error sets error message`() = runTest {
        val repository = mockk<NotificationsRepository>()
        coEvery { repository.getPreferences() } throws IOException("net err")

        val vm = NotificationPreferencesViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals("net err", state.error)
    }
}
