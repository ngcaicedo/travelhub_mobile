package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeasonalPricingWritePayload(
    @Json(name = "season_start") val seasonStart: String,
    @Json(name = "season_end") val seasonEnd: String,
    @Json(name = "price_per_night") val pricePerNight: Double,
    @Json(name = "currency") val currency: String = "COP",
    @Json(name = "tax_rate") val taxRate: Double = 0.0,
    @Json(name = "cleaning_fee") val cleaningFee: Double = 0.0,
)
