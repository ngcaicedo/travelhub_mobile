package com.uniandes.travelhub.models.properties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Property(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "location") val location: String,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "price_per_night") val pricePerNight: Double,
    @Json(name = "currency") val currency: String,
    @Json(name = "rating") val rating: Double,
    @Json(name = "review_count") val reviewCount: Int,
    @Json(name = "bedrooms") val bedrooms: Int,
    @Json(name = "bathrooms") val bathrooms: Double,
    @Json(name = "max_guests") val maxGuests: Int,
    @Json(name = "amenities") val amenities: List<String>,
    @Json(name = "images") val images: List<PropertyImage> = emptyList(),
    @Json(name = "reviews") val reviews: List<PropertyReview> = emptyList(),
    @Json(name = "status") val status: Int
)

@JsonClass(generateAdapter = true)
data class PropertyImage(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String,
    @Json(name = "alt_text") val altText: String?,
    @Json(name = "position") val position: Int
)

@JsonClass(generateAdapter = true)
data class PropertyReview(
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String,
    @Json(name = "rating") val rating: Int,
    @Json(name = "date") val date: String,
    @Json(name = "comment") val comment: String,
    @Json(name = "verified_stay") val verifiedStay: Boolean
)
