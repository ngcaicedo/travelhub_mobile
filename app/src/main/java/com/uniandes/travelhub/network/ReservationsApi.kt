package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.reservations.CreateReservationRequest
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

    @GET("api/v1/reservations/users/{userId}")
    suspend fun listForUser(
        @Path("userId") userId: String,
        @Query("status_group") statusGroup: String? = null,
    ): List<ReservationWithDetailsResponse>
}
