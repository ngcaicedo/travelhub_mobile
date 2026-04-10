package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.RegisterRequest
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.utils.AuthValidators
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Error(val message: ErrorMessage) : RegisterUiState
}

sealed interface RegisterEvent {
    data object NavigateToLogin : RegisterEvent
}

data class RegisterFormState(
    val fullName: String = "",
    val hotelName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val agreedToTerms: Boolean = false,
    val role: UserRole = UserRole.TRAVELER
)

class RegisterViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(RegisterFormState())
    val form: StateFlow<RegisterFormState> = _form.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onRoleChange(role: UserRole) = update { it.copy(role = role) }
    fun onFullNameChange(value: String) = update { it.copy(fullName = value) }
    fun onHotelNameChange(value: String) = update { it.copy(hotelName = value) }
    fun onEmailChange(value: String) = update { it.copy(email = value) }
    fun onPhoneChange(value: String) = update { it.copy(phone = value) }
    fun onPasswordChange(value: String) = update { it.copy(password = value) }
    fun onTermsChange(value: Boolean) = update { it.copy(agreedToTerms = value) }

    private fun update(transform: (RegisterFormState) -> RegisterFormState) {
        _form.value = transform(_form.value)
        if (_uiState.value is RegisterUiState.Error) _uiState.value = RegisterUiState.Idle
    }

    fun onSubmit() {
        val current = _form.value

        validate(current)?.let { errorMessage ->
            _uiState.value = RegisterUiState.Error(errorMessage)
            return
        }

        val payload = RegisterRequest(
            email = current.email.trim(),
            phone = current.phone.trim(),
            password = current.password,
            fullName = current.fullName.trim(),
            hotelName = current.hotelName.trim().takeIf { current.role == UserRole.HOTEL_PARTNER && it.isNotEmpty() },
            role = current.role
        )

        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            repository.register(payload).fold(
                onSuccess = {
                    _uiState.value = RegisterUiState.Idle
                    _events.send(RegisterEvent.NavigateToLogin)
                },
                onFailure = { throwable ->
                    _uiState.value = RegisterUiState.Error(
                        throwable.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.auth_register_default_error)
                    )
                }
            )
        }
    }

    private fun validate(state: RegisterFormState): ErrorMessage? {
        if (state.fullName.isBlank()) {
            return ErrorMessage.Resource(R.string.auth_register_full_name_required)
        }
        if (state.role == UserRole.HOTEL_PARTNER && state.hotelName.isBlank()) {
            return ErrorMessage.Resource(R.string.auth_register_hotel_name_required)
        }
        if (!AuthValidators.isValidEmail(state.email)) {
            return ErrorMessage.Resource(R.string.auth_login_email_invalid)
        }
        if (!AuthValidators.isValidPhone(state.phone)) {
            return ErrorMessage.Resource(R.string.auth_register_phone_invalid)
        }
        if (!AuthValidators.isStrongEnough(state.password)) {
            return ErrorMessage.Resource(R.string.auth_register_password_weak)
        }
        if (!state.agreedToTerms) {
            return ErrorMessage.Resource(R.string.auth_register_terms_required)
        }
        return null
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                "Unknown ViewModel class: $modelClass"
            }
            return RegisterViewModel(repository) as T
        }
    }
}
