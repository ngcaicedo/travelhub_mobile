package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.utils.AuthValidators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: ErrorMessage) : LoginUiState
}

sealed interface LoginEvent {
    data class NavigateToOtp(val email: String) : LoginEvent
}

class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _email.value = value
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }

    fun onSubmit() {
        val emailValue = _email.value.trim()
        val passwordValue = _password.value

        if (!AuthValidators.isValidEmail(emailValue)) {
            _uiState.value = LoginUiState.Error(
                ErrorMessage.Resource(R.string.auth_login_email_invalid)
            )
            return
        }
        if (passwordValue.isBlank()) {
            _uiState.value = LoginUiState.Error(
                ErrorMessage.Resource(R.string.auth_login_password_required)
            )
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.login(emailValue, passwordValue).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Idle
                    _events.send(LoginEvent.NavigateToOtp(emailValue))
                },
                onFailure = { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.auth_login_default_error)
                    )
                }
            )
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return LoginViewModel(repository) as T
        }
    }
}
