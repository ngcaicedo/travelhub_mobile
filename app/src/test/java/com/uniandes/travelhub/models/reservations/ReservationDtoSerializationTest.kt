package com.uniandes.travelhub.models.reservations

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test


class ReservationDtoSerializationTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `ReservationResponse parses the slim summary returned by POST reservations`() {
        val raw = """
            {
              "id": "29ae3ce0-50f0-4579-82f8-5bc56b9f000c",
              "status": "pending_payment",
              "total_price": "9704.69",
              "currency": "COP",
              "check_in_date": "2026-05-03T00:00:00Z",
              "check_out_date": "2026-05-06T00:00:00Z",
              "hold_expires_at": "2026-05-03T01:11:42.320835Z",
              "created_at": "2026-05-03T00:56:42.320835Z"
            }
        """.trimIndent()

        val parsed = moshi.adapter(ReservationResponse::class.java).fromJson(raw)

        assertNotNull(parsed)
        assertEquals("29ae3ce0-50f0-4579-82f8-5bc56b9f000c", parsed?.id)
        assertEquals("pending_payment", parsed?.status)
        assertEquals("9704.69", parsed?.totalPrice)
        assertNull(parsed?.idProperty)
        assertNull(parsed?.numberOfGuests)
    }

    @Test
    fun `ReservationCancellationPreviewResponse parses policy_applied as nested object`() {
        val raw = """
            {
              "reservation_id": "r-1",
              "refund_amount": "3330.10",
              "penalty_amount": "0.00",
              "refund_type": "full_refund",
              "eligible_until": "2026-05-15T00:00:00Z",
              "policy_applied": {
                "policy_type": "full_refund",
                "minimum_notice_hours": 48,
                "penalty_percentage": "0",
                "timezone": "UTC"
              },
              "change_allowed": true,
              "reasons": []
            }
        """.trimIndent()

        val parsed = moshi.adapter(ReservationCancellationPreviewResponse::class.java).fromJson(raw)

        assertNotNull(parsed)
        assertEquals("3330.10", parsed?.refundAmount)
        assertEquals("full_refund", parsed?.refundType)
        assertEquals("full_refund", parsed?.policyApplied?.policyType)
        assertEquals(48, parsed?.policyApplied?.minimumNoticeHours)
    }

    @Test
    fun `ReservationConfirmResponse parses status transition fields`() {
        val raw = """
            {
              "reservation": {
                "id": "r-1",
                "status": "modification_confirmed",
                "total_price": "1736.45",
                "currency": "COP",
                "check_in_date": "2026-05-17T00:00:00Z",
                "check_out_date": "2026-05-18T00:00:00Z"
              },
              "status_before": "confirmed",
              "status_after": "modification_confirmed",
              "action_applied": "modification_confirmed",
              "additional_charge_amount": "0.00",
              "refund_amount": "0.00"
            }
        """.trimIndent()

        val parsed = moshi.adapter(ReservationConfirmResponse::class.java).fromJson(raw)

        assertNotNull(parsed)
        assertEquals("modification_confirmed", parsed?.statusAfter)
        assertEquals("confirmed", parsed?.statusBefore)
        assertEquals("modification_confirmed", parsed?.reservation?.status)
    }

    @Test
    fun `ReservationModificationPreviewResponse tolerates missing optional fields`() {
        val raw = """
            {
              "delta_amount": "50.00",
              "change_allowed": true,
              "reasons": []
            }
        """.trimIndent()

        val parsed = moshi.adapter(ReservationModificationPreviewResponse::class.java).fromJson(raw)

        assertNotNull(parsed)
        assertEquals("50.00", parsed?.deltaAmount)
        assertEquals(true, parsed?.changeAllowed)
        assertNull(parsed?.policyApplied)
    }

    @Test
    fun `CreateReservationRequest serializes id_room when default mirrors id_property`() {
        val payload = CreateReservationRequest(
            idTraveler = "u-1",
            idProperty = "p-1",
            idRoom = "p-1",
            checkInDate = "2026-06-10",
            checkOutDate = "2026-06-12",
            numberOfGuests = 2,
            currency = "COP",
        )

        val json = moshi.adapter(CreateReservationRequest::class.java).toJson(payload)

        assertEquals(true, json.contains("\"id_room\":\"p-1\""))
        assertEquals(true, json.contains("\"id_property\":\"p-1\""))
        assertEquals(true, json.contains("\"id_traveler\":\"u-1\""))
    }
}
