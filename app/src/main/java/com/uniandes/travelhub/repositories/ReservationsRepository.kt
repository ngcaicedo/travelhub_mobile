package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationConfirmResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.models.reservations.ReservationStatusGroup
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import java.util.UUID
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.ReservationsApi
import kotlinx.coroutines.flow.first

class ReservationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ReservationsRepository(
    private val reservationsApi: ReservationsApi,
    private val tokenStore: AuthTokenStore,
    private val errorParser: (Throwable, String) -> String = ApiErrorParser::getApiErrorMessage,
) {

    suspend fun create(
        propertyId: String,
        checkIn: String,
        checkOut: String,
        guests: Int,
        currency: String,
        roomId: String? = null,
    ): Result<ReservationResponse> {
        val travelerId = tokenStore.userIdFlow.first()
            ?: return Result.failure(ReservationException("Sesión inválida"))
        // Backend requires id_room. The web passes id_room = id_property when
        // there is no separate "room" concept (single-unit properties), so we
        // mirror that default here.
        val effectiveRoomId = roomId?.takeIf { it.isNotBlank() } ?: propertyId
        return runCatching {
            reservationsApi.create(
                CreateReservationRequest(
                    idTraveler = travelerId,
                    idProperty = propertyId,
                    idRoom = effectiveRoomId,
                    checkInDate = checkIn,
                    checkOutDate = checkOut,
                    numberOfGuests = guests,
                    currency = currency,
                )
            )
        }.recoverFailure("No fue posible crear la reserva")
    }

    suspend fun getById(reservationId: String): Result<ReservationResponse> = runCatching {
        reservationsApi.getById(reservationId)
    }.recoverFailure("No fue posible cargar la reserva")

    suspend fun listForCurrentUser(group: ReservationStatusGroup? = null): Result<List<ReservationWithDetailsResponse>> {
        val userId = tokenStore.userIdFlow.first()
            ?: return Result.failure(ReservationException("Sesión inválida"))
        return runCatching {
            reservationsApi.listForUser(userId, group?.wire)
        }.recoverFailure("No fue posible cargar las reservas")
    }

    suspend fun previewModification(
        reservationId: String,
        checkIn: String,
        checkOut: String,
        guests: Int,
    ): Result<ReservationModificationPreviewResponse> = runCatching {
        reservationsApi.previewModification(
            reservationId,
            ReservationModificationPreviewRequest(checkIn, checkOut, guests),
        )
    }.recoverFailure("No fue posible calcular la modificación")

    suspend fun confirmModification(
        reservationId: String,
        checkIn: String,
        checkOut: String,
        guests: Int,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ReservationConfirmResponse> = runCatching {
        reservationsApi.confirmModification(
            reservationId,
            ReservationModificationConfirmRequest(checkIn, checkOut, guests, idempotencyKey),
        )
    }.recoverFailure("No fue posible modificar la reserva")

    suspend fun previewCancellation(
        reservationId: String,
    ): Result<ReservationCancellationPreviewResponse> = runCatching {
        reservationsApi.previewCancellation(reservationId)
    }.recoverFailure("No fue posible calcular la cancelación")

    suspend fun confirmCancellation(
        reservationId: String,
        reason: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ReservationConfirmResponse> = runCatching {
        reservationsApi.confirmCancellation(
            reservationId,
            ReservationCancellationConfirmRequest(idempotencyKey = idempotencyKey, reason = reason),
        )
    }.recoverFailure("No fue posible cancelar la reserva")

    private fun <T> Result<T>.recoverFailure(fallback: String): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            val message = errorParser(throwable, fallback)
            Result.failure(ReservationException(message, throwable))
        }
    )
}
