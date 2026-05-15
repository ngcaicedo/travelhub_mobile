package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.search.PropertyAvailabilityResponse
import com.uniandes.travelhub.models.search.SearchBounds
import com.uniandes.travelhub.models.search.SearchQuery
import com.uniandes.travelhub.models.search.SearchResponse
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.SearchApi

class SearchException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class SearchRepository(
    private val searchApi: SearchApi,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    private var lastResult: Pair<SearchQuery, SearchResponse>? = null

    suspend fun search(query: SearchQuery): Result<SearchResponse> = runCatching {
        searchApi.search(
            city = query.city,
            checkIn = query.checkIn,
            checkOut = query.checkOut,
            guests = query.guests,
            amenities = query.amenities.takeIf { it.isNotEmpty() },
            minPrice = query.minPrice,
            maxPrice = query.maxPrice,
            orderBy = query.orderBy?.wire,
            orderDir = query.orderDir?.wire,
            page = query.page,
            pageSize = query.pageSize,
        )
    }.onSuccess { response ->
        lastResult = query to response
    }.recoverFailure()

    suspend fun checkAvailability(
        propertyId: String,
        checkIn: String,
        checkOut: String,
        guests: Int,
    ): Result<PropertyAvailabilityResponse> = runCatching {
        searchApi.checkAvailability(
            propertyId = propertyId,
            checkIn = checkIn,
            checkOut = checkOut,
            guests = guests,
        )
    }.recoverFailure()

    /**
     * Search the hotels visible inside [bounds]. Used by the mobile map view.
     * `city` and dates are intentionally omitted — the backend will return all
     * properties in the bbox without filtering by availability when dates are absent.
     */
    suspend fun searchByBounds(
        bounds: SearchBounds,
        guests: Int = 1,
        pageSize: Int = 100,
    ): Result<SearchResponse> = runCatching {
        searchApi.search(
            city = null,
            checkIn = null,
            checkOut = null,
            guests = guests,
            minLat = bounds.minLat,
            maxLat = bounds.maxLat,
            minLng = bounds.minLng,
            maxLng = bounds.maxLng,
            page = 1,
            pageSize = pageSize,
        )
    }.recoverFailure()

    /**
     * Returns the last successful (query, response) pair so the UI can restore the
     * previous result without re-fetching when the user navigates back from a detail.
     */
    fun lastResult(): Pair<SearchQuery, SearchResponse>? = lastResult

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(SearchException(parseDetail(it), it)) }
    )
}
