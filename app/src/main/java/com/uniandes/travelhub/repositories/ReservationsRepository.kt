package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import com.uniandes.travelhub.models.reservations.CheckInQrArtifact
import com.uniandes.travelhub.models.reservations.CheckInQrPayload
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
import com.uniandes.travelhub.models.reservations.checkInFingerprint
import com.uniandes.travelhub.models.reservations.isCheckInEligible
import com.uniandes.travelhub.models.reservations.requiresCheckInInvalidation
import com.uniandes.travelhub.utils.CheckInQrCodec
import kotlinx.coroutines.flow.first
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
        val holderEmail = tokenStore.emailFlow.first().orEmpty()
        return runCatching {
            reservationsApi.listForUser(userId, group?.wire).also { reservations ->
                reservations.forEach { syncCheckInCacheFromSummary(it, userId, holderEmail) }
            }
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
        val userId = tokenStore.userIdFlow.first()
            ?: return Result.failure(ReservationException(message = null))
        val holderEmail = tokenStore.emailFlow.first().orEmpty()

        if (cached != null) {
            val refresh = runCatching { reservationsApi.getById(reservationId) }.getOrNull()
            if (refresh == null) {
                return Result.success(CheckInQrArtifact(cache = cached, isOffline = true))
            }
            if (refresh.requiresCheckInInvalidation(cached.reservationFingerprint)) {
                checkInQrCacheStore.remove(reservationId)
                return Result.success(
                    CheckInQrArtifact(
                        cache = cached,
                        isOffline = false,
                        requiresRefresh = true,
                    )
                )
            }
            val refreshedCache = cached.copy(
                reservationStatus = refresh.status,
                checkInDate = refresh.checkInDate,
                checkOutDate = refresh.checkOutDate,
                numberOfGuests = refresh.numberOfGuests,
                reservationFingerprint = refresh.checkInFingerprint(),
            )
            checkInQrCacheStore.put(refreshedCache)
            return Result.success(CheckInQrArtifact(cache = refreshedCache, isOffline = false))
        }

        val liveReservation = runCatching { reservationsApi.getById(reservationId) }.recoverFailure().getOrElse {
            return Result.failure(it)
        }
        if (!liveReservation.isCheckInEligible()) {
            return Result.failure(ReservationException("La reserva no tiene check-in disponible"))
        }
        val created = CachedCheckInQr(
            reservationId = liveReservation.id,
            reservationStatus = liveReservation.status,
            propertyName = null,
            propertyCoverImageUrl = null,
            checkInDate = liveReservation.checkInDate,
            checkOutDate = liveReservation.checkOutDate,
            numberOfGuests = liveReservation.numberOfGuests,
            reservationFingerprint = liveReservation.checkInFingerprint(),
            encryptedPayload = buildEncryptedPayload(
                reservationId = liveReservation.id,
                travelerId = userId,
                holderEmail = holderEmail,
            ),
            cachedAtEpochMs = System.currentTimeMillis(),
            holderEmail = holderEmail,
            travelerId = userId,
        )
        checkInQrCacheStore.put(created)
        return Result.success(CheckInQrArtifact(cache = created, isOffline = false))
    }

    private suspend fun syncCheckInCacheFromSummary(
        summary: ReservationWithDetailsResponse,
        travelerId: String,
        holderEmail: String,
    ) {
        val reservation = summary.reservation
        if (!reservation.isCheckInEligible()) {
            checkInQrCacheStore.remove(summary.id)
            return
        }
        val existing = checkInQrCacheStore.get(summary.id)
        val fingerprint = reservation.checkInFingerprint()
        val encryptedPayload = existing?.encryptedPayload ?: buildEncryptedPayload(
            reservationId = summary.id,
            travelerId = travelerId,
            holderEmail = holderEmail,
        )
        checkInQrCacheStore.put(
            CachedCheckInQr(
                reservationId = summary.id,
                reservationStatus = reservation.status,
                propertyName = summary.propertyName,
                propertyCoverImageUrl = summary.propertyCoverImageUrl,
                checkInDate = reservation.checkInDate,
                checkOutDate = reservation.checkOutDate,
                numberOfGuests = reservation.numberOfGuests,
                reservationFingerprint = fingerprint,
                encryptedPayload = encryptedPayload,
                cachedAtEpochMs = existing?.cachedAtEpochMs ?: System.currentTimeMillis(),
                holderEmail = existing?.holderEmail?.ifBlank { holderEmail } ?: holderEmail,
                travelerId = existing?.travelerId?.ifBlank { travelerId } ?: travelerId,
            )
        )
    }

    private fun buildEncryptedPayload(
        reservationId: String,
        travelerId: String,
        holderEmail: String,
    ): String = CheckInQrCodec.encodeEncryptedPayload(
        CheckInQrPayload(
            reservationId = reservationId,
            travelerId = travelerId,
            holderEmail = holderEmail,
            issuedAtEpochMs = System.currentTimeMillis(),
        )
    )
}
