package com.uniandes.travelhub.utils

import com.uniandes.travelhub.models.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JwtUtilsTest {

    // ---- helpers ----

    /** Builds a minimal JWT (header.payload.signature) from a raw JSON payload. */
    private fun jwt(payloadJson: String): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString(
            """{"alg":"HS256","typ":"JWT"}""".toByteArray()
        )
        val payload = encoder.encodeToString(payloadJson.toByteArray())
        return "$header.$payload.fake-signature"
    }

    // ---- extractRoleFromToken ----

    @Test
    fun `extracts TRAVELER role from role claim`() {
        val token = jwt("""{"sub":"u1","role":"traveler","exp":9999999999}""")
        assertEquals(UserRole.TRAVELER, JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `extracts HOTEL_PARTNER role from role claim`() {
        val token = jwt("""{"sub":"u2","role":"hotel_partner","exp":9999999999}""")
        assertEquals(UserRole.HOTEL_PARTNER, JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `extracts HOTEL_PARTNER from alternative names`() {
        assertEquals(
            UserRole.HOTEL_PARTNER,
            JwtUtils.extractRoleFromToken(jwt("""{"role":"hotel-partner"}""")),
        )
        assertEquals(
            UserRole.HOTEL_PARTNER,
            JwtUtils.extractRoleFromToken(jwt("""{"role":"partner"}""")),
        )
    }

    @Test
    fun `extracts ADMIN role from role claim`() {
        val token = jwt("""{"sub":"u3","role":"admin","exp":9999999999}""")
        assertEquals(UserRole.ADMIN, JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `extracts role from roles array claim`() {
        val token = jwt("""{"roles":["traveler"],"exp":9999999999}""")
        assertEquals(UserRole.TRAVELER, JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `extracts role from roles string claim`() {
        val token = jwt("""{"roles":"hotel_partner","exp":9999999999}""")
        assertEquals(UserRole.HOTEL_PARTNER, JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `returns null when role claim is missing`() {
        val token = jwt("""{"sub":"u4","exp":9999999999}""")
        assertNull(JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `returns null for unknown role value`() {
        val token = jwt("""{"role":"superuser"}""")
        assertNull(JwtUtils.extractRoleFromToken(token))
    }

    @Test
    fun `returns null for malformed token`() {
        assertNull(JwtUtils.extractRoleFromToken("not-a-jwt"))
        assertNull(JwtUtils.extractRoleFromToken(""))
    }

    // ---- isTokenExpired ----

    @Test
    fun `token with future exp is not expired`() {
        val token = jwt("""{"exp":${System.currentTimeMillis() / 1000 + 3600}}""")
        assertFalse(JwtUtils.isTokenExpired(token))
    }

    @Test
    fun `token with past exp is expired`() {
        val token = jwt("""{"exp":1000000000}""")
        assertTrue(JwtUtils.isTokenExpired(token))
    }

    @Test
    fun `token without exp claim is not expired`() {
        val token = jwt("""{"sub":"u1"}""")
        assertFalse(JwtUtils.isTokenExpired(token))
    }

    @Test
    fun `malformed token is treated as expired`() {
        assertTrue(JwtUtils.isTokenExpired("garbage"))
        assertTrue(JwtUtils.isTokenExpired(""))
    }
}
