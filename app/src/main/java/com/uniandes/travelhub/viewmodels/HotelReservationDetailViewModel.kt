package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.hotelreservations.HotelReservationCancellationReason
import com.uniandes.travelhub.models.hotelreservations.HotelReservationDetailResponse
import com.uniandes.travelhub.models.hotelreservations.hasAction
import com.uniandes.travelhub.repositories.HotelReservationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HotelReservationDetailUiState {
    data object Loading : HotelReservationDetailUiState
    data class Success(val detail: HotelReservationDetailResponse) : HotelReservationDetailUiState
    data class Error(val message: ErrorMessage) : HotelReservationDetailUiState
}

sealed interface HotelReservationActionState {
    data object Idle : HotelReservationActionState
    data object Working : HotelReservationActionState
    data class Success(val message: ErrorMessage) : HotelReservationActionState
    data class Error(val message: ErrorMessage) : HotelReservationActionState
}

class HotelReservationDetailViewModel(
    private val reservationId: String,
    private val repository: HotelReservationsRepository,
    private val localeProvider: () -> String = { "es" },
) : ViewModel() {

    private val _uiState = MutableStateFlow<HotelReservationDetailUiState>(HotelReservationDetailUiState.Loading)
    val uiState: StateFlow<HotelReservationDetailUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<HotelReservationActionState>(HotelReservationActionState.Idle)
    val actionState: StateFlow<HotelReservationActionState> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HotelReservationDetailUiState.Loading
            repository.getReservationDetail(reservationId)
                .onSuccess { _uiState.value = HotelReservationDetailUiState.Success(it) }
                .onFailure { error ->
                    _uiState.value = HotelReservationDetailUiState.Error(
                        error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.hotel_reservations_error_load_detail),
                    )
                }
        }
    }

    fun clearActionMessage() {
        _actionState.value = HotelReservationActionState.Idle
    }

    fun canConfirm(): Boolean = (uiState.value as? HotelReservationDetailUiState.Success)
        ?.detail?.availableActions?.hasAction("confirm") == true

    fun canCancel(): Boolean = (uiState.value as? HotelReservationDetailUiState.Success)
        ?.detail?.availableActions?.hasAction("cancel") == true

    fun confirmReservation() {
        viewModelScope.launch {
            _actionState.value = HotelReservationActionState.Working
            repository.confirmReservation(reservationId, locale = localeProvider())
                .onSuccess {
                    _actionState.value = HotelReservationActionState.Success(
                        ErrorMessage.Resource(R.string.hotel_reservations_action_confirm_success),
                    )
                    load()
                }
                .onFailure { error ->
                    _actionState.value = HotelReservationActionState.Error(
                        error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.hotel_reservations_action_confirm_error),
                    )
                }
        }
    }

    fun cancelReservation(reason: HotelReservationCancellationReason, note: String) {
        viewModelScope.launch {
            _actionState.value = HotelReservationActionState.Working
            repository.cancelReservation(
                reservationId = reservationId,
                reason = reason,
                note = note,
                locale = localeProvider(),
            ).onSuccess {
                _actionState.value = HotelReservationActionState.Success(
                    ErrorMessage.Resource(R.string.hotel_reservations_action_cancel_success),
                )
                load()
            }.onFailure { error ->
                _actionState.value = HotelReservationActionState.Error(
                    error.message?.let(ErrorMessage::Plain)
                        ?: ErrorMessage.Resource(R.string.hotel_reservations_action_cancel_error),
                )
            }
        }
    }

    class Factory(
        private val reservationId: String,
        private val repository: HotelReservationsRepository,
        private val localeProvider: () -> String = { "es" },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HotelReservationDetailViewModel(reservationId, repository, localeProvider) as T
    }
}
