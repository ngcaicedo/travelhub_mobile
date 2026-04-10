package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.auth.LoginRequest
import com.uniandes.travelhub.models.auth.LoginResponse
import com.uniandes.travelhub.models.auth.TokenResponse
import com.uniandes.travelhub.models.auth.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface SecurityApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): TokenResponse
}
