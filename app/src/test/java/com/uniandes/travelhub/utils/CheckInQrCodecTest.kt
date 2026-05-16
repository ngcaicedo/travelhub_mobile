package com.uniandes.travelhub.utils

import com.uniandes.travelhub.models.reservations.CheckInQrPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInQrCodecTest {

    @Test
    fun `encodeEncryptedPayload can be decrypted back for integrity`() {
        val payload = CheckInQrPayload(
            reservationId = "res-123",
            travelerId = "traveler-42",
            holderEmail = "ada@example.com",
            holderFullName = "Ada Lovelace",
            issuedAtEpochMs = 42L,
        )

        val encoded = CheckInQrCodec.encodeEncryptedPayload(payload)
        val decoded = CheckInQrCodec.decodeEncryptedPayloadForTest(encoded)

        assertTrue(encoded.startsWith("thci1."))
        assertEquals(payload, decoded)
    }
}
