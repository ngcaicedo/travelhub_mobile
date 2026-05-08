package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.hotelreservations.HotelReservationListItem
import com.uniandes.travelhub.models.hotelreservations.HotelReservationPropertyOption
import com.uniandes.travelhub.models.hotelreservations.HotelReservationStatusFilter
import com.uniandes.travelhub.repositories.HotelReservationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HotelReservationsListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val properties: List<HotelReservationPropertyOption> = emptyList(),
    val selectedPropertyId: String? = null,
    val selectedStatus: HotelReservationStatusFilter = HotelReservationStatusFilter.ALL,
    val reservations: List<HotelReservationListItem> = emptyList(),
    val error: ErrorMessage? = null,
)

class HotelReservationsListViewModel(
    private val repository: HotelReservationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HotelReservationsListUiState())
    val uiState: StateFlow<HotelReservationsListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val propertiesResult = repository.listProperties()
            propertiesResult.onSuccess { properties ->
                val selectedPropertyId = _uiState.value.selectedPropertyId ?: properties.firstOrNull()?.propertyId
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        properties = properties,
                        selectedPropertyId = selectedPropertyId,
                    )
                }
                if (!selectedPropertyId.isNullOrBlank()) {
                    loadReservations(selectedPropertyId, _uiState.value.selectedStatus, initial = true)
                } else {
                    _uiState.update { it.copy(isLoading = false, reservations = emptyList()) }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.hotel_reservations_error_load_properties),
                    )
                }
            }
        }
    }

    fun onPropertySelected(propertyId: String) {
        _uiState.update { it.copy(selectedPropertyId = propertyId, error = null) }
        loadReservations(propertyId, _uiState.value.selectedStatus)
    }

    fun onStatusSelected(status: HotelReservationStatusFilter) {
        _uiState.update { it.copy(selectedStatus = status, error = null) }
        _uiState.value.selectedPropertyId?.let { propertyId ->
            loadReservations(propertyId, status)
        }
    }

    private fun loadReservations(
        propertyId: String,
        status: HotelReservationStatusFilter,
        initial: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = initial,
                    isRefreshing = !initial,
                    error = null,
                )
            }
            repository.listReservations(propertyId, status)
                .onSuccess { reservations ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            reservations = reservations,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            reservations = emptyList(),
                            error = error.message?.let(ErrorMessage::Plain)
                                ?: ErrorMessage.Resource(R.string.hotel_reservations_error_load_list),
                        )
                    }
                }
        }
    }

    class Factory(
        private val repository: HotelReservationsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HotelReservationsListViewModel(repository) as T
    }
}
