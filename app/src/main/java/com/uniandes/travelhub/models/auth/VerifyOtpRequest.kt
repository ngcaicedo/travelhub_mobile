package com.uniandes.travelhub.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    val email: String,
    @Json(name = "otp_code") val otpCode: String
)
