package com.uniandes.travelhub.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uniandes.travelhub.models.UserRole


@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "hotel_name") val hotelName: String? = null,
    val role: UserRole
)
