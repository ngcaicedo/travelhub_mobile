package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeasonalPricingListResponse(
    @Json(name = "items") val items: List<SeasonalPricingResponse> = emptyList(),
    @Json(name = "total") val total: Int = 0,
)
