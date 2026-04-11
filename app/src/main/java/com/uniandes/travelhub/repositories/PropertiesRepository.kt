package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.PropertiesApi

class PropertiesRepository(
    private val propertiesApi: PropertiesApi,
    private val errorParser: (Throwable, String) -> String = ApiErrorParser::getApiErrorMessage
) {

    suspend fun getProperties(): Result<List<Property>> = runCatching {
        propertiesApi.getProperties()
    }.recoverFailure("No fue posible cargar las propiedades")

    suspend fun getPropertyDetail(id: String): Result<Property> = runCatching {
        propertiesApi.getPropertyDetail(id)
    }.recoverFailure("No fue posible cargar el detalle de la propiedad")

    private fun <T> Result<T>.recoverFailure(fallback: String): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            val message = errorParser(throwable, fallback)
            Result.failure(Exception(message, throwable))
        }
    )
}
