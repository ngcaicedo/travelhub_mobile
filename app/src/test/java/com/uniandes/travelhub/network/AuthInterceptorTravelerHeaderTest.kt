package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.UserRole
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Verifies that AuthInterceptor adds X-Traveler-Id only for traveler-role tokens.
 * Uses a hand-crafted JWT with `sub=user-42` (HS256 header & payload, signature ignored).
 */
class AuthInterceptorTravelerHeaderTest {

    private lateinit var server: MockWebServer

    // Header: {"alg":"HS256","typ":"JWT"}
    // Payload: {"sub":"user-42","email":"a@b.com","role":"traveler"}
    // Signature is irrelevant for the interceptor — JwtUtils only decodes payload.
    private val travelerToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
        ".eyJzdWIiOiJ1c2VyLTQyIiwiZW1haWwiOiJhQGIuY29tIiwicm9sZSI6InRyYXZlbGVyIn0" +
        ".sig"

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `traveler request gets X-Traveler-Id from JWT sub`() {
        val tokenStore: AuthTokenStore = mockk()
        every { tokenStore.tokenFlow } returns flowOf(travelerToken)
        every { tokenStore.roleFlow } returns flowOf(UserRole.TRAVELER)

        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        client.newCall(Request.Builder().url(server.url("/anything")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer $travelerToken", recorded.getHeader("Authorization"))
        assertEquals("user-42", recorded.getHeader(AuthInterceptor.HEADER_TRAVELER_ID))
    }

    @Test
    fun `non-traveler request does not include X-Traveler-Id`() {
        val tokenStore: AuthTokenStore = mockk()
        every { tokenStore.tokenFlow } returns flowOf(travelerToken)
        every { tokenStore.roleFlow } returns flowOf(UserRole.HOTEL_PARTNER)

        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        client.newCall(Request.Builder().url(server.url("/anything")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader(AuthInterceptor.HEADER_TRAVELER_ID))
    }
}
