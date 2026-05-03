package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.payments.ChargeResponse
import com.uniandes.travelhub.models.payments.CreateChargeRequest
import com.uniandes.travelhub.models.payments.PaymentsConfig
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.PaymentsApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsRepositoryTest {

    private val api: PaymentsApi = mockk()
    private val tokenStore: AuthTokenStore = mockk()

    @Test
    fun `getConfig returns success when api succeeds`() = runTest {
        val config = PaymentsConfig(provider = "fake_stripe", stripeEnabled = false, publishableKey = "pk_test")
        coEvery { api.getConfig() } returns config

        val result = PaymentsRepository(api, tokenStore).getConfig()

        assertTrue(result.isSuccess)
        assertEquals(config, result.getOrNull())
    }

    @Test
    fun `charge fails fast when no user id is available`() = runTest {
        every { tokenStore.userIdFlow } returns flowOf(null)

        val result = PaymentsRepository(api, tokenStore).charge(
            reservationId = "r-1",
            amountInCents = 1000,
            currency = "COP",
            paymentMethodToken = "pm_tok_visa_ok",
            idempotencyKey = "k-1",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PaymentException)
        coVerify(exactly = 0) { api.charge(any()) }
    }

    @Test
    fun `charge sends traveler id from token store`() = runTest {
        every { tokenStore.userIdFlow } returns flowOf("user-42")
        val captured = slot<CreateChargeRequest>()
        coEvery { api.charge(capture(captured)) } returns ChargeResponse(
            paymentId = "p-1",
            reservationId = "r-1",
            status = "confirmed",
            amountInCents = 1000,
            currency = "COP",
        )

        val result = PaymentsRepository(api, tokenStore).charge(
            reservationId = "r-1",
            amountInCents = 1000,
            currency = "COP",
            paymentMethodToken = "pm_tok_visa_ok",
            idempotencyKey = "k-1",
        )

        assertTrue(result.isSuccess)
        assertEquals("user-42", captured.captured.travelerId)
        assertEquals("r-1", captured.captured.reservationId)
        assertEquals("k-1", captured.captured.idempotencyKey)
    }
}
