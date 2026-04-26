package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.PropertyCacheStore
import com.uniandes.travelhub.network.PropertiesApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PropertiesRepositoryTest {

    private lateinit var api: PropertiesApi
    private lateinit var cacheStore: PropertyCacheStore
    private lateinit var repository: PropertiesRepository

    @Before
    fun setUp() {
        api = mockk()
        cacheStore = mockk(relaxed = true)
        repository = PropertiesRepository(
            propertiesApi = api,
            cacheStore = cacheStore,
            errorParser = { throwable, fallback -> throwable.message ?: fallback }
        )
    }

    // ---------- getProperties ----------

    @Test
    fun `getProperties success returns property list`() = runTest {
        val properties = listOf(sampleProperty("1"), sampleProperty("2"))
        coEvery { api.getProperties() } returns properties

        val result = repository.getProperties()

        assertTrue(result.isSuccess)
        assertEquals(properties, result.getOrNull())
        coVerify(exactly = 1) { cacheStore.saveProperty(properties[0]) }
        coVerify(exactly = 1) { cacheStore.saveProperty(properties[1]) }
    }

    @Test
    fun `getProperties wraps exception with parsed message`() = runTest {
        coEvery { api.getProperties() } throws IOException("network down")

        val result = repository.getProperties()

        assertTrue(result.isFailure)
        assertEquals("network down", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getProperties uses fallback when exception message is null`() = runTest {
        coEvery { api.getProperties() } throws RuntimeException()

        val result = repository.getProperties()

        assertTrue(result.isFailure)
        assertEquals("No fue posible cargar las propiedades", result.exceptionOrNull()?.message)
    }

    // ---------- getPropertyDetail ----------

    @Test
    fun `getPropertyDetail success returns property`() = runTest {
        val property = sampleProperty("42")
        coEvery { api.getPropertyDetail("42") } returns property

        val result = repository.getPropertyDetail("42")

        assertTrue(result.isSuccess)
        assertEquals(property, result.getOrNull())
        coVerify(exactly = 1) { cacheStore.saveProperty(property) }
    }

    @Test
    fun `getPropertyDetail wraps exception with parsed message`() = runTest {
        coEvery { api.getPropertyDetail(any()) } throws IOException("timeout")

        val result = repository.getPropertyDetail("42")

        assertTrue(result.isFailure)
        assertEquals("timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getCachedProperty returns in-memory preview before store`() = runTest {
        val property = sampleProperty("42")
        repository.primePropertyPreview(property)

        val result = repository.getCachedProperty("42")

        assertEquals(property, result)
    }

    @Test
    fun `getCachedProperty loads persisted property when memory cache misses`() = runTest {
        val property = sampleProperty("84")
        coEvery { cacheStore.getProperty("84") } returns property

        val result = repository.getCachedProperty("84")

        assertEquals(property, result)
        coVerify(exactly = 1) { cacheStore.getProperty("84") }
    }

    // ---------- helpers ----------

    private fun sampleProperty(id: String) = Property(
        id = id,
        name = "Test Property $id",
        description = "A test property",
        location = "Bogotá",
        latitude = 4.6,
        longitude = -74.1,
        pricePerNight = 100.0,
        currency = "COP",
        rating = 4.5,
        reviewCount = 10,
        bedrooms = 2,
        bathrooms = 1.0,
        maxGuests = 4,
        amenities = listOf("WiFi"),
        images = emptyList(),
        reviews = emptyList(),
        status = 1
    )
}
