package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import com.uniandes.travelhub.models.reservations.CheckInQrArtifact
import com.uniandes.travelhub.models.reservations.CheckInQrResponse
import com.uniandes.travelhub.models.reservations.ReservationCancellationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationConfirmResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.models.reservations.ReservationStatusGroup
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.CheckInQrCacheStore
import com.uniandes.travelhub.network.ReservationsApi
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class ReservationException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class ReservationsRepository(
    private val reservationsApi: ReservationsApi,
    private val tokenStore: AuthTokenStore,
    private val checkInQrCacheStore: CheckInQrCacheStore,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
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
            ?: return Result.failure(ReservationException(message = null))
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
        }.recoverFailure()
    }

    suspend fun getById(reservationId: String): Result<ReservationResponse> = runCatching {
        reservationsApi.getById(reservationId)
    }.recoverFailure()

    suspend fun listForCurrentUser(group: ReservationStatusGroup? = null): Result<List<ReservationWithDetailsResponse>> {
        val userId = tokenStore.userIdFlow.first()
            ?: return Result.failure(ReservationException(message = null))
        return runCatching {
            reservationsApi.listForUser(userId, group?.wire)
        }.recoverFailure()
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
    }.recoverFailure()

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
    }.recoverFailure()

    suspend fun previewCancellation(
        reservationId: String,
    ): Result<ReservationCancellationPreviewResponse> = runCatching {
        reservationsApi.previewCancellation(reservationId)
    }.recoverFailure()

    suspend fun confirmCancellation(
        reservationId: String,
        reason: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): Result<ReservationConfirmResponse> = runCatching {
        reservationsApi.confirmCancellation(
            reservationId,
            ReservationCancellationConfirmRequest(idempotencyKey = idempotencyKey, reason = reason),
        )
    }.recoverFailure()

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(ReservationException(parseDetail(it), it)) }
    )

    suspend fun getCheckInQr(reservationId: String): Result<CheckInQrArtifact> {
        val cached = checkInQrCacheStore.get(reservationId)

        if (cached != null) {
            val refreshResult = runCatching { reservationsApi.getCheckInQr(reservationId) }
            val refresh = refreshResult.getOrNull()
            if (refresh == null) {
                val refreshError = refreshResult.exceptionOrNull()
                if (refreshError is HttpException && refreshError.code() == 409) {
                    checkInQrCacheStore.remove(reservationId)
                    return Result.success(
                        CheckInQrArtifact(
                            cache = cached,
                            isOffline = false,
                            requiresRefresh = true,
                        )
                    )
                }
                if (refreshError is IOException) {
                    return Result.success(CheckInQrArtifact(cache = cached, isOffline = true))
                }
                return Result.success(CheckInQrArtifact(cache = cached, isOffline = true))
            }
            if (refresh.reservationFingerprint != cached.reservationFingerprint) {
                checkInQrCacheStore.remove(reservationId)
                return Result.success(
                    CheckInQrArtifact(
                        cache = cached,
                        isOffline = false,
                        requiresRefresh = true,
                    )
                )
            }
            val refreshedCache = refresh.toCachedCheckInQr(cached.cachedAtEpochMs)
            checkInQrCacheStore.put(refreshedCache)
            return Result.success(CheckInQrArtifact(cache = refreshedCache, isOffline = false))
        }

        val liveReservation = runCatching { reservationsApi.getCheckInQr(reservationId) }.recoverFailure().getOrElse {
            return Result.failure(it)
        }
        val created = liveReservation.toCachedCheckInQr()
        checkInQrCacheStore.put(created)
        return Result.success(CheckInQrArtifact(cache = created, isOffline = false))
    }

    private fun CheckInQrResponse.toCachedCheckInQr(
        cachedAtEpochMs: Long = this.issuedAtEpochMs,
    ): CachedCheckInQr = CachedCheckInQr(
        reservationId = reservationId,
        reservationStatus = reservationStatus,
        propertyName = propertyName,
        propertyCoverImageUrl = propertyCoverImageUrl,
        checkInDate = checkInDate,
        checkOutDate = checkOutDate,
        numberOfGuests = numberOfGuests,
        reservationFingerprint = reservationFingerprint,
        encryptedPayload = encryptedPayload,
        cachedAtEpochMs = cachedAtEpochMs,
        holderEmail = holderEmail,
        travelerId = travelerId,
    )
}
