package com.uniandes.travelhub.models.auth

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.uniandes.travelhub.models.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class AuthDtoSerializationTest {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(
        Map::class.java, String::class.java, Any::class.java
    )
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

    private fun toMap(json: String): Map<String, Any?> = mapAdapter.fromJson(json)!!

    @Test
    fun `RegisterRequest serializes traveler payload with snake_case keys and null hotel_name`() {
        val payload = RegisterRequest(
            email = "ada@example.com",
            phone = "+573001234567",
            password = "Sup3rSecret!",
            fullName = "Ada Lovelace",
            hotelName = null,
            role = UserRole.TRAVELER
        )

        val json = toMap(moshi.adapter(RegisterRequest::class.java).toJson(payload))

        assertEquals("ada@example.com", json["email"])
        assertEquals("+573001234567", json["phone"])
        assertEquals("Sup3rSecret!", json["password"])
        assertEquals("Ada Lovelace", json["full_name"])
        assertEquals("traveler", json["role"])
        assertFalse(json.containsKey("hotel_name"))
    }

    @Test
    fun `RegisterRequest serializes hotel partner payload including hotel_name`() {
        val payload = RegisterRequest(
            email = "front@hotelplaza.co",
            phone = "+573009998877",
            password = "An0therOne!",
            fullName = "Front Desk",
            hotelName = "Hotel Plaza",
            role = UserRole.HOTEL_PARTNER
        )

        val json = toMap(moshi.adapter(RegisterRequest::class.java).toJson(payload))

        assertEquals("Hotel Plaza", json["hotel_name"])
        assertEquals("hotel_partner", json["role"])
        assertEquals("Front Desk", json["full_name"])
    }

    @Test
    fun `VerifyOtpRequest serializes otpCode as otp_code`() {
        val payload = VerifyOtpRequest(email = "ada@example.com", otpCode = "123456")

        val json = toMap(moshi.adapter(VerifyOtpRequest::class.java).toJson(payload))

        assertEquals("ada@example.com", json["email"])
        assertEquals("123456", json["otp_code"])
    }

    @Test
    fun `TokenResponse parses access_token, token_type and role from snake_case JSON`() {
        val raw = """
            {
              "access_token": "eyJhbGciOiJIUzI1NiJ9.payload.sig",
              "token_type": "bearer",
              "role": "hotel_partner"
            }
        """.trimIndent()

        val parsed = moshi.adapter(TokenResponse::class.java).fromJson(raw)

        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", parsed?.accessToken)
        assertEquals("bearer", parsed?.tokenType)
        assertEquals("hotel_partner", parsed?.role)
    }

    @Test
    fun `TokenResponse also parses hotel alias role from backend`() {
        val raw = """
            {
              "access_token": "eyJhbGciOiJIUzI1NiJ9.payload.sig",
              "token_type": "bearer",
              "role": "hotel"
            }
        """.trimIndent()

        val parsed = moshi.adapter(TokenResponse::class.java).fromJson(raw)

        assertEquals("hotel", parsed?.role)
    }

    @Test
    fun `UserResponse parses full_name and treats missing hotel_name as null`() {
        val raw = """
            {
              "id": "u_42",
              "email": "ada@example.com",
              "phone": "+573001234567",
              "full_name": "Ada Lovelace",
              "status": 0
            }
        """.trimIndent()

        val parsed = moshi.adapter(UserResponse::class.java).fromJson(raw)

        assertEquals("Ada Lovelace", parsed?.fullName)
        assertNull(parsed?.hotelName)
        assertEquals(0, parsed?.status)
    }
}
