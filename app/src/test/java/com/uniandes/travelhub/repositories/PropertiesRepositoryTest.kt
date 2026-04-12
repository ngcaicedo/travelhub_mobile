package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.PropertiesApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PropertiesRepositoryTest {

    private lateinit var api: PropertiesApi
    private lateinit var repository: PropertiesRepository

    @Before
    fun setUp() {
        api = mockk()
        repository = PropertiesRepository(
            propertiesApi = api,
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
    }

    @Test
    fun `getPropertyDetail wraps exception with parsed message`() = runTest {
        coEvery { api.getPropertyDetail(any()) } throws IOException("timeout")

        val result = repository.getPropertyDetail("42")

        assertTrue(result.isFailure)
        assertEquals("timeout", result.exceptionOrNull()?.message)
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
