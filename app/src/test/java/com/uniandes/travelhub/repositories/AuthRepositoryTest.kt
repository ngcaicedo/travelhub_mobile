package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.LoginRequest
import com.uniandes.travelhub.models.auth.LoginResponse
import com.uniandes.travelhub.models.auth.RegisterRequest
import com.uniandes.travelhub.models.auth.TokenResponse
import com.uniandes.travelhub.models.auth.UserResponse
import com.uniandes.travelhub.models.auth.VerifyOtpRequest
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.SecurityApi
import com.uniandes.travelhub.network.UsersApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest {

    private lateinit var securityApi: SecurityApi
    private lateinit var usersApi: UsersApi
    private lateinit var tokenStore: AuthTokenStore
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        securityApi = mockk()
        usersApi = mockk()
        tokenStore = mockk(relaxed = true)
        repository = AuthRepository(
            securityApi = securityApi,
            usersApi = usersApi,
            tokenStore = tokenStore,
            parseDetail = { it.message }
        )
    }

    // ----- login -----

    @Test
    fun `login success returns Unit and does NOT touch token store`() = runTest {
        coEvery {
            securityApi.login(LoginRequest("ada@example.com", "Sup3rSecret!"))
        } returns LoginResponse("OTP enviado")

        val result = repository.login("ada@example.com", "Sup3rSecret!")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { tokenStore.saveSession(any(), any()) }
    }

    @Test
    fun `login wraps HttpException into AuthException with parsed message`() = runTest {
        val http = HttpException(
            Response.error<Any>(401, "{}".toResponseBody("application/json".toMediaType()))
        )
        coEvery { securityApi.login(any()) } throws http

        val result = repository.login("ada@example.com", "wrong")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AuthException)
        assertSame(http, error?.cause)
    }

    @Test
    fun `login wraps IOException into AuthException with fallback message`() = runTest {
        val io = IOException("network down")
        coEvery { securityApi.login(any()) } throws io

        val result = repository.login("ada@example.com", "x")

        val error = result.exceptionOrNull()
        assertTrue(error is AuthException)
        assertEquals("network down", error?.message)
    }

    // ----- verifyOtp -----

    @Test
    fun `verifyOtp success persists session and returns role`() = runTest {
        coEvery {
            securityApi.verifyOtp(VerifyOtpRequest("ada@example.com", "123456"))
        } returns TokenResponse(
            accessToken = "jwt.payload.sig",
            tokenType = "bearer",
            role = UserRole.HOTEL_PARTNER
        )

        val result = repository.verifyOtp("ada@example.com", "123456")

        assertEquals(UserRole.HOTEL_PARTNER, result.getOrNull())
        coVerify(exactly = 1) {
            tokenStore.saveSession("jwt.payload.sig", UserRole.HOTEL_PARTNER)
        }
    }

    @Test
    fun `verifyOtp failure does NOT persist session`() = runTest {
        coEvery { securityApi.verifyOtp(any()) } throws RuntimeException("invalid OTP")

        val result = repository.verifyOtp("ada@example.com", "000000")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { tokenStore.saveSession(any(), any()) }
    }

    // ----- register -----

    @Test
    fun `register success returns UserResponse from the api`() = runTest {
        val payload = RegisterRequest(
            email = "ada@example.com",
            phone = "+573001234567",
            password = "Sup3rSecret!",
            fullName = "Ada Lovelace",
            hotelName = null,
            role = UserRole.TRAVELER
        )
        val response = UserResponse(
            id = "u_42",
            email = "ada@example.com",
            phone = "+573001234567",
            fullName = "Ada Lovelace",
            hotelName = null,
            status = 0
        )
        coEvery { usersApi.register(payload) } returns response

        val result = repository.register(payload)

        assertEquals(response, result.getOrNull())
    }

    @Test
    fun `register failure wraps exception as AuthException`() = runTest {
        coEvery { usersApi.register(any()) } throws RuntimeException("email taken")

        val result = repository.register(
            RegisterRequest(
                email = "dup@example.com",
                phone = "+57",
                password = "x",
                fullName = "x",
                hotelName = null,
                role = UserRole.TRAVELER
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthException)
        assertEquals("email taken", result.exceptionOrNull()?.message)
    }

    // ----- logout / observe -----

    @Test
    fun `logout delegates to tokenStore clear`() = runTest {
        repository.logout()

        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun `observeToken delegates to tokenStore tokenFlow`() {
        val flow = kotlinx.coroutines.flow.flowOf("abc")
        io.mockk.every { tokenStore.tokenFlow } returns flow

        assertSame(flow, repository.observeToken())
    }

    @Test
    fun `observeRole delegates to tokenStore roleFlow`() {
        val flow = kotlinx.coroutines.flow.flowOf(UserRole.TRAVELER)
        io.mockk.every { tokenStore.roleFlow } returns flow

        assertSame(flow, repository.observeRole())
    }
}
