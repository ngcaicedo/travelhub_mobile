package com.uniandes.travelhub.models

import com.squareup.moshi.Json


enum class UserRole {
    @Json(name = "traveler")
    TRAVELER,

    @Json(name = "hotel_partner")
    HOTEL_PARTNER
}
