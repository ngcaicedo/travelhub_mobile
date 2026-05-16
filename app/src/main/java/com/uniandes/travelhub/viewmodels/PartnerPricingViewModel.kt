package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingWritePayload
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingLockedException
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.utils.JwtUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val HIGH_IMPACT_PCT_THRESHOLD = 30.0

sealed interface PartnerPricingUiState {
    data object Loading : PartnerPricingUiState
    data class Empty(val reason: ErrorMessage) : PartnerPricingUiState
    data class Ready(
        val properties: List<Property>,
        val selectedPropertyId: String?,
        val rules: List<SeasonalPricingResponse>,
        val isSubmitting: Boolean = false,
    ) : PartnerPricingUiState
    data class Error(val message: ErrorMessage) : PartnerPricingUiState
}

sealed interface PartnerPricingEvent {
    data class RequiresHighImpactConfirmation(val request: PendingDiscount) : PartnerPricingEvent
    data class SubmitError(val message: ErrorMessage) : PartnerPricingEvent
    data class SubmitSuccess(val rule: SeasonalPricingResponse) : PartnerPricingEvent
}

/** A request the user has built but the VM has paused awaiting user confirmation. */
data class PendingDiscount(
    val propertyId: String,
    val payload: SeasonalPricingWritePayload,
    val percent: Double,
)

class PartnerPricingViewModel(
    private val propertiesRepository: PropertiesRepository,
    private val seasonalPricingRepository: SeasonalPricingRepository,
    private val tokenStore: AuthTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartnerPricingUiState>(PartnerPricingUiState.Loading)
    val uiState: StateFlow<PartnerPricingUiState> = _uiState.asStateFlow()

    private val _events = Channel<PartnerPricingEvent>(Channel.BUFFERED)
    val events: Flow<PartnerPricingEvent> = _events.receiveAsFlow()

    init {
        loadProperties()
    }

    fun loadProperties() {
        viewModelScope.launch {
            _uiState.value = PartnerPricingUiState.Loading
            val token = tokenStore.tokenFlow.first()
            val ownerId = token?.let { JwtUtils.extractSubject(it) }
            if (ownerId.isNullOrBlank()) {
                _uiState.value = PartnerPricingUiState.Error(
                    ErrorMessage.Resource(R.string.partner_pricing_error_no_admin)
                )
                return@launch
            }
            propertiesRepository.getPropertiesByOwner(ownerId)
                .onSuccess { properties ->
                    if (properties.isEmpty()) {
                        _uiState.value = PartnerPricingUiState.Empty(
                            ErrorMessage.Resource(R.string.partner_pricing_empty_no_properties)
                        )
                    } else {
                        val selected = properties.first().id
                        _uiState.value = PartnerPricingUiState.Ready(
                            properties = properties,
                            selectedPropertyId = selected,
                            rules = emptyList(),
                        )
                        loadRules(selected)
                    }
                }
                .onFailure { error ->
                    _uiState.value = PartnerPricingUiState.Error(
                        error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.partner_pricing_error_load_properties)
                    )
                }
        }
    }

    fun selectProperty(propertyId: String) {
        val current = _uiState.value as? PartnerPricingUiState.Ready ?: return
        if (current.selectedPropertyId == propertyId) return
        _uiState.value = current.copy(selectedPropertyId = propertyId, rules = emptyList())
        loadRules(propertyId)
    }

    fun loadRules(propertyId: String) {
        viewModelScope.launch {
            seasonalPricingRepository.list(propertyId)
                .onSuccess { rules ->
                    val current = _uiState.value as? PartnerPricingUiState.Ready ?: return@onSuccess
                    if (current.selectedPropertyId == propertyId) {
                        _uiState.value = current.copy(rules = rules)
                    }
                }
                .onFailure { /* keep existing state; rules surface error in dedicated screen */ }
        }
    }

    /**
     * Build the pending request and either submit immediately or emit a
     * [PartnerPricingEvent.RequiresHighImpactConfirmation] when the change
     * exceeds [HIGH_IMPACT_PCT_THRESHOLD] from the property's base rate.
     */
    fun submitNewDiscount(
        percent: Double,
        seasonStart: String,
        seasonEnd: String,
    ) {
        val current = _uiState.value as? PartnerPricingUiState.Ready ?: return
        val propertyId = current.selectedPropertyId ?: return
        val property = current.properties.firstOrNull { it.id == propertyId } ?: return

        if (percent <= 0.0 || percent >= 100.0) {
            viewModelScope.launch {
                _events.send(
                    PartnerPricingEvent.SubmitError(
                        ErrorMessage.Resource(R.string.partner_pricing_validation_invalid_percent)
                    )
                )
            }
            return
        }
        if (seasonStart.isBlank() || seasonEnd.isBlank() || seasonEnd <= seasonStart) {
            viewModelScope.launch {
                _events.send(
                    PartnerPricingEvent.SubmitError(
                        ErrorMessage.Resource(R.string.partner_pricing_validation_invalid_date_range)
                    )
                )
            }
            return
        }

        val finalPrice = roundTo2(property.pricePerNight * (1.0 - percent / 100.0))
        val payload = SeasonalPricingWritePayload(
            seasonStart = seasonStart,
            seasonEnd = seasonEnd,
            pricePerNight = finalPrice,
            currency = property.currency,
            taxRate = property.taxRate,
            cleaningFee = property.cleaningFee,
        )
        val pending = PendingDiscount(propertyId, payload, percent)

        if (percent >= HIGH_IMPACT_PCT_THRESHOLD) {
            viewModelScope.launch {
                _events.send(PartnerPricingEvent.RequiresHighImpactConfirmation(pending))
            }
            return
        }
        confirmAndSubmit(pending)
    }

    /** Called by the UI when the user accepts the high-impact confirmation dialog. */
    fun confirmAndSubmit(pending: PendingDiscount) {
        val current = _uiState.value as? PartnerPricingUiState.Ready ?: return
        _uiState.value = current.copy(isSubmitting = true)
        viewModelScope.launch {
            seasonalPricingRepository.create(pending.propertyId, pending.payload)
                .onSuccess { rule ->
                    val now = _uiState.value as? PartnerPricingUiState.Ready
                    if (now != null && now.selectedPropertyId == pending.propertyId) {
                        _uiState.value = now.copy(
                            isSubmitting = false,
                            rules = listOf(rule) + now.rules,
                        )
                    }
                    _events.send(PartnerPricingEvent.SubmitSuccess(rule))
                }
                .onFailure { error ->
                    val now = _uiState.value as? PartnerPricingUiState.Ready
                    if (now != null) _uiState.value = now.copy(isSubmitting = false)
                    val msg = when (error) {
                        is SeasonalPricingLockedException -> ErrorMessage.Resource(
                            R.string.partner_pricing_error_locked
                        )
                        else -> error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.partner_pricing_error_submit)
                    }
                    _events.send(PartnerPricingEvent.SubmitError(msg))
                }
        }
    }

    private fun roundTo2(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }

    class Factory(
        private val propertiesRepository: PropertiesRepository,
        private val seasonalPricingRepository: SeasonalPricingRepository,
        private val tokenStore: AuthTokenStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PartnerPricingViewModel(
                propertiesRepository,
                seasonalPricingRepository,
                tokenStore,
            ) as T
        }
    }
}
