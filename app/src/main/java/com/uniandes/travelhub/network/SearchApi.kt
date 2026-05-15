package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.search.PropertyAvailabilityResponse
import com.uniandes.travelhub.models.search.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {

    @GET("api/v1/search")
    suspend fun search(
        @Query("city") city: String? = null,
        @Query("check_in") checkIn: String? = null,
        @Query("check_out") checkOut: String? = null,
        @Query("guests") guests: Int,
        @Query("amenities") amenities: List<String>? = null,
        @Query("min_price") minPrice: Int? = null,
        @Query("max_price") maxPrice: Int? = null,
        @Query("min_lat") minLat: Double? = null,
        @Query("max_lat") maxLat: Double? = null,
        @Query("min_lng") minLng: Double? = null,
        @Query("max_lng") maxLng: Double? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("order_dir") orderDir: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): SearchResponse

    @GET("api/v1/search/properties/{propertyId}/availability")
    suspend fun checkAvailability(
        @Path("propertyId") propertyId: String,
        @Query("check_in") checkIn: String,
        @Query("check_out") checkOut: String,
        @Query("guests") guests: Int,
    ): PropertyAvailabilityResponse
}
