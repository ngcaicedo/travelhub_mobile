package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.properties.Property
import retrofit2.http.GET
import retrofit2.http.Path

interface PropertiesApi {

    @GET("properties")
    suspend fun getProperties(): List<Property>

    @GET("properties/{id}")
    suspend fun getPropertyDetail(@Path("id") id: String): Property
}
