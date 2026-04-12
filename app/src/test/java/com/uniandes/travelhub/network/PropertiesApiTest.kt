package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class PropertiesApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PropertiesApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(PropertiesApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getProperties sends GET to correct path and deserializes list`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": "1",
                        "name": "Casa Playa",
                        "description": "Beach house",
                        "location": "Cartagena",
                        "latitude": 10.4,
                        "longitude": -75.5,
                        "price_per_night": 250.0,
                        "currency": "USD",
                        "rating": 4.8,
                        "review_count": 15,
                        "bedrooms": 3,
                        "bathrooms": 2.0,
                        "max_guests": 6,
                        "amenities": ["WiFi", "Pool"],
                        "images": [],
                        "reviews": [],
                        "status": 1
                      }
                    ]
                    """.trimIndent()
                )
        )

        val result = api.getProperties()

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
        assertEquals("Casa Playa", result[0].name)
        assertEquals("Cartagena", result[0].location)
        assertEquals(250.0, result[0].pricePerNight, 0.01)
        assertEquals(listOf("WiFi", "Pool"), result[0].amenities)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v1/properties", recorded.path)
    }

    @Test
    fun `getPropertyDetail sends GET with id path and deserializes response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id": "42",
                      "name": "Mountain Lodge",
                      "description": "A cozy lodge",
                      "location": "Villa de Leyva",
                      "latitude": 5.6,
                      "longitude": -73.5,
                      "price_per_night": 180.0,
                      "currency": "USD",
                      "rating": 4.9,
                      "review_count": 30,
                      "bedrooms": 2,
                      "bathrooms": 1.5,
                      "max_guests": 4,
                      "amenities": ["Fireplace"],
                      "images": [
                        {"id": "img1", "url": "https://example.com/img.jpg", "alt_text": "Front", "position": 0}
                      ],
                      "reviews": [
                        {"id": "rev1", "author": "Ada", "rating": 5, "date": "2026-01-01", "comment": "Amazing!", "verified_stay": true}
                      ],
                      "status": 1
                    }
                    """.trimIndent()
                )
        )

        val result = api.getPropertyDetail("42")

        assertEquals("42", result.id)
        assertEquals("Mountain Lodge", result.name)
        assertEquals(1, result.images.size)
        assertEquals("https://example.com/img.jpg", result.images[0].url)
        assertEquals(1, result.reviews.size)
        assertEquals("Ada", result.reviews[0].author)
        assertTrue(result.reviews[0].verifiedStay)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v1/properties/42", recorded.path)
    }

    @Test(expected = HttpException::class)
    fun `getProperties throws HttpException on non-2xx response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"detail":"Internal server error"}""")
        )

        api.getProperties()
    }
}
