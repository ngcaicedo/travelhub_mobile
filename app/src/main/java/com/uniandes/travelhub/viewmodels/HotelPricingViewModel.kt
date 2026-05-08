package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.pricing.HotelDiscountType
import com.uniandes.travelhub.models.pricing.HotelPricingApplyRequest
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewRequest
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.repositories.HotelPricingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HotelPricingValidation(
    val targetError: ErrorMessage? = null,
    val startDateError: ErrorMessage? = null,
    val endDateError: ErrorMessage? = null,
    val changeError: ErrorMessage? = null,
    val basePriceError: ErrorMessage? = null,
    val discountValueError: ErrorMessage? = null,
) {
    val isValid: Boolean
        get() = targetError == null &&
            startDateError == null &&
            endDateError == null &&
            changeError == null &&
            basePriceError == null &&
            discountValueError == null
}

data class HotelPricingFormState(
    val selectedRatePlanId: String? = null,
    val proposedBasePrice: String = "",
    val ruleName: String = "",
    val discountType: HotelDiscountType? = HotelDiscountType.PERCENTAGE,
    val discountValue: String = "",
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val endDate: String = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
    val validation: HotelPricingValidation = HotelPricingValidation(),
)

data class HotelPricingUiState(
    val isLoading: Boolean = true,
    val isPreviewLoading: Boolean = false,
    val isApplying: Boolean = false,
    val isRefreshingHistory: Boolean = false,
    val targets: List<HotelPricingTargetOption> = emptyList(),
    val preview: HotelPricingPreviewResponse? = null,
    val history: List<HotelPricingHistoryItem> = emptyList(),
    val error: ErrorMessage? = null,
    val success: ErrorMessage? = null,
)

