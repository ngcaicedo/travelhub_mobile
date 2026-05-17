package com.uniandes.travelhub.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uniandes.travelhub.models.UserRole

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    val role: UserRole
)
