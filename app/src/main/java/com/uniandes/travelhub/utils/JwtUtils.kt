package com.uniandes.travelhub.utils

import android.util.Base64
import com.uniandes.travelhub.models.UserRole
import org.json.JSONObject
import java.nio.charset.Charset

object JwtUtils {

    /**
     * Decodes the payload of a JWT token and extracts the user role.
     * The role is expected to be in a claim named "role" or "roles".
     */
    fun extractRoleFromToken(token: String): UserRole? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payloadBase64 = parts[1]
            val payloadJson = String(
                Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charset.forName("UTF-8")
            )
            
            val jsonObject = JSONObject(payloadJson)
            
            // Adjust the key based on your backend implementation (e.g., "role", "role", "sub", etc.)
            val roleString = when {
                jsonObject.has("role") -> jsonObject.getString("role")
                jsonObject.has("roles") -> {
                    val roles = jsonObject.get("roles")
                    if (roles is String) roles else jsonObject.getJSONArray("roles").getString(0)
                }
                else -> null
            }

            roleString?.let { mapToUserRole(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapToUserRole(role: String): UserRole? {
        return when (role.lowercase()) {
            "traveler" -> UserRole.TRAVELER
            "hotel_partner", "hotel-partner", "partner" -> UserRole.HOTEL_PARTNER
            "admin" -> UserRole.ADMIN
            else -> null
        }
    }

    /**
     * Checks if the token is expired based on the "exp" claim.
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true

            val payloadJson = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            )
            val jsonObject = JSONObject(payloadJson)
            
            if (jsonObject.has("exp")) {
                val exp = jsonObject.getLong("exp")
                val currentTime = System.currentTimeMillis() / 1000
                return currentTime >= exp
            }
            false
        } catch (e: Exception) {
            true
        }
    }
}
