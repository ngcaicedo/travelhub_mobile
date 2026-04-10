package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.LoginRequest
import com.uniandes.travelhub.models.auth.VerifyOtpRequest
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

class SecurityApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SecurityApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(SecurityApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login posts JSON body to the right path and parses message`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"message":"OTP enviado"}""")
        )

        val response = api.login(LoginRequest(email = "ada@example.com", password = "Sup3rSecret!"))

        assertEquals("OTP enviado", response.message)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/auth/login", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"email\":\"ada@example.com\""))
        assertTrue(body.contains("\"password\":\"Sup3rSecret!\""))
    }

    @Test
    fun `verifyOtp posts otp_code field and parses TokenResponse with role`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "access_token":"jwt.payload.sig",
                      "token_type":"bearer",
                      "role":"hotel_partner"
                    }
                    """.trimIndent()
                )
        )

        val response = api.verifyOtp(
            VerifyOtpRequest(email = "ada@example.com", otpCode = "123456")
        )

        assertEquals("jwt.payload.sig", response.accessToken)
        assertEquals("bearer", response.tokenType)
        assertEquals(UserRole.HOTEL_PARTNER, response.role)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/auth/verify-otp", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue("body must use snake_case otp_code, was: $body", body.contains("\"otp_code\":\"123456\""))
    }

    @Test(expected = HttpException::class)
    fun `login surfaces HttpException on 401`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"detail":"Credenciales inválidas"}""")
        )

        api.login(LoginRequest(email = "ada@example.com", password = "wrong"))
    }
}
