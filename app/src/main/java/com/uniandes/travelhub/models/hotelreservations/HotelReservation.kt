package com.uniandes.travelhub.models.hotelreservations

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uniandes.travelhub.models.reservations.ReservationResponse

@JsonClass(generateAdapter = true)
data class HotelReservationPropertyOption(
    val propertyId: String,
    val propertyName: String,
)

@JsonClass(generateAdapter = true)
data class HotelAvailableAction(
    @Json(name = "action") val action: String,
    @Json(name = "label") val label: String,
)

@JsonClass(generateAdapter = true)
data class HotelReservationListItem(
    @Json(name = "id") val id: String,
    @Json(name = "reservation_number") val reservationNumber: String,
    @Json(name = "id_property") val idProperty: String,
    @Json(name = "id_room") val idRoom: String,
    @Json(name = "id_traveler") val idTraveler: String,
    @Json(name = "guest_full_name") val guestFullName: String? = null,
    @Json(name = "room_type") val roomType: String? = null,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
    @Json(name = "number_of_guests") val numberOfGuests: Int,
    @Json(name = "total_price") val totalPrice: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "available_actions") val availableActions: List<HotelAvailableAction> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class HotelReservationsPage(
    @Json(name = "items") val items: List<HotelReservationListItem>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "page_size") val pageSize: Int,
)

@JsonClass(generateAdapter = true)
data class HotelGuestInfo(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
)

@JsonClass(generateAdapter = true)
data class HotelReservationChangeRecord(
    @Json(name = "id") val id: String,
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "action") val action: String,
    @Json(name = "previous_status") val previousStatus: String,
    @Json(name = "new_status") val newStatus: String,
    @Json(name = "reason") val reason: String,
    @Json(name = "actor_user_id") val actorUserId: String? = null,
    @Json(name = "source_ip") val sourceIp: String? = null,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class HotelInternalNoteResponse(
    @Json(name = "id") val id: String,
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "content") val content: String,
    @Json(name = "author_user_id") val authorUserId: String,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class HotelReservationDetailResponse(
    @Json(name = "reservation") val reservation: ReservationResponse,
    @Json(name = "guest") val guest: HotelGuestInfo? = null,
    @Json(name = "change_history") val changeHistory: List<HotelReservationChangeRecord> = emptyList(),
    @Json(name = "internal_notes") val internalNotes: List<HotelInternalNoteResponse> = emptyList(),
    @Json(name = "available_actions") val availableActions: List<HotelAvailableAction> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class HotelReservationActionResponse(
    @Json(name = "reservation") val reservation: ReservationResponse,
    @Json(name = "status_before") val statusBefore: String,
    @Json(name = "status_after") val statusAfter: String,
    @Json(name = "action_applied") val actionApplied: String,
    @Json(name = "reason") val reason: String,
    @Json(name = "refund_requested") val refundRequested: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class HotelReservationConfirmationRequest(
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "locale") val locale: String? = null,
)

enum class HotelReservationCancellationReason(val wire: String) {
    MAINTENANCE("maintenance"),
    OVERBOOKING("overbooking"),
    HOTEL_POLICY("hotel_policy"),
    OTHER("other"),
}

@JsonClass(generateAdapter = true)
data class HotelReservationCancellationRequest(
    @Json(name = "reason") val reason: String,
    @Json(name = "note") val note: String? = null,
    @Json(name = "locale") val locale: String? = null,
)

enum class HotelReservationStatusFilter(val wire: String?) {
    ALL(null),
    PENDING_PAYMENT("pending_payment"),
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    COMPLETED("completed"),
    MODIFICATION_PENDING_PAYMENT("modification_pending_payment"),
    MODIFICATION_CONFIRMED("modification_confirmed"),
}

fun List<HotelAvailableAction>.hasAction(action: String): Boolean = any { it.action == action }
