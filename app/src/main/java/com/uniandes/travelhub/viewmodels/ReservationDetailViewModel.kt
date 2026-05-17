package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.repositories.ReservationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReservationDetailUiState {
    data object Loading : ReservationDetailUiState
    data class Success(val reservation: ReservationResponse) : ReservationDetailUiState
    data class Error(val message: ErrorMessage) : ReservationDetailUiState
}

/**
 * UI state for the cancel/modify action panels. Each panel walks the same
 * preview → confirm path the web uses.
 */
sealed interface CancelActionState {
    data object Idle : CancelActionState
    data object LoadingPreview : CancelActionState
    data class Preview(val data: ReservationCancellationPreviewResponse) : CancelActionState
    data object Confirming : CancelActionState
    data class Error(val message: ErrorMessage) : CancelActionState
    data object Done : CancelActionState
}

sealed interface ModifyActionState {
    data object Idle : ModifyActionState
    data object LoadingPreview : ModifyActionState
    data class Preview(val data: ReservationModificationPreviewResponse) : ModifyActionState
    data object Confirming : ModifyActionState
    data class Error(val message: ErrorMessage) : ModifyActionState
    data object Done : ModifyActionState
}

class ReservationDetailViewModel(
    private val reservationId: String,
    private val repository: ReservationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReservationDetailUiState>(ReservationDetailUiState.Loading)
    val uiState: StateFlow<ReservationDetailUiState> = _uiState.asStateFlow()

    private val _cancelState = MutableStateFlow<CancelActionState>(CancelActionState.Idle)
    val cancelState: StateFlow<CancelActionState> = _cancelState.asStateFlow()

    private val _modifyState = MutableStateFlow<ModifyActionState>(ModifyActionState.Idle)
    val modifyState: StateFlow<ModifyActionState> = _modifyState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ReservationDetailUiState.Loading
            repository.getById(reservationId)
                .onSuccess { _uiState.value = ReservationDetailUiState.Success(it) }
                .onFailure { error ->
                    _uiState.value = ReservationDetailUiState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservation_detail_error)
                    )
                }
        }
    }

    // --- Cancel ---

    fun startCancel() {
        viewModelScope.launch {
            _cancelState.value = CancelActionState.LoadingPreview
            repository.previewCancellation(reservationId)
                .onSuccess { _cancelState.value = CancelActionState.Preview(it) }
                .onFailure { error ->
                    _cancelState.value = CancelActionState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservation_action_cancel_error)
                    )
                }
        }
    }

    fun dismissCancel() {
        _cancelState.value = CancelActionState.Idle
    }

    fun confirmCancel(reason: String? = null) {
        viewModelScope.launch {
            _cancelState.value = CancelActionState.Confirming
            repository.confirmCancellation(reservationId, reason)
                .onSuccess {
                    _cancelState.value = CancelActionState.Done
                    load()
                }
                .onFailure { error ->
                    _cancelState.value = CancelActionState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservation_action_cancel_error)
                    )
                }
        }
    }

    // --- Modify ---

    fun previewModify(checkIn: String, checkOut: String, guests: Int) {
        viewModelScope.launch {
            _modifyState.value = ModifyActionState.LoadingPreview
            repository.previewModification(reservationId, checkIn, checkOut, guests)
                .onSuccess { _modifyState.value = ModifyActionState.Preview(it) }
                .onFailure { error ->
                    _modifyState.value = ModifyActionState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservation_action_modify_error)
                    )
                }
        }
    }

    fun dismissModify() {
        _modifyState.value = ModifyActionState.Idle
    }

    fun confirmModify(checkIn: String, checkOut: String, guests: Int) {
        viewModelScope.launch {
            _modifyState.value = ModifyActionState.Confirming
            repository.confirmModification(reservationId, checkIn, checkOut, guests)
                .onSuccess {
                    _modifyState.value = ModifyActionState.Done
                    load()
                }
                .onFailure { error ->
                    _modifyState.value = ModifyActionState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservation_action_modify_error)
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
            ReservationDetailViewModel(reservationId, repository) as T
    }
}
