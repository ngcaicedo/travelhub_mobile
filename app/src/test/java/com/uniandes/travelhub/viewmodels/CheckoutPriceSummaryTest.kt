package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Locks the canonical pricing formula to the same numbers the web frontend
 * (`travelhub_frontend/app/utils/pricing.ts`) and the backend
 * (`services/reservations/.../create_reservation.py`) compute, so the three
 * stay in sync. The exact numbers come from the dev backend response for
 * `Mansión Renacentista` × 3 nights × 2 guests:
 *
 *   accommodation = 1240 × 3 × 2          = 7440.00
 *   service_fee   = 7440 × 0.08           =  595.20
 *   cleaning      = (from property)       =  120.00
 *   subtotal      = acc + clean + service = 8155.20
 *   taxes         = subtotal × 0.19       = 1549.488
 *   total                                 = 9704.688 → backend rounds to 9704.69
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutPriceSummaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reservationsRepository: ReservationsRepository = mockk()

    private val mansion = Property(
        id = "p-1",
        name = "Mansión Renacentista & Viñedo Privado",
        description = "",
        location = "Fiesole",
        pricePerNight = 1240.0,
        currency = "COP",
        rating = 4.98,
        reviewCount = 54,
        bedrooms = 4,
        bathrooms = 4.5,
        maxGuests = 12,
        amenities = emptyList(),
        cancellationPolicy = "",
        taxRate = 0.19,
        cleaningFee = 120.0,
    )

    private fun newViewModel(initialCheckIn: String? = null, initialCheckOut: String? = null, guests: Int? = null) =
        CheckoutViewModel(
            propertyId = "p-1",
            reservationsRepository = reservationsRepository,
            propertiesRepository = mockk {
                coEvery { getCachedProperty("p-1") } returns mansion
                coEvery { getPropertyDetail("p-1", any(), any()) } returns Result.success(mansion)
            },
            initialCheckIn = initialCheckIn,
            initialCheckOut = initialCheckOut,
            initialGuests = guests,
        )

    @Test
    fun `computeSummary returns null until check-in and check-out are both set`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        assertNull(vm.computeSummary())
    }

    @Test
    fun `computeSummary returns null when check-out is on or before check-in`() = runTest {
        val vm = newViewModel(initialCheckIn = "2026-06-10", initialCheckOut = "2026-06-10")
        advanceUntilIdle()

        assertNull(vm.computeSummary())
    }

    @Test
    fun `computeSummary mirrors the canonical formula for 3 nights and 2 guests`() = runTest {
        val vm = newViewModel(initialCheckIn = "2026-05-03", initialCheckOut = "2026-05-06", guests = 2)
        advanceUntilIdle()

        val s = vm.computeSummary()

        assertNotNull(s)
        s!!
        assertEquals(3, s.nights)
        assertEquals(2, s.guests)
        assertEquals(7440.0, s.accommodation, 0.001)
        assertEquals(595.20, s.serviceFee, 0.001)
        assertEquals(120.0, s.cleaningFee, 0.001)
        // Backend rounds taxes per cent so we tolerate a 1c drift on the double total.
        assertEquals(1549.488, s.taxes, 0.01)
        assertEquals(9704.688, s.total, 0.01)
        assertEquals("COP", s.currency)
    }

    @Test
    fun `computeSummary scales accommodation linearly with guests`() = runTest {
        val solo = newViewModel(initialCheckIn = "2026-05-03", initialCheckOut = "2026-05-04", guests = 1)
        advanceUntilIdle()
        val pair = newViewModel(initialCheckIn = "2026-05-03", initialCheckOut = "2026-05-04", guests = 2)
        advanceUntilIdle()

        // Same dates, double the guests → double the accommodation.
        val soloAcc = solo.computeSummary()!!.accommodation
        val pairAcc = pair.computeSummary()!!.accommodation
        assertEquals(soloAcc * 2.0, pairAcc, 0.001)
    }

    @Test
    fun `computeSummary scales accommodation linearly with nights`() = runTest {
        val oneNight = newViewModel(initialCheckIn = "2026-05-03", initialCheckOut = "2026-05-04", guests = 1)
        advanceUntilIdle()
        val twoNights = newViewModel(initialCheckIn = "2026-05-03", initialCheckOut = "2026-05-05", guests = 1)
        advanceUntilIdle()

        val one = oneNight.computeSummary()!!.accommodation
        val two = twoNights.computeSummary()!!.accommodation
        assertEquals(one * 2.0, two, 0.001)
    }
}
