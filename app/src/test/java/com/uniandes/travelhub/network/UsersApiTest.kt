package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.RegisterRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class UsersApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UsersApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(UsersApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `register traveler posts payload without hotel_name and parses UserResponse`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id":"u_42",
                      "email":"ada@example.com",
                      "phone":"+573001234567",
                      "full_name":"Ada Lovelace",
                      "status":0
                    }
                    """.trimIndent()
                )
        )

        val response = api.register(
            RegisterRequest(
                email = "ada@example.com",
                phone = "+573001234567",
                password = "Sup3rSecret!",
                fullName = "Ada Lovelace",
                hotelName = null,
                role = UserRole.TRAVELER
            )
        )

        assertEquals("u_42", response.id)
        assertEquals("Ada Lovelace", response.fullName)
        assertEquals(null, response.hotelName)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/users", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"full_name\":\"Ada Lovelace\""))
        assertTrue(body.contains("\"role\":\"traveler\""))
        assertTrue(
            "Moshi must omit null hotel_name from the wire (was: $body)",
            !body.contains("hotel_name")
        )
    }

    @Test
    fun `register hotel partner posts hotel_name and role hotel_partner`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id":"u_99",
                      "email":"front@hotelplaza.co",
                      "phone":"+573009998877",
                      "full_name":"Front Desk",
                      "hotel_name":"Hotel Plaza",
                      "status":0
                    }
                    """.trimIndent()
                )
        )

        val response = api.register(
            RegisterRequest(
                email = "front@hotelplaza.co",
                phone = "+573009998877",
                password = "An0therOne!",
                fullName = "Front Desk",
                hotelName = "Hotel Plaza",
                role = UserRole.HOTEL_PARTNER
            )
        )

        assertEquals("Hotel Plaza", response.hotelName)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"hotel_name\":\"Hotel Plaza\""))
        assertTrue(body.contains("\"role\":\"hotel_partner\""))
    }
}
