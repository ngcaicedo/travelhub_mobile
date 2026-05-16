package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.SeasonalPricingPatchPayload
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingWritePayload
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.PropertiesApi
import retrofit2.HttpException

/** Distinguishable failure for HTTP 423 so the UI can show the locked-state copy. */
class SeasonalPricingLockedException(message: String? = null) : Exception(message)

class SeasonalPricingRepository(
    private val api: PropertiesApi,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun list(propertyId: String): Result<List<SeasonalPricingResponse>> = runCatching {
        api.listSeasonalPricing(propertyId).items
    }.recoverFailure()

    suspend fun create(
        propertyId: String,
        payload: SeasonalPricingWritePayload,
    ): Result<SeasonalPricingResponse> = runCatching {
        api.createSeasonalPricing(propertyId, payload)
    }.recoverFailure()

    suspend fun update(
        propertyId: String,
        seasonalPriceId: String,
        payload: SeasonalPricingPatchPayload,
    ): Result<SeasonalPricingResponse> = runCatching {
        api.updateSeasonalPricing(propertyId, seasonalPriceId, payload)
    }.recoverFailure()

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            if (throwable is HttpException && throwable.code() == 423) {
                Result.failure(
                    SeasonalPricingLockedException(parseDetail(throwable))
                )
            } else {
                Result.failure(Exception(parseDetail(throwable), throwable))
            }
        },
    )
}
