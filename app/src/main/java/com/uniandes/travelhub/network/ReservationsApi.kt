package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationConfirmResponse
import com.uniandes.travelhub.models.reservations.CheckInQrResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationConfirmRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewRequest
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationsApi {

    @POST("api/v1/reservations")
    suspend fun create(@Body body: CreateReservationRequest): ReservationResponse

    @GET("api/v1/reservations/{id}")
    suspend fun getById(@Path("id") reservationId: String): ReservationResponse

    @GET("api/v1/reservations/{id}/checkin-qr")
    suspend fun getCheckInQr(@Path("id") reservationId: String): CheckInQrResponse

    @GET("api/v1/reservations/users/{userId}")
    suspend fun listForUser(
        @Path("userId") userId: String,
        @Query("status_group") statusGroup: String? = null,
    ): List<ReservationWithDetailsResponse>

    @POST("api/v1/reservations/{id}/modifications/preview")
    suspend fun previewModification(
        @Path("id") reservationId: String,
        @Body body: ReservationModificationPreviewRequest,
    ): ReservationModificationPreviewResponse

    @POST("api/v1/reservations/{id}/modifications/confirm")
    suspend fun confirmModification(
        @Path("id") reservationId: String,
        @Body body: ReservationModificationConfirmRequest,
    ): ReservationConfirmResponse

    @POST("api/v1/reservations/{id}/cancellation/preview")
    suspend fun previewCancellation(
        @Path("id") reservationId: String,
    ): ReservationCancellationPreviewResponse

    @POST("api/v1/reservations/{id}/cancellation/confirm")
    suspend fun confirmCancellation(
        @Path("id") reservationId: String,
        @Body body: ReservationCancellationConfirmRequest,
    ): ReservationConfirmResponse
}
