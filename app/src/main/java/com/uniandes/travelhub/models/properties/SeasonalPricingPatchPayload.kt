package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * PATCH partial update payload. Only non-null fields are sent — Moshi must be
 * built with `serializeNulls = false` (which is the default) so the backend
 * receives a true partial body and ignores untouched fields.
 */
@JsonClass(generateAdapter = true)
data class SeasonalPricingPatchPayload(
    @Json(name = "season_start") val seasonStart: String? = null,
    @Json(name = "season_end") val seasonEnd: String? = null,
    @Json(name = "price_per_night") val pricePerNight: Double? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "tax_rate") val taxRate: Double? = null,
    @Json(name = "cleaning_fee") val cleaningFee: Double? = null,
)
