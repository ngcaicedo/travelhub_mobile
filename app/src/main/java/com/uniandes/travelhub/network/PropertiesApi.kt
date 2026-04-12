package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.properties.Property
import retrofit2.http.GET
import retrofit2.http.Path

interface PropertiesApi {

    @GET("api/v1/properties")
    suspend fun getProperties(): List<Property>

    @GET("api/v1/properties/{id}")
    suspend fun getProperty(@Path("id") id: String): Property

    @GET("api/v1/properties/{id}")
    suspend fun getPropertyDetail(@Path("id") id: String): Property
}
