package com.uniandes.travelhub.models.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Input for a search request. Dates are ISO YYYY-MM-DD; passed as query params by the API.
 */
data class SearchQuery(
    val city: String,
    val checkIn: String,
    val checkOut: String,
    val guests: Int,
    val amenities: List<String> = emptyList(),
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val orderBy: SearchOrderBy? = null,
    val orderDir: SearchOrderDir? = null,
    val page: Int = 1,
    val pageSize: Int = 8,
)

enum class SearchOrderBy(val wire: String) {
    PRICE("price"),
    RATING("rating"),
    NAME("name"),
}

enum class SearchOrderDir(val wire: String) {
    ASC("asc"),
    DESC("desc"),
}

@JsonClass(generateAdapter = true)
data class SearchResultItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "city") val city: String,
    @Json(name = "country") val country: String,
    @Json(name = "max_capacity") val maxCapacity: Int,
    @Json(name = "main_image_url") val mainImageUrl: String? = null,
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "price_from") val priceFrom: String,
    @Json(name = "base_price_from") val basePriceFrom: String? = null,
    @Json(name = "has_seasonal_discount") val hasSeasonalDiscount: Boolean = false,
    @Json(name = "currency") val currency: String,
    @Json(name = "amenities") val amenities: List<String> = emptyList(),
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
)

@JsonClass(generateAdapter = true)
data class SearchPagination(
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "page_size") val pageSize: Int,
    @Json(name = "total_pages") val totalPages: Int,
)

@JsonClass(generateAdapter = true)
data class SearchEmptyStateItem(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String,
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "items") val items: List<SearchResultItem> = emptyList(),
    @Json(name = "pagination") val pagination: SearchPagination,
    @Json(name = "empty_state") val emptyState: List<SearchEmptyStateItem> = emptyList(),
)

object Amenities {
    const val WIFI = "wifi"
    const val POOL = "pool"
    const val BREAKFAST = "breakfast"
    const val AIR_CONDITIONING = "air_conditioning"
    const val PET_FRIENDLY = "pet_friendly"
    const val PARKING = "parking"
    const val GYM = "gym"
    const val SPA = "spa"

    val ALL: List<String> = listOf(
        WIFI, POOL, BREAKFAST, AIR_CONDITIONING, PET_FRIENDLY, PARKING, GYM, SPA
    )
}
