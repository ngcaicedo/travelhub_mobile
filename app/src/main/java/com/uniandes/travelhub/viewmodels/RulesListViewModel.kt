package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.utils.JwtUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class IntegrityState { Normal, Locked, Compromised }

data class RuleRow(
    val rule: SeasonalPricingResponse,
    val property: Property,
    val derivedPercent: Int,
    val state: IntegrityState,
)

sealed interface RulesListUiState {
    data object Loading : RulesListUiState
    data class Ready(val rows: List<RuleRow>) : RulesListUiState
    data object Empty : RulesListUiState
    data class Error(val message: ErrorMessage) : RulesListUiState
    data object NoProperties : RulesListUiState
}

class RulesListViewModel(
    private val propertiesRepository: PropertiesRepository,
    private val seasonalPricingRepository: SeasonalPricingRepository,
    private val tokenStore: AuthTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RulesListUiState>(RulesListUiState.Loading)
    val uiState: StateFlow<RulesListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = RulesListUiState.Loading
            val token = tokenStore.tokenFlow.first()
            val ownerId = token?.let { JwtUtils.extractSubject(it) }
            if (ownerId.isNullOrBlank()) {
                _uiState.value = RulesListUiState.Error(
                    ErrorMessage.Resource(R.string.partner_pricing_error_no_admin)
                )
                return@launch
            }

            propertiesRepository.getPropertiesByOwner(ownerId)
                .onFailure { error ->
                    _uiState.value = RulesListUiState.Error(
                        error.message?.let(ErrorMessage::Plain)
                            ?: ErrorMessage.Resource(R.string.partner_pricing_error_load_properties)
                    )
                }
                .onSuccess { properties ->
                    if (properties.isEmpty()) {
                        _uiState.value = RulesListUiState.NoProperties
                        return@onSuccess
                    }

                    // Fetch seasonal pricing for every property in parallel and
                    // flatten results into a single list. Failures per-property
                    // are silently treated as "no rules for that property" so a
                    // single bad endpoint doesn't blank the whole screen — the
                    // partner can still see the rules of the other properties.
                    val rows: List<RuleRow> = coroutineScope {
                        properties.map { property ->
                            async {
                                seasonalPricingRepository.list(property.id)
                                    .getOrNull()
                                    .orEmpty()
                                    .map { rule -> toRow(rule, property) }
                            }
                        }.awaitAll().flatten()
                    }

                    _uiState.value = if (rows.isEmpty()) {
                        RulesListUiState.Empty
                    } else {
                        // Sort by season_start desc so the most recently active
                        // rules surface first, then by property name for stability.
                        val sorted = rows.sortedWith(
                            compareByDescending<RuleRow> { it.rule.seasonStart }
                                .thenBy { it.property.name }
                        )
                        RulesListUiState.Ready(rows = sorted)
                    }
                }
        }
    }

    private fun toRow(rule: SeasonalPricingResponse, property: Property): RuleRow {
        val state = when {
            rule.integrityLocked -> IntegrityState.Locked
            !rule.integrityValid -> IntegrityState.Compromised
            else -> IntegrityState.Normal
        }
        val derivedPercent =
            if (property.pricePerNight > 0.0) {
                kotlin.math.round((1.0 - rule.pricePerNight / property.pricePerNight) * 100.0).toInt()
            } else 0
        return RuleRow(rule, property, derivedPercent, state)
    }

    class Factory(
        private val propertiesRepository: PropertiesRepository,
        private val seasonalPricingRepository: SeasonalPricingRepository,
        private val tokenStore: AuthTokenStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RulesListViewModel(
                propertiesRepository,
                seasonalPricingRepository,
                tokenStore,
            ) as T
        }
    }
}
