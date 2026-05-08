package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.pricing.HotelPricingApplyRequest
import com.uniandes.travelhub.models.pricing.HotelPricingApplyResponse
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewRequest
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingRevertResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.HotelPricingApi

class HotelPricingException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class HotelPricingRepository(
    private val api: HotelPricingApi,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun listTargets(): Result<List<HotelPricingTargetOption>> = runCatching {
        api.listTargets()
    }.recoverFailure()

    suspend fun preview(payload: HotelPricingPreviewRequest): Result<HotelPricingPreviewResponse> = runCatching {
        api.preview(payload)
    }.recoverFailure()

    suspend fun apply(payload: HotelPricingApplyRequest): Result<HotelPricingApplyResponse> = runCatching {
        api.apply(payload)
    }.recoverFailure()

    suspend fun listHistory(): Result<List<HotelPricingHistoryItem>> = runCatching {
        api.listHistory()
    }.recoverFailure()

    suspend fun revert(changeId: String): Result<HotelPricingRevertResponse> = runCatching {
        api.revert(changeId)
    }.recoverFailure()

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(HotelPricingException(parseDetail(it), it)) },
    )
}
