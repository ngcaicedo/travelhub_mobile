package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.PropertyCacheStore
import com.uniandes.travelhub.network.PropertiesApi

class PropertiesRepository(
    private val propertiesApi: PropertiesApi,
    private val cacheStore: PropertyCacheStore? = null,
    private val errorParser: (Throwable, String) -> String = ApiErrorParser::getApiErrorMessage
) {
    private val memoryCache = linkedMapOf<String, Property>()

    suspend fun getProperties(): Result<List<Property>> = runCatching {
        propertiesApi.getProperties()
    }.onSuccess { properties ->
        properties.forEach { cacheProperty(it) }
    }.recoverFailure("No fue posible cargar las propiedades")

    suspend fun getPropertyDetail(id: String): Result<Property> = runCatching {
        propertiesApi.getPropertyDetail(id)
    }.onSuccess { property ->
        cacheProperty(property)
    }.recoverFailure("No fue posible cargar el detalle de la propiedad")

    fun primePropertyPreview(property: Property) {
        memoryCache[property.id] = property
    }

    suspend fun getCachedProperty(id: String): Property? {
        memoryCache[id]?.let { return it }
        val cached = cacheStore?.getProperty(id)
        if (cached != null) {
            memoryCache[id] = cached
        }
        return cached
    }

    private suspend fun cacheProperty(property: Property) {
        memoryCache[property.id] = property
        cacheStore?.saveProperty(property)
    }

    private fun <T> Result<T>.recoverFailure(fallback: String): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            val message = errorParser(throwable, fallback)
            Result.failure(Exception(message, throwable))
        }
    )
}
