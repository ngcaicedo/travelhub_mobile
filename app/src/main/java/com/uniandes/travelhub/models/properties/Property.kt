package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Property(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "location") val location: String,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "price_per_night") val pricePerNight: Double,
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "review_count") val reviewCount: Int = 0,
    @Json(name = "bedrooms") val bedrooms: Int = 1,
    @Json(name = "bathrooms") val bathrooms: Double = 1.0,
    @Json(name = "max_guests") val maxGuests: Int = 2,
    @Json(name = "amenities") val amenities: List<String> = emptyList(),
    @Json(name = "cancellation_policy") val cancellationPolicy: String = "",
    @Json(name = "tax_rate") val taxRate: Double = 0.0,
    @Json(name = "cleaning_fee") val cleaningFee: Double = 0.0,
    @Json(name = "images") val images: List<PropertyImage> = emptyList(),
    @Json(name = "reviews") val reviews: List<PropertyReview> = emptyList(),
    @Json(name = "property_type") val propertyType: String = "Apartment",
    @Json(name = "status") val status: Int = 0
)

@JsonClass(generateAdapter = true)
data class PropertyImage(
    @Json(name = "id") val id: String = "",
    @Json(name = "url") val url: String,
    @Json(name = "url_hires") val urlHires: String? = null,
    @Json(name = "alt_text") val altText: String? = null,
    @Json(name = "is_cover") val isCover: Boolean = false,
    @Json(name = "position") val position: Int = 0
)

@JsonClass(generateAdapter = true)
data class PropertyReview(
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String,
    @Json(name = "rating") val rating: Int,
    @Json(name = "review_date") val reviewDate: String,
    @Json(name = "comment") val comment: String,
    @Json(name = "verified_stay") val verifiedStay: Boolean
)
