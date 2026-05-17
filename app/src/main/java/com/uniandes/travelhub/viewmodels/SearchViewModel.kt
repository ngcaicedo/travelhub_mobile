package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.SearchOrderBy
import com.uniandes.travelhub.models.search.SearchOrderDir
import com.uniandes.travelhub.models.search.SearchQuery
import com.uniandes.travelhub.models.search.SearchResponse
import com.uniandes.travelhub.repositories.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reactive form state. Held separately from [SearchResultsState] so the user
 * can edit filters while previous results stay visible in the panel.
 */
data class SearchFormState(
    val city: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val guests: Int = 1,
    val amenities: Set<String> = emptySet(),
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val orderBy: SearchOrderBy? = null,
    val orderDir: SearchOrderDir? = null,
    val validation: SearchFormValidation = SearchFormValidation(),
) {
    val isValid: Boolean
        get() = validation.cityError == null &&
                validation.checkInError == null &&
                validation.checkOutError == null &&
                validation.guestsError == null
}

data class SearchFormValidation(
    val cityError: ErrorMessage? = null,
    val checkInError: ErrorMessage? = null,
    val checkOutError: ErrorMessage? = null,
    val guestsError: ErrorMessage? = null,
)

sealed interface SearchResultsState {
    data object Idle : SearchResultsState
    data object Loading : SearchResultsState
    data class Success(val response: SearchResponse) : SearchResultsState
    data class Error(val message: ErrorMessage) : SearchResultsState
}

class SearchViewModel(
    private val repository: SearchRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(SearchFormState())
    val form: StateFlow<SearchFormState> = _form.asStateFlow()

    private val _results = MutableStateFlow<SearchResultsState>(SearchResultsState.Idle)
    val results: StateFlow<SearchResultsState> = _results.asStateFlow()

    init {
        // Restore last successful query/result if the user is coming back to the screen.
        repository.lastResult()?.let { (query, response) ->
            _form.value = SearchFormState(
                city = query.city,
                checkIn = query.checkIn,
                checkOut = query.checkOut,
                guests = query.guests,
                amenities = query.amenities.toSet(),
                minPrice = query.minPrice,
                maxPrice = query.maxPrice,
                orderBy = query.orderBy,
                orderDir = query.orderDir,
            )
            _results.value = SearchResultsState.Success(response)
        }
    }

    fun onCityChange(value: String) = _form.update { it.copy(city = value, validation = it.validation.copy(cityError = null)) }
    fun onCheckInChange(value: String) = _form.update { it.copy(checkIn = value, validation = it.validation.copy(checkInError = null)) }
    fun onCheckOutChange(value: String) = _form.update { it.copy(checkOut = value, validation = it.validation.copy(checkOutError = null)) }
    fun onGuestsChange(value: Int) = _form.update { it.copy(guests = value.coerceAtLeast(1), validation = it.validation.copy(guestsError = null)) }
    fun onMinPriceChange(value: Int?) = _form.update { it.copy(minPrice = value) }
    fun onMaxPriceChange(value: Int?) = _form.update { it.copy(maxPrice = value) }
    fun onOrderByChange(value: SearchOrderBy?) = _form.update {
        it.copy(orderBy = value, orderDir = defaultDirFor(value))
    }
    fun onOrderDirChange(value: SearchOrderDir?) = _form.update { it.copy(orderDir = value) }

    private fun defaultDirFor(orderBy: SearchOrderBy?): SearchOrderDir? = when (orderBy) {
        SearchOrderBy.PRICE -> SearchOrderDir.ASC   // cheapest first
        SearchOrderBy.RATING -> SearchOrderDir.DESC // best first
        SearchOrderBy.NAME -> SearchOrderDir.ASC    // A-Z
        null -> null
    }

    fun toggleAmenity(amenity: String) = _form.update {
        val updated = if (it.amenities.contains(amenity)) it.amenities - amenity else it.amenities + amenity
        it.copy(amenities = updated)
    }

    fun submit() {
        val current = _form.value
        val validated = validate(current)
        if (!validated.isValid) {
            _form.value = validated
            return
        }
        _form.value = validated
        runQuery(buildQuery(validated, page = 1))
    }

    fun loadNextPage() {
        val state = _results.value as? SearchResultsState.Success ?: return
        val current = state.response.pagination
        if (current.page >= current.totalPages) return
        runQuery(buildQuery(_form.value, page = current.page + 1))
    }

    private fun runQuery(query: SearchQuery) {
        viewModelScope.launch {
            _results.value = SearchResultsState.Loading
            repository.search(query)
                .onSuccess { _results.value = SearchResultsState.Success(it) }
                .onFailure { error ->
                    _results.value = SearchResultsState.Error(
                        error.message?.let { ErrorMessage.Plain(it) }
                            ?: ErrorMessage.Resource(R.string.search_error_generic)
                    )
                }
        }
    }

    private fun validate(form: SearchFormState): SearchFormState {
        val validation = SearchFormValidation(
            cityError = if (form.city.isBlank()) ErrorMessage.Resource(R.string.search_error_city_required) else null,
            checkInError = if (form.checkIn.isBlank()) ErrorMessage.Resource(R.string.search_error_check_in_required) else null,
            checkOutError = when {
                form.checkOut.isBlank() -> ErrorMessage.Resource(R.string.search_error_check_out_required)
                form.checkIn.isNotBlank() && form.checkOut <= form.checkIn ->
                    ErrorMessage.Resource(R.string.search_error_check_out_after_check_in)
                else -> null
            },
            guestsError = if (form.guests < 1) ErrorMessage.Resource(R.string.search_error_guests_min) else null,
        )
        return form.copy(validation = validation)
    }

    private fun buildQuery(form: SearchFormState, page: Int): SearchQuery = SearchQuery(
        city = form.city.trim(),
        checkIn = form.checkIn,
        checkOut = form.checkOut,
        guests = form.guests,
        amenities = form.amenities.toList(),
        minPrice = form.minPrice,
        maxPrice = form.maxPrice,
        orderBy = form.orderBy,
        orderDir = form.orderDir,
        page = page,
        pageSize = DEFAULT_PAGE_SIZE,
    )

    class Factory(private val repository: SearchRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(repository) as T
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 8
    }
}
