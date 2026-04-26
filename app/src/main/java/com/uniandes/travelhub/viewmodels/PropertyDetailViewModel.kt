package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.repositories.PropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the property detail screen.
 */
sealed interface PropertyDetailUiState {
    data object Idle : PropertyDetailUiState
    data object Loading : PropertyDetailUiState
    data class Success(
        val property: Property,
        val isRefreshing: Boolean = false,
        val isFromCache: Boolean = false
    ) : PropertyDetailUiState
    data class Error(val message: ErrorMessage) : PropertyDetailUiState
}

class PropertyDetailViewModel(
    private val repository: PropertiesRepository,
    private val propertyId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<PropertyDetailUiState>(PropertyDetailUiState.Loading)
    val uiState: StateFlow<PropertyDetailUiState> = _uiState.asStateFlow()

    init {
        loadPropertyDetail()
    }

    fun loadPropertyDetail() {
        viewModelScope.launch {
            val cached = repository.getCachedProperty(propertyId)
            _uiState.value = if (cached != null) {
                PropertyDetailUiState.Success(
                    property = cached,
                    isRefreshing = true,
                    isFromCache = true
                )
            } else {
                PropertyDetailUiState.Loading
            }
            repository.getPropertyDetail(propertyId)
                .onSuccess { property ->
                    _uiState.value = PropertyDetailUiState.Success(property = property)
                }
                .onFailure { error ->
                    if (cached == null) {
                        _uiState.value = PropertyDetailUiState.Error(
                            error.message?.let { ErrorMessage.Plain(it) }
                                ?: ErrorMessage.Resource(R.string.property_detail_load_error)
                        )
                    } else {
                        _uiState.value = PropertyDetailUiState.Success(
                            property = cached,
                            isRefreshing = false,
                            isFromCache = true
                        )
                    }
                }
        }
    }

    class Factory(
        private val repository: PropertiesRepository,
        private val propertyId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PropertyDetailViewModel(repository, propertyId) as T
        }
    }
}
