package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.PropertiesApi

class PropertiesRepository(private val propertiesApi: PropertiesApi) {
    suspend fun getProperties(): Result<List<Property>> = try {
        Result.success(propertiesApi.getProperties())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPropertyDetail(id: String): Result<Property> = try {
        Result.success(propertiesApi.getProperty(id))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
