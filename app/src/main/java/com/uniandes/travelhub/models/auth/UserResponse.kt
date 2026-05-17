package com.uniandes.travelhub.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val email: String,
    val phone: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "hotel_name") val hotelName: String? = null,
    val status: Int,
    @Json(name = "country_code") val countryCode: String = "CO",
    @Json(name = "data_region") val dataRegion: String = "aws-us-east-1"
)
