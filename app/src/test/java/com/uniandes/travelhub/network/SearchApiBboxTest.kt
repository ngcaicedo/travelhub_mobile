package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Verifies the SearchApi sends bbox params and parses lat/lng on the way back. */
class SearchApiBboxTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SearchApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(SearchApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `search forwards bbox query params and parses lat lng`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "items": [
                        {
                          "id": "p1",
                          "name": "Hotel Bogotá",
                          "city": "Bogotá",
                          "country": "Colombia",
                          "max_capacity": 2,
                          "main_image_url": null,
                          "rating": 4.5,
                          "price_from": "200000",
                          "currency": "COP",
                          "amenities": [],
                          "latitude": 4.71,
                          "longitude": -74.07
                        }
                      ],
                      "pagination": {"total": 1, "page": 1, "page_size": 100, "total_pages": 1},
                      "empty_state": []
                    }
                    """.trimIndent()
                )
        )

        val response = api.search(
            guests = 2,
            minLat = 4.55,
            maxLat = 4.78,
            minLng = -74.12,
            maxLng = -74.02,
            page = 1,
            pageSize = 100,
        )

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        val url = req.requestUrl!!
        assertEquals("/api/v1/search", url.encodedPath)
        assertEquals("4.55", url.queryParameter("min_lat"))
        assertEquals("4.78", url.queryParameter("max_lat"))
        assertEquals("-74.12", url.queryParameter("min_lng"))
        assertEquals("-74.02", url.queryParameter("max_lng"))
        // city / check_in / check_out should NOT be sent when null.
        assertTrue(url.queryParameterNames.none { it == "city" || it == "check_in" || it == "check_out" })

        assertEquals(1, response.items.size)
        val item = response.items.first()
        assertNotNull(item.latitude)
        assertNotNull(item.longitude)
        assertEquals(4.71, item.latitude!!, 1e-6)
        assertEquals(-74.07, item.longitude!!, 1e-6)
    }

    @Test
    fun `search omits null lat lng on response gracefully`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "items": [
                        {
                          "id": "p1", "name": "X", "city": "B", "country": "C",
                          "max_capacity": 1, "rating": 0,
                          "price_from": "1", "currency": "COP", "amenities": []
                        }
                      ],
                      "pagination": {"total":1,"page":1,"page_size":1,"total_pages":1},
                      "empty_state": []
                    }
                    """.trimIndent()
                )
        )

        val response = api.search(guests = 1)
        assertEquals(1, response.items.size)
        // latitude/longitude default to null when absent.
        assertEquals(null, response.items.first().latitude)
        assertEquals(null, response.items.first().longitude)
    }
}
