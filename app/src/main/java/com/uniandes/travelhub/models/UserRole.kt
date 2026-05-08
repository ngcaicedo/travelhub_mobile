package com.uniandes.travelhub.models

import com.squareup.moshi.Json


enum class UserRole {
    @Json(name = "traveler")
    TRAVELER,

    @Json(name = "hotel_partner")
    HOTEL_PARTNER,

    @Json(name = "admin")
    ADMIN;

    companion object {
        fun fromWire(value: String?): UserRole? = when (value?.trim()?.lowercase()) {
            "traveler" -> TRAVELER
            "hotel_partner", "hotel-partner", "hotel", "partner" -> HOTEL_PARTNER
            "admin" -> ADMIN
            else -> null
        }
    }
}
