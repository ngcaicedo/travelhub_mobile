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

/**
 * Used by both `POST /reservations` (returns the slimmer `ReservationSummary`
 * shape on the backend — only id/status/total/currency/dates/hold/created)
 * and `GET /reservations/{id}` (returns the fuller shape with property/room
 * ids, guests, etc). Everything except the always-present fields is nullable.
 */
@JsonClass(generateAdapter = true)
data class ReservationResponse(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String,
    @Json(name = "total_price") val totalPrice: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "id_property") val idProperty: String? = null,
    @Json(name = "id_traveler") val idTraveler: String? = null,
    @Json(name = "id_room") val idRoom: String? = null,
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

@JsonClass(generateAdapter = true)
data class ReservationPolicySnapshot(
    @Json(name = "policy_type") val policyType: String,
    @Json(name = "minimum_notice_hours") val minimumNoticeHours: Int = 24,
    @Json(name = "penalty_percentage") val penaltyPercentage: String = "0",
    @Json(name = "timezone") val timezone: String = "UTC",
)

// --- Modify ---

@JsonClass(generateAdapter = true)
data class ReservationModificationPreviewRequest(
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int,
)

@JsonClass(generateAdapter = true)
data class ReservationModificationPreviewResponse(
    @Json(name = "delta_amount") val deltaAmount: String,
    @Json(name = "requires_additional_charge") val requiresAdditionalCharge: Boolean = false,
    @Json(name = "estimated_refund_amount") val estimatedRefundAmount: String? = null,
    @Json(name = "policy_applied") val policyApplied: ReservationPolicySnapshot? = null,
    @Json(name = "change_allowed") val changeAllowed: Boolean = false,
    @Json(name = "reasons") val reasons: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ReservationModificationConfirmRequest(
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int,
    @Json(name = "idempotency_key") val idempotencyKey: String,
)

// --- Cancel ---

@JsonClass(generateAdapter = true)
data class ReservationCancellationPreviewResponse(
    @Json(name = "refund_amount") val refundAmount: String,
    @Json(name = "penalty_amount") val penaltyAmount: String,
    @Json(name = "refund_type") val refundType: String,
    @Json(name = "eligible_until") val eligibleUntil: String? = null,
    @Json(name = "reservation_id") val reservationId: String? = null,
    @Json(name = "policy_applied") val policyApplied: ReservationPolicySnapshot? = null,
    @Json(name = "change_allowed") val changeAllowed: Boolean = false,
    @Json(name = "reasons") val reasons: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ReservationCancellationConfirmRequest(
    @Json(name = "idempotency_key") val idempotencyKey: String,
    @Json(name = "reason") val reason: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReservationConfirmResponse(
    @Json(name = "reservation") val reservation: ReservationResponse,
    @Json(name = "status_before") val statusBefore: String,
    @Json(name = "status_after") val statusAfter: String,
    @Json(name = "action_applied") val actionApplied: String,
    @Json(name = "additional_charge_amount") val additionalChargeAmount: String? = null,
    @Json(name = "refund_amount") val refundAmount: String? = null,
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
    const val MODIFICATION_PENDING_PAYMENT = "modification_pending_payment"
    const val MODIFICATION_CONFIRMED = "modification_confirmed"
}
