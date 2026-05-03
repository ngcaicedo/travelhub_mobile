package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationStatusGroup
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import com.uniandes.travelhub.repositories.ReservationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReservationsListUiState {
    data object Loading : ReservationsListUiState
    data class Success(val reservations: List<ReservationWithDetailsResponse>) : ReservationsListUiState
    data class Error(val message: ErrorMessage) : ReservationsListUiState
}

class ReservationsListViewModel(
    private val repository: ReservationsRepository,
) : ViewModel() {

    private val _selectedGroup = MutableStateFlow<ReservationStatusGroup?>(ReservationStatusGroup.ACTIVE)
    val selectedGroup: StateFlow<ReservationStatusGroup?> = _selectedGroup.asStateFlow()

    private val _uiState = MutableStateFlow<ReservationsListUiState>(ReservationsListUiState.Loading)
    val uiState: StateFlow<ReservationsListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectGroup(group: ReservationStatusGroup?) {
        _selectedGroup.value = group
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ReservationsListUiState.Loading
            repository.listForCurrentUser(_selectedGroup.value)
                .onSuccess { reservations ->
                    val sorted = reservations.sortedBy { it.reservation.checkInDate }
                    _uiState.value = ReservationsListUiState.Success(sorted)
                }
                .onFailure { error ->
                    _uiState.value = ReservationsListUiState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.reservations_list_error)
                    )
                }
        }
    }

    class Factory(private val repository: ReservationsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReservationsListViewModel(repository) as T
    }
}
