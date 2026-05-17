package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.network.NotificationItemDto
import com.uniandes.travelhub.repositories.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(val items: List<NotificationItemDto>) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

enum class NotificationsFilter { ALL, UNREAD }

class NotificationsViewModel(
    private val repository: NotificationsRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(NotificationsFilter.ALL)
    val filter: StateFlow<NotificationsFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun selectFilter(value: NotificationsFilter) {
        _filter.value = value
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            runCatching {
                repository.listNotifications(unreadOnly = _filter.value == NotificationsFilter.UNREAD)
            }.onSuccess { _uiState.value = NotificationsUiState.Success(it) }
                .onFailure { _uiState.value = NotificationsUiState.Error(it.message ?: "Error") }
        }
    }

    fun markOpened(auditId: String) {
        viewModelScope.launch {
            runCatching { repository.markOpened(auditId) }
            load()
        }
    }

    class Factory(private val repository: NotificationsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotificationsViewModel(repository) as T
    }
}
