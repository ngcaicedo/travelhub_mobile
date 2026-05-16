package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeasonalPricingResponse(
    @Json(name = "id") val id: String,
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "season_start") val seasonStart: String,
    @Json(name = "season_end") val seasonEnd: String,
    @Json(name = "price_per_night") val pricePerNight: Double,
    @Json(name = "currency") val currency: String,
    @Json(name = "tax_rate") val taxRate: Double,
    @Json(name = "cleaning_fee") val cleaningFee: Double,
    @Json(name = "signature_hash") val signatureHash: String,
    @Json(name = "signature_algo") val signatureAlgo: String,
    @Json(name = "integrity_locked") val integrityLocked: Boolean,
    @Json(name = "integrity_checked_at") val integrityCheckedAt: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "integrity_valid") val integrityValid: Boolean = true,
)
