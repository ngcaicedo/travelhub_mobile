package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.search.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("api/v1/search")
    suspend fun search(
        @Query("city") city: String,
        @Query("check_in") checkIn: String,
        @Query("check_out") checkOut: String,
        @Query("guests") guests: Int,
        @Query("amenities") amenities: List<String>? = null,
        @Query("min_price") minPrice: Int? = null,
        @Query("max_price") maxPrice: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("order_dir") orderDir: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): SearchResponse
}
