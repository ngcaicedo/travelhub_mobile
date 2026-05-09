package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.hotelreservations.HotelReservationActionResponse
import com.uniandes.travelhub.models.hotelreservations.HotelReservationCancellationReason
import com.uniandes.travelhub.models.hotelreservations.HotelReservationCancellationRequest
import com.uniandes.travelhub.models.hotelreservations.HotelReservationConfirmationRequest
import com.uniandes.travelhub.models.hotelreservations.HotelReservationDetailResponse
import com.uniandes.travelhub.models.hotelreservations.HotelReservationListItem
import com.uniandes.travelhub.models.hotelreservations.HotelReservationPropertyOption
import com.uniandes.travelhub.models.hotelreservations.HotelReservationStatusFilter
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.HotelPricingApi
import com.uniandes.travelhub.network.HotelReservationsApi

class HotelReservationsException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class HotelReservationsRepository(
    private val api: HotelReservationsApi,
    private val pricingApi: HotelPricingApi,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun listProperties(): Result<List<HotelReservationPropertyOption>> = runCatching {
        val hostItems = api.listHostReservations(pageSize = 100).items
        val targetNames = runCatching {
            pricingApi.listTargets().associate { it.propertyId to it.propertyName }
        }.getOrDefault(emptyMap())

        val hostOptions = hostItems
            .groupBy { it.idProperty }
            .map { (propertyId, reservations) ->
                val roomName = reservations
                    .firstNotNullOfOrNull { it.roomType?.trim()?.takeIf(String::isNotBlank) }
                HotelReservationPropertyOption(
                    propertyId = propertyId,
                    propertyName = targetNames[propertyId]
                        ?: roomName
                        ?: fallbackPropertyName(propertyId),
                )
            }

        val fallbackOptions = targetNames
            .map { (propertyId, propertyName) ->
                HotelReservationPropertyOption(propertyId = propertyId, propertyName = propertyName)
            }

        (hostOptions.ifEmpty { fallbackOptions })
            .distinctBy { it.propertyId }
            .sortedBy { it.propertyName.lowercase() }
    }.recoverFailure()

    suspend fun listReservations(
        propertyId: String,
        status: HotelReservationStatusFilter = HotelReservationStatusFilter.ALL,
    ): Result<List<HotelReservationListItem>> = runCatching {
        api.listHostReservations(
            statuses = status.wire?.let(::listOf),
            page = 1,
            pageSize = 100,
        ).items.filter { reservation ->
            propertyId.isBlank() || reservation.idProperty == propertyId
        }
    }.recoverFailure()

    suspend fun getReservationDetail(reservationId: String): Result<HotelReservationDetailResponse> = runCatching {
        api.getReservationDetail(reservationId)
    }.recoverFailure()

    suspend fun confirmReservation(
        reservationId: String,
        locale: String? = null,
        reason: String = "mobile_hotel_confirmation",
    ): Result<HotelReservationActionResponse> = runCatching {
        api.confirmReservation(
            reservationId = reservationId,
            body = HotelReservationConfirmationRequest(reason = reason, locale = locale),
        )
    }.recoverFailure()

    suspend fun cancelReservation(
        reservationId: String,
        reason: HotelReservationCancellationReason,
        note: String? = null,
        locale: String? = null,
    ): Result<HotelReservationActionResponse> = runCatching {
        api.cancelReservation(
            reservationId = reservationId,
            body = HotelReservationCancellationRequest(
                reason = reason.wire,
                note = note?.trim()?.ifBlank { null },
                locale = locale,
            ),
        )
    }.recoverFailure()

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(HotelReservationsException(parseDetail(it), it)) },
    )

    private fun fallbackPropertyName(propertyId: String): String =
        "P-${propertyId.takeLast(6).uppercase()}"
}
