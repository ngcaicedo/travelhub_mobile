package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingListResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingPatchPayload
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingWritePayload
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PropertiesApi {

    @GET("api/v1/properties")
    suspend fun getProperties(): List<Property>

    @GET("api/v1/properties")
    suspend fun getPropertiesByOwner(@Query("owner_id") ownerId: String): List<Property>

    @GET("api/v1/properties/{id}")
    suspend fun getPropertyDetail(
        @Path("id") id: String,
        @Query("check_in") checkIn: String? = null,
        @Query("check_out") checkOut: String? = null,
    ): Property

    @GET("api/v1/properties/{id}/seasonal-pricing")
    suspend fun listSeasonalPricing(@Path("id") propertyId: String): SeasonalPricingListResponse

    @POST("api/v1/properties/{id}/seasonal-pricing")
    suspend fun createSeasonalPricing(
        @Path("id") propertyId: String,
        @Body body: SeasonalPricingWritePayload,
    ): SeasonalPricingResponse

    @PATCH("api/v1/properties/{id}/seasonal-pricing/{spId}")
    suspend fun updateSeasonalPricing(
        @Path("id") propertyId: String,
        @Path("spId") seasonalPriceId: String,
        @Body body: SeasonalPricingPatchPayload,
    ): SeasonalPricingResponse
}
