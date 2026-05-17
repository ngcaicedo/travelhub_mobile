package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingPatchPayload
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingLockedException
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val HIGH_IMPACT_PCT_THRESHOLD = 30.0

sealed interface EditRuleUiState {
    data object Loading : EditRuleUiState
    data class Ready(
        val property: Property,
        val rule: SeasonalPricingResponse,
        val basePrice: Double,
        val canEdit: Boolean,
        val isSubmitting: Boolean = false,
    ) : EditRuleUiState
    data class Error(val message: ErrorMessage) : EditRuleUiState
}

sealed interface EditRuleEvent {
    data class RequiresHighImpactConfirmation(val pending: PendingPatch) : EditRuleEvent
    data class SubmitError(val message: ErrorMessage) : EditRuleEvent
    data class SubmitSuccess(val rule: SeasonalPricingResponse) : EditRuleEvent
}

data class PendingPatch(
    val propertyId: String,
    val ruleId: String,
    val patch: SeasonalPricingPatchPayload,
)

class EditRuleViewModel(
    private val propertiesRepository: PropertiesRepository,
    private val seasonalPricingRepository: SeasonalPricingRepository,
    private val propertyId: String,
    private val ruleId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditRuleUiState>(EditRuleUiState.Loading)
    val uiState: StateFlow<EditRuleUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditRuleEvent>(Channel.BUFFERED)
    val events: Flow<EditRuleEvent> = _events.receiveAsFlow()

    init {
        loadRule()
    }

    fun loadRule() {
        viewModelScope.launch {
            _uiState.value = EditRuleUiState.Loading
            propertiesRepository.getPropertyDetail(propertyId)
                .onSuccess { property ->
                    seasonalPricingRepository.list(propertyId)
                        .onSuccess { rules ->
                            val rule = rules.firstOrNull { it.id == ruleId }
                            if (rule == null) {
                                _uiState.value = EditRuleUiState.Error(
                                    ErrorMessage.Resource(R.string.partner_pricing_error_rule_not_found)
                                )
                            } else {
                                _uiState.value = EditRuleUiState.Ready(
                                    property = property,
                                    rule = rule,
                                    basePrice = property.pricePerNight,
                                    canEdit = !rule.integrityLocked,
                                )
                            }
                        }
                        .onFailure { error -> emitLoadError(error) }
                }
                .onFailure { error -> emitLoadError(error) }
        }
    }

    private fun emitLoadError(error: Throwable) {
        _uiState.value = EditRuleUiState.Error(
            error.message?.let(ErrorMessage::Plain)
                ?: ErrorMessage.Resource(R.string.partner_pricing_error_load_rules)
        )
    }

    /**
     * Build a partial patch from optional new values; only fields that actually
     * differ from the persisted rule are sent. The server re-signs over the
     * resulting merged state, so we don't need to recompute the signature here.
     */
    fun submitUpdate(
        newPercent: Double?,
        newSeasonStart: String?,
        newSeasonEnd: String?,
    ) {
        val current = _uiState.value as? EditRuleUiState.Ready ?: return
        if (!current.canEdit) {
            viewModelScope.launch {
                _events.send(
                    EditRuleEvent.SubmitError(
                        ErrorMessage.Resource(R.string.partner_pricing_error_locked)
                    )
                )
            }
            return
        }

        val basePrice = current.basePrice
        val newPrice = if (newPercent != null) {
            if (newPercent <= 0.0 || newPercent >= 100.0) {
                viewModelScope.launch {
                    _events.send(
                        EditRuleEvent.SubmitError(
                            ErrorMessage.Resource(R.string.partner_pricing_validation_invalid_percent)
                        )
                    )
                }
                return
            }
            kotlin.math.round(basePrice * (1.0 - newPercent / 100.0) * 100.0) / 100.0
        } else null

        val seasonStart = newSeasonStart?.takeIf { it != current.rule.seasonStart }
        val seasonEnd = newSeasonEnd?.takeIf { it != current.rule.seasonEnd }
        val priceField = newPrice?.takeIf { it != current.rule.pricePerNight }

        if (seasonStart == null && seasonEnd == null && priceField == null) {
            viewModelScope.launch {
                _events.send(
                    EditRuleEvent.SubmitError(
                        ErrorMessage.Resource(R.string.partner_pricing_validation_no_changes)
                    )
                )
            }
            return
        }
        if (seasonStart != null && seasonEnd != null && seasonEnd <= seasonStart) {
            viewModelScope.launch {
                _events.send(
                    EditRuleEvent.SubmitError(
                        ErrorMessage.Resource(R.string.partner_pricing_validation_invalid_date_range)
                    )
                )
            }
            return
        }

        val patch = SeasonalPricingPatchPayload(
            seasonStart = seasonStart,
            seasonEnd = seasonEnd,
            pricePerNight = priceField,
        )
        val pending = PendingPatch(propertyId, ruleId, patch)

        val effectivePct = newPercent
            ?: ((1.0 - current.rule.pricePerNight / basePrice) * 100.0)
        if (newPrice != null && effectivePct >= HIGH_IMPACT_PCT_THRESHOLD) {
            viewModelScope.launch {
                _events.send(EditRuleEvent.RequiresHighImpactConfirmation(pending))
            }
            return
        }
        confirmAndSubmit(pending)
    }

    fun confirmAndSubmit(pending: PendingPatch) {
        val current = _uiState.value as? EditRuleUiState.Ready ?: return
        _uiState.value = current.copy(isSubmitting = true)
        viewModelScope.launch {
            seasonalPricingRepository.update(pending.propertyId, pending.ruleId, pending.patch)
                .onSuccess { rule ->
                    val now = _uiState.value as? EditRuleUiState.Ready ?: return@onSuccess
                    _uiState.value = now.copy(rule = rule, isSubmitting = false)
                    _events.send(EditRuleEvent.SubmitSuccess(rule))
                }
                .onFailure { error ->
                    val now = _uiState.value as? EditRuleUiState.Ready
                    if (now != null) _uiState.value = now.copy(isSubmitting = false)
                    val msg = when (error) {
                        is SeasonalPricingLockedException -> ErrorMessage.Resource(
                            R.string.partner_pricing_error_locked
                        )
                        else -> error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.partner_pricing_error_submit)
                    }
                    _events.send(EditRuleEvent.SubmitError(msg))
                }
        }
    }

    class Factory(
        private val propertiesRepository: PropertiesRepository,
        private val seasonalPricingRepository: SeasonalPricingRepository,
        private val propertyId: String,
        private val ruleId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditRuleViewModel(
                propertiesRepository,
                seasonalPricingRepository,
                propertyId,
                ruleId,
            ) as T
        }
    }
}
