package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationException
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reservationsRepository: ReservationsRepository = mockk()
    private val searchRepository: SearchRepository = mockk {
        coEvery { checkAvailability(any(), any(), any(), any()) } returns Result.failure(RuntimeException("not stubbed"))
    }
    private val propertiesRepository: PropertiesRepository = mockk {
        // CheckoutViewModel.init triggers loadProperty(); stub both calls so the
        // VM doesn't crash before we exercise the form/submit logic.
        coEvery { getCachedProperty(any()) } returns null
        coEvery { getPropertyDetail(any()) } returns Result.failure(RuntimeException("not stubbed"))
    }

    @Test
    fun `submit with checkout before checkin flags checkout error and never hits repository`() = runTest {
        val viewModel = CheckoutViewModel(
            propertyId = "p-1",
            reservationsRepository = reservationsRepository,
            propertiesRepository = propertiesRepository,
            searchRepository = searchRepository,
        )
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-08")

        viewModel.submit()

        assertEquals(
            R.string.checkout_error_check_out_after_check_in,
            (viewModel.form.value.checkOutError as ErrorMessage.Resource).id,
        )
        coVerify(exactly = 0) {
            reservationsRepository.create(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `successful submit transitions to Success and emits NavigateToPayment`() = runTest {
        val reservation = ReservationResponse(
            id = "r-1",
            idProperty = "p-1",
            status = "pending_payment",
            totalPrice = "100.00",
            currency = "COP",
            checkInDate = "2026-06-10",
            checkOutDate = "2026-06-15",
        )
        coEvery {
            reservationsRepository.create(
                propertyId = "p-1",
                checkIn = "2026-06-10",
                checkOut = "2026-06-15",
                guests = 2,
                currency = "COP",
                roomId = null,
            )
        } returns Result.success(reservation)

        val viewModel = CheckoutViewModel(
            propertyId = "p-1",
            reservationsRepository = reservationsRepository,
            propertiesRepository = propertiesRepository,
            searchRepository = searchRepository,
        )
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")
        viewModel.onGuestsChange(2)

        viewModel.events.test {
            viewModel.submit()
            val event = awaitItem()
            assertTrue(event is CheckoutEvent.NavigateToPayment)
            assertEquals(reservation, (event as CheckoutEvent.NavigateToPayment).reservation)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(reservation, (viewModel.uiState.value as CheckoutUiState.Success).reservation)
    }

    @Test
    fun `failed submit ends in Error with the parsed message`() = runTest {
        coEvery {
            reservationsRepository.create(any(), any(), any(), any(), any(), any())
        } returns Result.failure(ReservationException("hold expired"))

        val viewModel = CheckoutViewModel(
            propertyId = "p-1",
            reservationsRepository = reservationsRepository,
            propertiesRepository = propertiesRepository,
            searchRepository = searchRepository,
        )
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            ErrorMessage.Plain("hold expired"),
            (viewModel.uiState.value as CheckoutUiState.Error).message,
        )
    }

    @Test
    fun `failed submit with unavailable room detail maps to localized descriptive message`() = runTest {
        coEvery {
            reservationsRepository.create(any(), any(), any(), any(), any(), any())
        } returns Result.failure(ReservationException("Room abc is not available for the selected dates"))

        val viewModel = CheckoutViewModel(
            propertyId = "p-1",
            reservationsRepository = reservationsRepository,
            propertiesRepository = propertiesRepository,
            searchRepository = searchRepository,
        )
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")

        viewModel.submit()
        advanceUntilIdle()

        val error = (viewModel.uiState.value as CheckoutUiState.Error).message as ErrorMessage.Resource
        assertEquals(R.string.checkout_selected_dates_unavailable, error.id)
        assertEquals(2, error.args.size)
    }
}
