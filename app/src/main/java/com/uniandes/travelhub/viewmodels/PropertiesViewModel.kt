package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.repositories.PropertiesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PropertyListUiState {
    object Loading : PropertyListUiState()
    data class Success(val properties: List<Property>) : PropertyListUiState()
    data class Error(val message: String) : PropertyListUiState()
}

class PropertiesViewModel(private val repository: PropertiesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<PropertyListUiState>(PropertyListUiState.Loading)
    val uiState: StateFlow<PropertyListUiState> = _uiState.asStateFlow()

    init {
        loadProperties()
    }

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = PropertyListUiState.Loading
            repository.getProperties()
                .onSuccess { properties ->
                    _uiState.value = PropertyListUiState.Success(properties)
                }
                .onFailure { error ->
                    _uiState.value = PropertyListUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    class Factory(private val repository: PropertiesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PropertiesViewModel(repository) as T
        }
    }
}
