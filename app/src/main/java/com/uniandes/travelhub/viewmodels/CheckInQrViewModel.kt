package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import com.uniandes.travelhub.repositories.ReservationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CheckInQrUiState {
    data object Loading : CheckInQrUiState
    data class Available(val payload: CachedCheckInQr, val isOffline: Boolean) : CheckInQrUiState
    data class Invalidated(val message: ErrorMessage) : CheckInQrUiState
    data class Error(val message: ErrorMessage) : CheckInQrUiState
}

class CheckInQrViewModel(
    private val reservationId: String,
    private val repository: ReservationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckInQrUiState>(CheckInQrUiState.Loading)
    val uiState: StateFlow<CheckInQrUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CheckInQrUiState.Loading
            repository.getCheckInQr(reservationId)
                .onSuccess { artifact ->
                    _uiState.value = if (artifact.requiresRefresh) {
                        CheckInQrUiState.Invalidated(
                            ErrorMessage.Resource(R.string.checkin_qr_invalidated)
                        )
                    } else {
                        CheckInQrUiState.Available(
                            payload = artifact.cache,
                            isOffline = artifact.isOffline,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = CheckInQrUiState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.checkin_qr_error)
                    )
                }
        }
    }

    class Factory(
        private val reservationId: String,
        private val repository: ReservationsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckInQrViewModel(reservationId, repository) as T
    }
}
