package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.utils.AuthValidators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface VerifyOtpUiState {
    data object Idle : VerifyOtpUiState
    data object Loading : VerifyOtpUiState
    data class Error(val message: ErrorMessage) : VerifyOtpUiState
}

sealed interface VerifyOtpEvent {
    data class NavigateToHome(val role: UserRole) : VerifyOtpEvent
}

class VerifyOtpViewModel(
    private val repository: AuthRepository,
    private val email: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<VerifyOtpUiState>(VerifyOtpUiState.Idle)
    val uiState: StateFlow<VerifyOtpUiState> = _uiState.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _events = Channel<VerifyOtpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onOtpChange(value: String) {
        _otpCode.value = value.filter { it.isDigit() }.take(6)
        if (_uiState.value is VerifyOtpUiState.Error) _uiState.value = VerifyOtpUiState.Idle
    }

    fun onSubmit() {
        val code = _otpCode.value
        if (!AuthValidators.isValidOtp(code)) {
            _uiState.value = VerifyOtpUiState.Error(
                ErrorMessage.Resource(R.string.auth_verify_otp_invalid)
            )
            return
        }

        _uiState.value = VerifyOtpUiState.Loading
        viewModelScope.launch {
            repository.verifyOtp(email, code).fold(
                onSuccess = { role ->
                    _uiState.value = VerifyOtpUiState.Idle
                    _events.send(VerifyOtpEvent.NavigateToHome(role))
                },
                onFailure = { throwable ->
                    _uiState.value = VerifyOtpUiState.Error(
                        throwable.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.auth_verify_otp_default_error)
                    )
                }
            )
        }
    }

    class Factory(
        private val repository: AuthRepository,
        private val email: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VerifyOtpViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return VerifyOtpViewModel(repository, email) as T
        }
    }
}
