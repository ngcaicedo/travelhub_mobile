package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.hotelreservations.HotelReservationActionResponse
import com.uniandes.travelhub.models.hotelreservations.HotelReservationCancellationRequest
import com.uniandes.travelhub.models.hotelreservations.HotelReservationConfirmationRequest
import com.uniandes.travelhub.models.hotelreservations.HotelReservationDetailResponse
import com.uniandes.travelhub.models.hotelreservations.HotelReservationListItem
import com.uniandes.travelhub.models.hotelreservations.HotelReservationsPage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HotelReservationsApi {

    @GET("api/v1/reservations/host/me")
    suspend fun listHostReservations(
        @Query("status") statuses: List<String>? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
    ): HotelReservationsPage

    @GET("api/v1/hotel/reservations")
    suspend fun listReservations(
        @Query("propertyId") propertyId: String,
        @Query("status") status: String? = null,
    ): List<HotelReservationListItem>

    @GET("api/v1/hotel/reservations/{id}")
    suspend fun getReservationDetail(@Path("id") reservationId: String): HotelReservationDetailResponse

    @POST("api/v1/hotel/reservations/{id}/confirm")
    suspend fun confirmReservation(
        @Path("id") reservationId: String,
        @Body body: HotelReservationConfirmationRequest,
    ): HotelReservationActionResponse

    @POST("api/v1/hotel/reservations/{id}/cancel")
    suspend fun cancelReservation(
        @Path("id") reservationId: String,
        @Body body: HotelReservationCancellationRequest,
    ): HotelReservationActionResponse
}
