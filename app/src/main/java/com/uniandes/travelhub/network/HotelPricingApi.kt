package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.pricing.HotelPricingApplyRequest
import com.uniandes.travelhub.models.pricing.HotelPricingApplyResponse
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewRequest
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingRevertResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface HotelPricingApi {

    @GET("api/v1/inventory/hotel/pricing/targets")
    suspend fun listTargets(): List<HotelPricingTargetOption>

    @POST("api/v1/inventory/hotel/pricing/preview")
    suspend fun preview(
        @Header("X-Pricing-Checksum") checksum: String,
        @Body body: HotelPricingPreviewRequest,
    ): HotelPricingPreviewResponse

    @POST("api/v1/inventory/hotel/pricing/apply")
    suspend fun apply(
        @Header("X-Pricing-Checksum") checksum: String,
        @Body body: HotelPricingApplyRequest,
    ): HotelPricingApplyResponse

    @GET("api/v1/inventory/hotel/pricing/history")
    suspend fun listHistory(): List<HotelPricingHistoryItem>

    @POST("api/v1/inventory/hotel/pricing/history/{changeId}/revert")
    suspend fun revert(@Path("changeId") changeId: String): HotelPricingRevertResponse
}
