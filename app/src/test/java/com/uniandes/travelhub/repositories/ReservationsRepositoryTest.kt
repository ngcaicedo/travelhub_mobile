package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationConfirmResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.ReservationsApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationsRepositoryTest {

    private val api: ReservationsApi = mockk()
    private val tokenStore: AuthTokenStore = mockk()

    private fun reservation(id: String = "r-1") = ReservationResponse(
        id = id,
        status = "pending_payment",
        totalPrice = "100.00",
        currency = "COP",
        checkInDate = "2026-06-10",
        checkOutDate = "2026-06-12",
    )

    @Test
    fun `create defaults id_room to id_property when caller does not provide one`() = runTest {
        every { tokenStore.userIdFlow } returns flowOf("user-42")
        val captured = slot<CreateReservationRequest>()
        coEvery { api.create(capture(captured)) } returns reservation()

        ReservationsRepository(api, tokenStore).create(
            propertyId = "prop-1",
            checkIn = "2026-06-10",
            checkOut = "2026-06-12",
            guests = 2,
            currency = "COP",
        )

        assertEquals("prop-1", captured.captured.idRoom)
        assertEquals("prop-1", captured.captured.idProperty)
    }

    @Test
    fun `create respects an explicit roomId when provided`() = runTest {
        every { tokenStore.userIdFlow } returns flowOf("user-42")
        val captured = slot<CreateReservationRequest>()
        coEvery { api.create(capture(captured)) } returns reservation()

        ReservationsRepository(api, tokenStore).create(
            propertyId = "prop-1",
            checkIn = "2026-06-10",
            checkOut = "2026-06-12",
            guests = 2,
            currency = "COP",
            roomId = "room-explicit",
        )

        assertEquals("room-explicit", captured.captured.idRoom)
    }

    @Test
    fun `create fails fast when no user id is in the session`() = runTest {
        every { tokenStore.userIdFlow } returns flowOf(null)

        val result = ReservationsRepository(api, tokenStore).create(
            propertyId = "prop-1",
            checkIn = "2026-06-10",
            checkOut = "2026-06-12",
            guests = 2,
            currency = "COP",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ReservationException)
        coVerify(exactly = 0) { api.create(any()) }
    }

    @Test
    fun `previewModification forwards the request body`() = runTest {
        coEvery {
            api.previewModification(
                "r-1",
                match {
                    it.checkInDate == "2026-06-15" && it.checkOutDate == "2026-06-17" && it.numberOfGuests == 3
                },
            )
        } returns ReservationModificationPreviewResponse(
            deltaAmount = "50.00",
            changeAllowed = true,
        )

        val result = ReservationsRepository(api, tokenStore).previewModification(
            reservationId = "r-1",
            checkIn = "2026-06-15",
            checkOut = "2026-06-17",
            guests = 3,
        )

        assertTrue(result.isSuccess)
        assertEquals("50.00", result.getOrNull()?.deltaAmount)
    }

    @Test
    fun `confirmModification generates a non-empty idempotency key by default`() = runTest {
        val captured = slot<ReservationModificationConfirmRequest>()
        coEvery {
            api.confirmModification(eq("r-1"), capture(captured))
        } returns confirmResponse()

        val result = ReservationsRepository(api, tokenStore).confirmModification(
            reservationId = "r-1",
            checkIn = "2026-06-15",
            checkOut = "2026-06-17",
            guests = 3,
        )

        assertTrue(result.isSuccess)
        // Idempotency key was generated — non-empty UUID-shaped string.
        assertTrue(captured.captured.idempotencyKey.isNotBlank())
        assertNotEquals("", captured.captured.idempotencyKey)
    }

    @Test
    fun `previewCancellation passes the reservation id`() = runTest {
        coEvery { api.previewCancellation("r-99") } returns ReservationCancellationPreviewResponse(
            refundAmount = "1000.00",
            penaltyAmount = "0.00",
            refundType = "full_refund",
            changeAllowed = true,
        )

        val result = ReservationsRepository(api, tokenStore).previewCancellation("r-99")

        assertTrue(result.isSuccess)
        assertEquals("full_refund", result.getOrNull()?.refundType)
    }

    @Test
    fun `confirmCancellation forwards reason and generates idempotency key`() = runTest {
        val captured = slot<ReservationCancellationConfirmRequest>()
        coEvery {
            api.confirmCancellation(eq("r-1"), capture(captured))
        } returns confirmResponse()

        ReservationsRepository(api, tokenStore).confirmCancellation(
            reservationId = "r-1",
            reason = "schedule changed",
        )

        assertEquals("schedule changed", captured.captured.reason)
        assertTrue(captured.captured.idempotencyKey.isNotBlank())
    }

    private fun confirmResponse() = ReservationConfirmResponse(
        reservation = reservation(),
        statusBefore = "confirmed",
        statusAfter = "modification_confirmed",
        actionApplied = "modification_confirmed",
    )

    @Suppress("unused")
    private fun assertNotNullPlaceholder() = assertNotNull("unused helper to silence imports", Any())
}