class HotelPricingViewModel(
    private val repository: HotelPricingRepository,
    private val deviceLabelProvider: () -> String = { "Android device" },
    private val devicePlatformProvider: () -> String = { "Android" },
) : ViewModel() {

    private val _form = MutableStateFlow(HotelPricingFormState())
    val form: StateFlow<HotelPricingFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow(HotelPricingUiState())
    val uiState: StateFlow<HotelPricingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val targetsResult = repository.listTargets()
            val historyResult = repository.listHistory()

            targetsResult.onSuccess { targets ->
                val selected = _form.value.selectedRatePlanId?.let { current ->
                    targets.firstOrNull { it.ratePlanId == current }
                } ?: targets.firstOrNull()

                _form.update {
                    it.copy(
                        selectedRatePlanId = selected?.ratePlanId,
                        proposedBasePrice = selected?.basePrice?.toPlainAmount() ?: it.proposedBasePrice,
                        validation = it.validation.copy(targetError = null),
                    )
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        targets = targets,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.hotel_pricing_error_load_targets),
                    )
                }
            }

            historyResult.onSuccess { history ->
                _uiState.update { it.copy(history = history) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        history = emptyList(),
                        error = error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.hotel_pricing_error_load_history),
                    )
                }
            }
        }
    }

    fun onTargetSelected(ratePlanId: String) {
        val target = _uiState.value.targets.firstOrNull { it.ratePlanId == ratePlanId }
        _form.update {
            it.copy(
                selectedRatePlanId = ratePlanId,
                proposedBasePrice = target?.basePrice?.toPlainAmount() ?: "",
                validation = it.validation.copy(targetError = null),
            )
        }
        clearPreview()
    }

    fun onBasePriceChange(value: String) {
        _form.update {
            it.copy(
                proposedBasePrice = value,
                validation = it.validation.copy(basePriceError = null, changeError = null),
            )
        }
        clearPreview()
    }

    fun onRuleNameChange(value: String) {
        _form.update { it.copy(ruleName = value) }
        clearPreview()
    }

    fun onDiscountTypeChange(value: HotelDiscountType?) {
        _form.update {
            it.copy(
                discountType = value,
                validation = it.validation.copy(discountValueError = null, changeError = null),
            )
        }
        clearPreview()
    }

    fun onDiscountValueChange(value: String) {
        _form.update {
            it.copy(
                discountValue = value,
                validation = it.validation.copy(discountValueError = null, changeError = null),
            )
        }
        clearPreview()
    }

    fun onStartDateChange(value: String) {
        _form.update {
            it.copy(
                startDate = value,
                validation = it.validation.copy(startDateError = null, endDateError = null),
            )
        }
        clearPreview()
    }

    fun onEndDateChange(value: String) {
        _form.update {
            it.copy(
                endDate = value,
                validation = it.validation.copy(endDateError = null),
            )
        }
        clearPreview()
    }

    fun preview() {
        val validated = validate(_form.value)
        _form.value = validated
        if (!validated.validation.isValid) return

        val payload = buildPreviewRequest(validated) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPreviewLoading = true, error = null, success = null) }
            repository.preview(payload)
                .onSuccess { preview ->
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            preview = preview,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            error = error.message?.let(ErrorMessage::Plain)
                                ?: ErrorMessage.Resource(R.string.hotel_pricing_error_preview),
                        )
                    }
                }
        }
    }

    fun apply(confirmationAcknowledged: Boolean) {
        val validated = validate(_form.value)
        _form.value = validated
        if (!validated.validation.isValid) return

        val target = selectedTarget() ?: return
        val payload = HotelPricingApplyRequest(
            propertyId = target.propertyId,
            ratePlanId = target.ratePlanId,
            startDate = validated.startDate,
            endDate = validated.endDate,
            proposedBasePrice = validated.proposedBasePrice.toDoubleOrNull(),
            discountType = validated.discountType?.takeIf { validated.discountValue.isNotBlank() }?.wire,
            discountValue = validated.discountValue.toDoubleOrNull(),
            ruleName = validated.ruleName.trim().ifBlank { null },
            confirmationAcknowledged = confirmationAcknowledged,
            deviceLabel = deviceLabelProvider(),
            devicePlatform = devicePlatformProvider(),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, error = null, success = null) }
            repository.apply(payload)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            preview = response.preview,
                            success = ErrorMessage.Resource(R.string.hotel_pricing_success_apply),
                        )
                    }
                    refreshHistoryAndTargets(selectRatePlanId = target.ratePlanId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            error = error.message?.let(ErrorMessage::Plain)
                                ?: ErrorMessage.Resource(R.string.hotel_pricing_error_apply),
                        )
                    }
                }
        }
    }

    fun revert(changeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingHistory = true, error = null, success = null) }
            repository.revert(changeId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRefreshingHistory = false,
                            success = ErrorMessage.Resource(R.string.hotel_pricing_success_revert),
                        )
                    }
                    refreshHistoryAndTargets(selectRatePlanId = _form.value.selectedRatePlanId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingHistory = false,
                            error = error.message?.let(ErrorMessage::Plain)
                                ?: ErrorMessage.Resource(R.string.hotel_pricing_error_revert),
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, success = null) }
    }

    fun selectedTarget(): HotelPricingTargetOption? =
        _uiState.value.targets.firstOrNull { it.ratePlanId == _form.value.selectedRatePlanId }

    private fun refreshHistoryAndTargets(selectRatePlanId: String?) {
        viewModelScope.launch {
            val targets = repository.listTargets().getOrNull().orEmpty()
            val history = repository.listHistory().getOrNull().orEmpty()
            val selected = targets.firstOrNull { it.ratePlanId == selectRatePlanId } ?: targets.firstOrNull()

            _form.update {
                it.copy(
                    selectedRatePlanId = selected?.ratePlanId,
                    proposedBasePrice = selected?.basePrice?.toPlainAmount() ?: it.proposedBasePrice,
                )
            }
            _uiState.update {
                it.copy(
                    targets = targets,
                    history = history,
                )
            }
        }
    }

    private fun clearPreview() {
        _uiState.update { it.copy(preview = null) }
    }

    private fun validate(form: HotelPricingFormState): HotelPricingFormState {
        val hasBasePrice = form.proposedBasePrice.isNotBlank()
        val hasDiscount = form.discountValue.isNotBlank()
        val start = runCatching { LocalDate.parse(form.startDate) }.getOrNull()
        val end = runCatching { LocalDate.parse(form.endDate) }.getOrNull()
        val basePriceValue = form.proposedBasePrice.toDoubleOrNull()
        val discountValueParsed = form.discountValue.toDoubleOrNull()

        val validation = HotelPricingValidation(
            targetError = if (form.selectedRatePlanId.isNullOrBlank()) {
                ErrorMessage.Resource(R.string.hotel_pricing_error_target_required)
            } else null,
            startDateError = if (start == null) {
                ErrorMessage.Resource(R.string.hotel_pricing_error_start_date_required)
            } else null,
            endDateError = when {
                end == null -> ErrorMessage.Resource(R.string.hotel_pricing_error_end_date_required)
                start != null && end.isBefore(start) -> ErrorMessage.Resource(R.string.hotel_pricing_error_end_date_after_start)
                else -> null
            },
            changeError = if (!hasBasePrice && !hasDiscount) {
                ErrorMessage.Resource(R.string.hotel_pricing_error_change_required)
            } else null,
            basePriceError = if (hasBasePrice && (basePriceValue == null || basePriceValue < 0.0)) {
                ErrorMessage.Resource(R.string.hotel_pricing_error_base_price_invalid)
            } else null,
            discountValueError = if (hasDiscount && (discountValueParsed == null || discountValueParsed < 0.0)) {
                ErrorMessage.Resource(R.string.hotel_pricing_error_discount_invalid)
            } else null,
        )
        return form.copy(validation = validation)
    }

    private fun buildPreviewRequest(form: HotelPricingFormState): HotelPricingPreviewRequest? {
        val target = selectedTarget() ?: return null
        val hasDiscount = form.discountValue.isNotBlank()
        return HotelPricingPreviewRequest(
            propertyId = target.propertyId,
            ratePlanId = target.ratePlanId,
            startDate = form.startDate,
            endDate = form.endDate,
            proposedBasePrice = form.proposedBasePrice.toDoubleOrNull(),
            discountType = form.discountType?.takeIf { hasDiscount }?.wire,
            discountValue = form.discountValue.toDoubleOrNull(),
            ruleName = form.ruleName.trim().ifBlank { null },
        )
    }

    class Factory(
        private val repository: HotelPricingRepository,
        private val deviceLabelProvider: () -> String = { "Android device" },
        private val devicePlatformProvider: () -> String = { "Android" },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HotelPricingViewModel(repository, deviceLabelProvider, devicePlatformProvider) as T
    }
}

private fun Double.toPlainAmount(): String = String.format(Locale.US, "%.2f", this)
