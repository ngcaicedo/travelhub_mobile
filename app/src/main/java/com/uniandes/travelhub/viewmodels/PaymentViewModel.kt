package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.payments.PaymentsConfig
import com.uniandes.travelhub.repositories.PaymentsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface PaymentUiState {
    data object LoadingConfig : PaymentUiState
    data class Ready(val config: PaymentsConfig) : PaymentUiState
    data object Processing : PaymentUiState
    data class Succeeded(val confirmation: PaymentConfirmationSummary) : PaymentUiState
    data class Failed(val message: ErrorMessage) : PaymentUiState
}

sealed interface PaymentEvent {
    data class NavigateToConfirmation(val confirmation: PaymentConfirmationSummary) : PaymentEvent
}

class PaymentViewModel(
    private val reservationId: String,
    private val amountInCents: Long,
    private val currency: String,
    private val repository: PaymentsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.LoadingConfig)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _events = Channel<PaymentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.LoadingConfig
            repository.getConfig()
                .onSuccess { _uiState.value = PaymentUiState.Ready(it) }
                .onFailure { error ->
                    _uiState.value = PaymentUiState.Failed(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.payment_error_config)
                    )
                }
        }
    }

    fun pay(paymentMethodToken: String) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Processing
            // Use the reservation id as idempotency anchor — same reservation cannot be charged twice.
            val idempotencyKey = "$reservationId:$paymentMethodToken"
            repository.charge(
                reservationId = reservationId,
                amountInCents = amountInCents,
                currency = currency,
                paymentMethodToken = paymentMethodToken,
                idempotencyKey = idempotencyKey,
            ).onSuccess { charge ->
                if (charge.status.equals("confirmed", ignoreCase = true) ||
                    charge.status.equals("succeeded", ignoreCase = true)
                ) {
                    fetchConfirmation(charge.paymentId)
                } else {
                    _uiState.value = PaymentUiState.Failed(
                        charge.failureReason?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.payment_error_declined)
                    )
                }
            }.onFailure { error ->
                _uiState.value = PaymentUiState.Failed(
                    error.message?.let { ErrorMessage.Plain(it) }
                        ?: ErrorMessage.Resource(R.string.payment_error_generic)
                )
            }
        }
    }

    private suspend fun fetchConfirmation(paymentId: String) {
        repository.getConfirmation(paymentId)
            .onSuccess { confirmation ->
                _uiState.value = PaymentUiState.Succeeded(confirmation)
                _events.send(PaymentEvent.NavigateToConfirmation(confirmation))
            }
            .onFailure { error ->
                _uiState.value = PaymentUiState.Failed(
                    error.message?.let { ErrorMessage.Plain(it) }
                        ?: ErrorMessage.Resource(R.string.payment_error_confirmation)
                )
            }
    }

    class Factory(
        private val reservationId: String,
        private val amountInCents: Long,
        private val currency: String,
        private val repository: PaymentsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaymentViewModel(reservationId, amountInCents, currency, repository) as T
    }
}
