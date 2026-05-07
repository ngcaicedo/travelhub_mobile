package com.uniandes.travelhub.models.reservations

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckInQrPayload(
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "traveler_id") val travelerId: String,
    @Json(name = "holder_email") val holderEmail: String,
    @Json(name = "issued_at_epoch_ms") val issuedAtEpochMs: Long,
)

@JsonClass(generateAdapter = true)
data class CachedCheckInQr(
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "reservation_status") val reservationStatus: String,
    @Json(name = "property_name") val propertyName: String? = null,
    @Json(name = "property_cover_image_url") val propertyCoverImageUrl: String? = null,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int? = null,
    @Json(name = "reservation_fingerprint") val reservationFingerprint: String,
    @Json(name = "encrypted_payload") val encryptedPayload: String,
    @Json(name = "cached_at_epoch_ms") val cachedAtEpochMs: Long,
    @Json(name = "holder_email") val holderEmail: String,
    @Json(name = "traveler_id") val travelerId: String,
)

data class CheckInQrArtifact(
    val cache: CachedCheckInQr,
    val isOffline: Boolean,
    val requiresRefresh: Boolean = false,
)

fun ReservationResponse.isCheckInEligible(): Boolean =
    status == ReservationStatus.CONFIRMED || status == ReservationStatus.MODIFICATION_CONFIRMED

fun ReservationResponse.requiresCheckInInvalidation(previousFingerprint: String): Boolean =
    !isCheckInEligible() || checkInFingerprint() != previousFingerprint

fun ReservationResponse.checkInFingerprint(): String = listOf(
    status,
    checkInDate.take(10),
    checkOutDate.take(10),
    numberOfGuests?.toString().orEmpty(),
).joinToString(separator = "|")
