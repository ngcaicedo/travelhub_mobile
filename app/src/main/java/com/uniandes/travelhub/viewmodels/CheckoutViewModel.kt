package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class CheckoutFormState(
    val checkIn: String = "",
    val checkOut: String = "",
    val guests: Int = 1,
    val currency: String = DEFAULT_CURRENCY,
    val checkInError: ErrorMessage? = null,
    val checkOutError: ErrorMessage? = null,
    val guestsError: ErrorMessage? = null,
) {
    val isValid: Boolean
        get() = checkInError == null && checkOutError == null && guestsError == null

    companion object {
        const val DEFAULT_CURRENCY = "COP"
    }
}

/**
 * Live, client-side estimate of the reservation cost. Mirrors the formula
 * used by the reservations service so the UI matches what the backend will
 * compute (accommodation + taxes + cleaning fee).
 */
data class PriceSummary(
    val nights: Int,
    val guests: Int,
    val nightlyRate: Double,
    val accommodation: Double,
    val serviceFee: Double,
    val taxes: Double,
    val cleaningFee: Double,
    val total: Double,
    val currency: String,
) {
    companion object {
        const val SERVICE_FEE_RATE: Double = 0.08
    }
}

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Submitting : CheckoutUiState
    data class Error(val message: ErrorMessage) : CheckoutUiState
    data class Success(val reservation: ReservationResponse) : CheckoutUiState
}

sealed interface CheckoutEvent {
    data class NavigateToPayment(val reservation: ReservationResponse) : CheckoutEvent
}

class CheckoutViewModel(
    private val propertyId: String,
    private val reservationsRepository: ReservationsRepository,
    private val propertiesRepository: PropertiesRepository,
    initialCheckIn: String? = null,
    initialCheckOut: String? = null,
    initialGuests: Int? = null,
    initialCurrency: String? = null,
) : ViewModel() {

    private val _form = MutableStateFlow(
        CheckoutFormState(
            checkIn = initialCheckIn.orEmpty(),
            checkOut = initialCheckOut.orEmpty(),
            guests = initialGuests?.coerceAtLeast(1) ?: 1,
            currency = initialCurrency ?: CheckoutFormState.DEFAULT_CURRENCY,
        )
    )
    val form: StateFlow<CheckoutFormState> = _form.asStateFlow()

    private val _property = MutableStateFlow<Property?>(null)
    val property: StateFlow<Property?> = _property.asStateFlow()

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProperty()
    }

    private fun loadProperty() {
        viewModelScope.launch {
            val current = _form.value
            val checkIn = current.checkIn.takeIf { it.isNotBlank() }
            val checkOut = current.checkOut.takeIf { it.isNotBlank() }
            // Cache holds the canonical (no-range) property; only show it when
            // the user hasn't picked dates yet so we don't flash the base price
            // before the seasonal-aware fetch resolves.
            if (checkIn == null || checkOut == null) {
                propertiesRepository.getCachedProperty(propertyId)?.let {
                    _property.value = it
                    _form.update { f -> f.copy(currency = it.currency.ifBlank { f.currency }) }
                }
            }
            propertiesRepository.getPropertyDetail(propertyId, checkIn, checkOut).onSuccess { fresh ->
                _property.value = fresh
                _form.update { f -> f.copy(currency = fresh.currency.ifBlank { f.currency }) }
            }
        }
    }

    fun onCheckInChange(value: String) {
        _form.update { it.copy(checkIn = value, checkInError = null) }
        // Re-fetch the property with the new range so any seasonal override
        // applicable to it is reflected in the price breakdown.
        if (value.isNotBlank() && _form.value.checkOut.isNotBlank()) loadProperty()
    }

    fun onCheckOutChange(value: String) {
        _form.update { it.copy(checkOut = value, checkOutError = null) }
        if (value.isNotBlank() && _form.value.checkIn.isNotBlank()) loadProperty()
    }
    fun onGuestsChange(value: Int) = _form.update { it.copy(guests = value.coerceAtLeast(1), guestsError = null) }

    /**
     * Canonical breakdown — mirrors `services/reservations/.../create_reservation.py`
     * and `travelhub_frontend/app/utils/pricing.ts`. Keep these three in sync.
     *
     *   accommodation = price_per_night × nights × guests
     *   service_fee   = accommodation × SERVICE_FEE_RATE
     *   subtotal      = accommodation + cleaning_fee + service_fee
     *   taxes         = subtotal × tax_rate
     *   total         = subtotal + taxes
     */
    fun computeSummary(): PriceSummary? {
        val p = _property.value ?: return null
        val f = _form.value
        val nights = nightsBetween(f.checkIn, f.checkOut) ?: return null
        if (nights <= 0) return null
        val guests = f.guests.coerceAtLeast(1)
        val nightlyRate = p.pricePerNight
        val accommodation = nightlyRate * nights * guests
        val cleaning = p.cleaningFee
        val serviceFee = accommodation * PriceSummary.SERVICE_FEE_RATE
        val subtotal = accommodation + cleaning + serviceFee
        val taxes = subtotal * p.taxRate
        return PriceSummary(
            nights = nights,
            guests = guests,
            nightlyRate = nightlyRate,
            accommodation = accommodation,
            serviceFee = serviceFee,
            taxes = taxes,
            cleaningFee = cleaning,
            total = subtotal + taxes,
            currency = p.currency.ifBlank { f.currency },
        )
    }

    fun submit() {
        val current = _form.value
        val validated = validate(current)
        if (!validated.isValid) {
            _form.value = validated
            return
        }
        _form.value = validated

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Submitting
            reservationsRepository.create(
                propertyId = propertyId,
                checkIn = current.checkIn,
                checkOut = current.checkOut,
                guests = current.guests,
                currency = current.currency,
            ).onSuccess { reservation ->
                _uiState.value = CheckoutUiState.Success(reservation)
                _events.send(CheckoutEvent.NavigateToPayment(reservation))
            }.onFailure { error ->
                _uiState.value = CheckoutUiState.Error(
                    error.message?.let { ErrorMessage.Plain(it) }
                        ?: ErrorMessage.Resource(R.string.checkout_error_generic)
                )
            }
        }
    }

    private fun validate(form: CheckoutFormState): CheckoutFormState = form.copy(
        checkInError = if (form.checkIn.isBlank()) ErrorMessage.Resource(R.string.checkout_error_check_in_required) else null,
        checkOutError = when {
            form.checkOut.isBlank() -> ErrorMessage.Resource(R.string.checkout_error_check_out_required)
            form.checkIn.isNotBlank() && form.checkOut <= form.checkIn ->
                ErrorMessage.Resource(R.string.checkout_error_check_out_after_check_in)
            else -> null
        },
        guestsError = if (form.guests < 1) ErrorMessage.Resource(R.string.checkout_error_guests_min) else null,
    )

    private fun nightsBetween(checkIn: String, checkOut: String): Int? = runCatching {
        val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
        val from = LocalDate.parse(checkIn, isoDate)
        val to = LocalDate.parse(checkOut, isoDate)
        ChronoUnit.DAYS.between(from, to).toInt()
    }.getOrNull()

    class Factory(
        private val propertyId: String,
        private val reservationsRepository: ReservationsRepository,
        private val propertiesRepository: PropertiesRepository,
        private val initialCheckIn: String? = null,
        private val initialCheckOut: String? = null,
        private val initialGuests: Int? = null,
        private val initialCurrency: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CheckoutViewModel(
            propertyId,
            reservationsRepository,
            propertiesRepository,
            initialCheckIn,
            initialCheckOut,
            initialGuests,
            initialCurrency,
        ) as T
    }
}
