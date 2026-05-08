package com.uniandes.travelhub.utils

import com.uniandes.travelhub.models.UserRole
import java.nio.charset.StandardCharsets
import java.util.Base64

object JwtUtils {

    private val urlSafeDecoder: Base64.Decoder = Base64.getUrlDecoder()

    private fun decodePayload(token: String): String? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val padded = parts[1].padEnd(parts[1].length + (4 - parts[1].length % 4) % 4, '=')
        val bytes = runCatching { urlSafeDecoder.decode(padded) }.getOrNull() ?: return null
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun stringClaim(payload: String, name: String): String? {
        val regex = Regex("\"$name\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(payload)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    private fun longClaim(payload: String, name: String): Long? {
        val regex = Regex("\"$name\"\\s*:\\s*(-?\\d+)")
        return regex.find(payload)?.groupValues?.get(1)?.toLongOrNull()
    }

    fun extractRoleFromToken(token: String): UserRole? {
        val payload = decodePayload(token) ?: return null
        val role = stringClaim(payload, "role")
            ?: stringClaim(payload, "roles")
            ?: Regex("\"roles\"\\s*:\\s*\\[\\s*\"([^\"]*)\"")
                .find(payload)?.groupValues?.get(1)
        return role?.let(::mapToUserRole)
    }

    private fun mapToUserRole(role: String): UserRole? = when (role.lowercase()) {
        "traveler" -> UserRole.TRAVELER
        "hotel_partner", "hotel-partner", "hotel", "partner" -> UserRole.HOTEL_PARTNER
        "admin" -> UserRole.ADMIN
        else -> null
    }

    fun extractSubject(token: String): String? {
        val payload = decodePayload(token) ?: return null
        return stringClaim(payload, "sub")
    }

    fun isTokenExpired(token: String): Boolean {
        val payload = decodePayload(token) ?: return true
        val exp = longClaim(payload, "exp") ?: return false
        return System.currentTimeMillis() / 1000 >= exp
    }
}
