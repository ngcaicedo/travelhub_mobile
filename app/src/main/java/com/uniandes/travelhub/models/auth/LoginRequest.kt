package com.uniandes.travelhub.models.auth

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)
