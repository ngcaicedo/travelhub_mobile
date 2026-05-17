package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.payments.ChargeResponse
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.payments.PaymentsConfig
import com.uniandes.travelhub.repositories.PaymentException
import com.uniandes.travelhub.repositories.PaymentsRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: PaymentsRepository = mockk()

    private val sampleConfig = PaymentsConfig(
        provider = "fake_stripe",
        stripeEnabled = false,
        publishableKey = "pk_test",
    )

    private val sampleConfirmation = PaymentConfirmationSummary(
        paymentId = "p-1",
        reservationId = "r-1",
        travelerId = "u-1",
        status = "confirmed",
        amountInCents = 1000,
        currency = "COP",
    )

    private fun newViewModel() = PaymentViewModel(
        reservationId = "r-1",
        amountInCents = 1000,
        currency = "COP",
        repository = repository,
    )

    @Test
    fun `initial loadConfig success transitions to Ready`() = runTest {
        coEvery { repository.getConfig() } returns Result.success(sampleConfig)

        val vm = newViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PaymentUiState.Ready)
        assertEquals(sampleConfig, (state as PaymentUiState.Ready).config)
    }

    @Test
    fun `loadConfig failure surfaces a localized error`() = runTest {
        coEvery { repository.getConfig() } returns Result.failure(PaymentException("network down"))

        val vm = newViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PaymentUiState.Failed)
        assertEquals(ErrorMessage.Plain("network down"), (state as PaymentUiState.Failed).message)
    }

    @Test
    fun `pay succeeds end-to-end charge then confirmation transitions to Succeeded`() = runTest {
        coEvery { repository.getConfig() } returns Result.success(sampleConfig)
        coEvery {
            repository.charge(
                reservationId = "r-1",
                amountInCents = 1000,
                currency = "COP",
                paymentMethodToken = "pm_tok_visa_ok",
                idempotencyKey = any(),
            )
        } returns Result.success(
            ChargeResponse(
                paymentId = "p-1",
                reservationId = "r-1",
                status = "confirmed",
                amountInCents = 1000,
                currency = "COP",
            )
        )
        coEvery { repository.getConfirmation("p-1") } returns Result.success(sampleConfirmation)

        val vm = newViewModel()
        advanceUntilIdle()
        vm.pay("pm_tok_visa_ok")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PaymentUiState.Succeeded)
        assertEquals(sampleConfirmation, (state as PaymentUiState.Succeeded).confirmation)
    }

    @Test
    fun `pay returns a non-confirmed status surfaces the failure reason`() = runTest {
        coEvery { repository.getConfig() } returns Result.success(sampleConfig)
        coEvery {
            repository.charge(any(), any(), any(), any(), any())
        } returns Result.success(
            ChargeResponse(
                paymentId = "p-2",
                reservationId = "r-1",
                status = "failed",
                amountInCents = 1000,
                currency = "COP",
                failureReason = "card declined",
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()
        vm.pay("pm_fail_card_declined")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PaymentUiState.Failed)
        assertEquals(ErrorMessage.Plain("card declined"), (state as PaymentUiState.Failed).message)
    }

    @Test
    fun `pay falls back to a Resource error when no failureReason and charge fails generically`() = runTest {
        coEvery { repository.getConfig() } returns Result.success(sampleConfig)
        coEvery {
            repository.charge(any(), any(), any(), any(), any())
        } returns Result.success(
            ChargeResponse(
                paymentId = "p-3",
                reservationId = "r-1",
                status = "rejected",
                amountInCents = 1000,
                currency = "COP",
            )
        )

        val vm = newViewModel()
        advanceUntilIdle()
        vm.pay("pm_tok_visa_ok")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PaymentUiState.Failed)
        assertEquals(
            ErrorMessage.Resource(R.string.payment_error_declined),
            (state as PaymentUiState.Failed).message,
        )
    }
}
