package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.auth.RegisterRequest
import com.uniandes.travelhub.models.auth.UserResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface UsersApi {

    @POST("api/v1/users")
    suspend fun register(@Body body: RegisterRequest): UserResponse
}
