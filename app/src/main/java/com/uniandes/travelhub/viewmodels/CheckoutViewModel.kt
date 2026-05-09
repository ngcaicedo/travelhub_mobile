package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.repositories.SearchRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

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

sealed interface CheckoutPricingState {
    data object Idle : CheckoutPricingState
    data object Loading : CheckoutPricingState
    data class Available(val nightlyRate: Double, val currency: String) : CheckoutPricingState
    data class Unavailable(val message: ErrorMessage) : CheckoutPricingState
    data class Error(val message: ErrorMessage) : CheckoutPricingState
}

class CheckoutViewModel(
    private val propertyId: String,
    private val reservationsRepository: ReservationsRepository,
    private val propertiesRepository: PropertiesRepository,
    private val searchRepository: SearchRepository,
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

    private val _pricingState = MutableStateFlow<CheckoutPricingState>(CheckoutPricingState.Idle)
    val pricingState: StateFlow<CheckoutPricingState> = _pricingState.asStateFlow()

    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProperty()
        refreshEffectivePricing()
    }

    private fun loadProperty() {
        viewModelScope.launch {
            // Prefer cache for instant render; fall back to network.
            propertiesRepository.getCachedProperty(propertyId)?.let {
                _property.value = it
                _form.update { f -> f.copy(currency = it.currency.ifBlank { f.currency }) }
            }
            propertiesRepository.getPropertyDetail(propertyId).onSuccess { fresh ->
                _property.value = fresh
                _form.update { f -> f.copy(currency = fresh.currency.ifBlank { f.currency }) }
                refreshEffectivePricing()
            }
        }
    }

    fun onCheckInChange(value: String) {
        _form.update { it.copy(checkIn = value, checkInError = null) }
        refreshEffectivePricing()
    }

    fun onCheckOutChange(value: String) {
        _form.update { it.copy(checkOut = value, checkOutError = null) }
        refreshEffectivePricing()
    }

    fun onGuestsChange(value: Int) {
        _form.update { it.copy(guests = value.coerceAtLeast(1), guestsError = null) }
        refreshEffectivePricing()
    }

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
        if (pricingState.value is CheckoutPricingState.Unavailable) return null
        val guests = f.guests.coerceAtLeast(1)
        val activeQuote = pricingState.value as? CheckoutPricingState.Available
        val nightlyRate = activeQuote?.nightlyRate ?: p.pricePerNight
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
            currency = activeQuote?.currency ?: p.currency.ifBlank { f.currency },
        )
    }

    private fun refreshEffectivePricing() {
        val current = _form.value
        val datesAreValid = current.checkIn.isNotBlank() &&
            current.checkOut.isNotBlank() &&
            nightsBetween(current.checkIn, current.checkOut)?.let { it > 0 } == true

        if (!datesAreValid) {
            _pricingState.value = CheckoutPricingState.Idle
            return
        }

        viewModelScope.launch {
            _pricingState.value = CheckoutPricingState.Loading
            searchRepository.checkAvailability(
                propertyId = propertyId,
                checkIn = current.checkIn,
                checkOut = current.checkOut,
                guests = current.guests.coerceAtLeast(1),
            ).onSuccess { availability ->
                _pricingState.value = if (availability.available && availability.priceFrom != null) {
                    CheckoutPricingState.Available(
                        nightlyRate = availability.priceFrom,
                        currency = availability.currency ?: current.currency,
                    )
                } else if (!availability.available) {
                    CheckoutPricingState.Unavailable(buildUnavailableMessage(current.checkIn, current.checkOut))
                } else {
                    CheckoutPricingState.Idle
                }
            }.onFailure {
                _pricingState.value = CheckoutPricingState.Error(
                    ErrorMessage.Resource(R.string.checkout_price_verification_error)
                )
            }
        }
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
                    mapSubmitError(
                        detail = error.message,
                        checkIn = current.checkIn,
                        checkOut = current.checkOut,
                    )
                )
            }
        }
    }

    private fun mapSubmitError(
        detail: String?,
        checkIn: String,
        checkOut: String,
    ): ErrorMessage {
        val normalizedDetail = detail?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return if ("not available" in normalizedDetail) {
            buildUnavailableMessage(checkIn, checkOut)
        } else {
            detail?.takeIf { it.isNotBlank() }?.let(ErrorMessage::Plain)
                ?: ErrorMessage.Resource(R.string.checkout_error_generic)
        }
    }

    private fun buildUnavailableMessage(checkIn: String, checkOut: String): ErrorMessage =
        ErrorMessage.Resource(
            id = R.string.checkout_selected_dates_unavailable,
            args = listOf(
                formatDisplayDate(checkIn),
                formatDisplayDate(checkOut),
            ),
        )

    private fun formatDisplayDate(rawDate: String): String = runCatching {
        LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE)
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
            )
    }.getOrDefault(rawDate)

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
        private val searchRepository: SearchRepository,
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
            searchRepository,
            initialCheckIn,
            initialCheckOut,
            initialGuests,
            initialCurrency,
        ) as T
    }
}
