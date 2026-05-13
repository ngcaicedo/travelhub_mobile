package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.repositories.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationPreferencesState(
    val statusChanges: Boolean = true,
    val arrivalReminders: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class NotificationPreferencesViewModel(
    private val repository: NotificationsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationPreferencesState())
    val state: StateFlow<NotificationPreferencesState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { repository.getPreferences() }
                .onSuccess { dto ->
                    _state.value = NotificationPreferencesState(
                        statusChanges = dto.status_changes_enabled,
                        arrivalReminders = dto.arrival_reminders_enabled,
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message,
                    )
                }
        }
    }

    fun setStatusChanges(enabled: Boolean) = update(statusChanges = enabled)

    fun setArrivalReminders(enabled: Boolean) = update(arrivalReminders = enabled)

    private fun update(statusChanges: Boolean? = null, arrivalReminders: Boolean? = null) {
        // Optimistic UI update with rollback if request fails.
        val previous = _state.value
        _state.value = previous.copy(
            statusChanges = statusChanges ?: previous.statusChanges,
            arrivalReminders = arrivalReminders ?: previous.arrivalReminders,
            error = null,
        )
        viewModelScope.launch {
            runCatching {
                repository.updatePreferences(
                    statusChanges = statusChanges,
                    arrivalReminders = arrivalReminders,
                )
            }.onFailure { error ->
                _state.value = previous.copy(error = error.message)
            }
        }
    }

    class Factory(private val repository: NotificationsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotificationPreferencesViewModel(repository) as T
    }
}
