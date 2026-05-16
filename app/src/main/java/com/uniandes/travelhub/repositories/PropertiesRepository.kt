package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.PropertyCacheStore
import com.uniandes.travelhub.network.PropertiesApi

class PropertiesRepository(
    private val propertiesApi: PropertiesApi,
    private val cacheStore: PropertyCacheStore? = null,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {
    private val memoryCache = linkedMapOf<String, Property>()

    suspend fun getProperties(): Result<List<Property>> = runCatching {
        propertiesApi.getProperties()
    }.onSuccess { properties ->
        properties.forEach { cacheProperty(it) }
    }.recoverFailure()

    suspend fun getPropertiesByOwner(ownerId: String): Result<List<Property>> = runCatching {
        propertiesApi.getPropertiesByOwner(ownerId)
    }.onSuccess { properties ->
        properties.forEach { cacheProperty(it) }
    }.recoverFailure()

    suspend fun getPropertyDetail(
        id: String,
        checkIn: String? = null,
        checkOut: String? = null,
    ): Result<Property> = runCatching {
        propertiesApi.getPropertyDetail(id, checkIn, checkOut)
    }.onSuccess { property ->
        // Only cache the canonical (no-range) response so range-specific prices
        // don't poison the cache for other ranges.
        if (checkIn == null && checkOut == null) cacheProperty(property)
    }.recoverFailure()

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

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(Exception(parseDetail(it), it)) }
    )
}
