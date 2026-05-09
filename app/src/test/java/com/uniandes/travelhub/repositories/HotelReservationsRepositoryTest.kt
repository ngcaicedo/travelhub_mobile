package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.hotelreservations.HotelAvailableAction
import com.uniandes.travelhub.models.hotelreservations.HotelReservationListItem
import com.uniandes.travelhub.models.hotelreservations.HotelReservationsPage
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.network.HotelPricingApi
import com.uniandes.travelhub.network.HotelReservationsApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class HotelReservationsRepositoryTest {

    private val reservationsApi: HotelReservationsApi = mockk()
    private val pricingApi: HotelPricingApi = mockk()

    @Test
    fun `listProperties falls back to host reservations when pricing targets fail`() = runTest {
        coEvery { reservationsApi.listHostReservations(statuses = null, page = 1, pageSize = 100) } returns
            HotelReservationsPage(
                items = listOf(
                    reservation(propertyId = "p-1", roomType = "Hotel Riviera"),
                    reservation(propertyId = "p-2", roomType = "Ocean Suites"),
                ),
                total = 2,
                page = 1,
                pageSize = 100,
            )
        coEvery { pricingApi.listTargets() } throws http404()

        val result = HotelReservationsRepository(
            api = reservationsApi,
            pricingApi = pricingApi,
        ).listProperties()

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("Hotel Riviera", "Ocean Suites"),
            result.getOrNull()?.map { it.propertyName },
        )
    }

    @Test
    fun `listProperties prefers pricing names when available`() = runTest {
        coEvery { reservationsApi.listHostReservations(statuses = null, page = 1, pageSize = 100) } returns
            HotelReservationsPage(
                items = listOf(reservation(propertyId = "p-1", roomType = "Room name")),
                total = 1,
                page = 1,
                pageSize = 100,
            )
        coEvery { pricingApi.listTargets() } returns listOf(
            HotelPricingTargetOption(
                propertyId = "p-1",
                propertyName = "Hotel Riviera",
                roomTypeId = "rt-1",
                roomTypeName = "Suite",
                ratePlanId = "rp-1",
                ratePlanName = "Flexible",
                currency = "USD",
                basePrice = 240.0,
            )
        )

        val result = HotelReservationsRepository(
            api = reservationsApi,
            pricingApi = pricingApi,
        ).listProperties()

        assertTrue(result.isSuccess)
        assertEquals("Hotel Riviera", result.getOrNull()?.single()?.propertyName)
    }

    private fun reservation(
        propertyId: String,
        roomType: String,
    ) = HotelReservationListItem(
        id = "r-1",
        reservationNumber = "RES-1",
        idProperty = propertyId,
        idRoom = propertyId,
        idTraveler = "traveler-1",
        guestFullName = "Ana García",
        roomType = roomType,
        checkInDate = "2026-05-10T00:00:00Z",
        checkOutDate = "2026-05-12T00:00:00Z",
        numberOfGuests = 2,
        totalPrice = "100.00",
        currency = "USD",
        status = "pending_payment",
        createdAt = "2026-05-08T00:00:00Z",
        availableActions = listOf(
            HotelAvailableAction(action = "confirm", label = "Confirmar reserva"),
        ),
    )

    private fun http404(): HttpException =
        HttpException(
            Response.error<String>(
                404,
                """{"detail":"Not found"}""".toResponseBody("application/json".toMediaType()),
            ),
        )
}
