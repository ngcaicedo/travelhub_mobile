package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Property(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
    @Json(name = "price_per_night") val pricePerNight: Double,
    val currency: String = "USD",
    val rating: Double = 0.0,
    @Json(name = "review_count") val reviewCount: Int = 0,
    val images: List<PropertyImage> = emptyList(),
    val amenities: List<String> = emptyList(),
    @Json(name = "max_guests") val maxGuests: Int = 2,
    val bedrooms: Double = 1.0,
    val bathrooms: Double = 1.0,
    @Json(name = "property_type") val propertyType: String = "Apartment"
)

@JsonClass(generateAdapter = true)
data class PropertyImage(
    val url: String,
    @Json(name = "is_primary") val isPrimary: Boolean = false,
    @Json(name = "alt_text") val altText: String? = null
)
