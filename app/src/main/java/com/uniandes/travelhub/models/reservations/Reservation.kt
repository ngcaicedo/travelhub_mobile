package com.uniandes.travelhub.models.reservations

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Input for `POST /api/v1/reservations`. Mirrors the web `ReservationRequest`.
 */
@JsonClass(generateAdapter = true)
data class CreateReservationRequest(
    @Json(name = "id_traveler") val idTraveler: String,
    @Json(name = "id_property") val idProperty: String,
    @Json(name = "id_room") val idRoom: String? = null,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int,
    @Json(name = "currency") val currency: String,
)

@JsonClass(generateAdapter = true)
data class ReservationResponse(
    @Json(name = "id") val id: String,
    @Json(name = "id_property") val idProperty: String,
    @Json(name = "status") val status: String,
    @Json(name = "total_price") val totalPrice: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int? = null,
    @Json(name = "hold_expires_at") val holdExpiresAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "price_breakdown") val priceBreakdown: ReservationPriceBreakdown? = null,
)

@JsonClass(generateAdapter = true)
data class ReservationPriceBreakdown(
    @Json(name = "accommodation_in_cents") val accommodationInCents: Long,
    @Json(name = "cleaning_fee_in_cents") val cleaningFeeInCents: Long,
    @Json(name = "service_fee_in_cents") val serviceFeeInCents: Long,
    @Json(name = "taxes_in_cents") val taxesInCents: Long,
    @Json(name = "total_in_cents") val totalInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "nights") val nights: Int,
    @Json(name = "nightly_rate_in_cents") val nightlyRateInCents: Long,
)

@JsonClass(generateAdapter = true)
data class ReservationWithDetailsResponse(
    @Json(name = "id") val id: String,
    @Json(name = "property_name") val propertyName: String? = null,
    @Json(name = "property_cover_image_url") val propertyCoverImageUrl: String? = null,
    @Json(name = "reservation") val reservation: ReservationResponse,
)

enum class ReservationStatusGroup(val wire: String) {
    ACTIVE("active"),
    PAST("past"),
    CANCELLED("cancelled"),
}

object ReservationStatus {
    const val PENDING_PAYMENT = "pending_payment"
    const val CONFIRMED = "confirmed"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
    const val REFUND_PENDING = "refund_pending"
    const val REFUND_COMPLETED = "refund_completed"
    const val REFUND_FAILED = "refund_failed"
}
