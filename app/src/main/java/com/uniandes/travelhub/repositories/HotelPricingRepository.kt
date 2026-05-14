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
import java.security.MessageDigest

class HotelPricingException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class HotelPricingRepository(
    private val api: HotelPricingApi,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun listTargets(): Result<List<HotelPricingTargetOption>> = runCatching {
        api.listTargets()
    }.recoverFailure()

    suspend fun preview(payload: HotelPricingPreviewRequest): Result<HotelPricingPreviewResponse> = runCatching {
        api.preview(payload.checksum(), payload)
    }.recoverFailure()

    suspend fun apply(payload: HotelPricingApplyRequest): Result<HotelPricingApplyResponse> = runCatching {
        api.apply(payload.checksum(), payload)
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

private fun HotelPricingPreviewRequest.checksum(): String = sha256Hex(
    canonicalJson(
        linkedMapOf(
            "property_id" to propertyId,
            "rate_plan_id" to ratePlanId,
            "start_date" to startDate,
            "end_date" to endDate,
            "proposed_base_price" to proposedBasePrice,
            "discount_type" to discountType,
            "discount_value" to discountValue,
            "rule_name" to ruleName,
        )
    )
)

private fun HotelPricingApplyRequest.checksum(): String = sha256Hex(
    canonicalJson(
        linkedMapOf(
            "property_id" to propertyId,
            "rate_plan_id" to ratePlanId,
            "start_date" to startDate,
            "end_date" to endDate,
            "proposed_base_price" to proposedBasePrice,
            "discount_type" to discountType,
            "discount_value" to discountValue,
            "rule_name" to ruleName,
            "confirmation_acknowledged" to confirmationAcknowledged,
            "device_label" to deviceLabel,
            "device_platform" to devicePlatform,
        )
    )
)

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun canonicalJson(input: Map<String, Any?>): String =
    input
        .filterValues { it != null }
        .toSortedMap()
        .entries
        .joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            "\"${escapeJson(key)}\":${jsonValue(value)}"
        }

private fun jsonValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> "\"${escapeJson(value)}\""
    is Boolean -> value.toString()
    is Int, is Long -> value.toString()
    is Double -> "\"${java.math.BigDecimal.valueOf(value).toPlainString()}\""
    is Float -> "\"${java.math.BigDecimal.valueOf(value.toDouble()).toPlainString()}\""
    else -> "\"${escapeJson(value.toString())}\""
}

private fun escapeJson(value: String): String = buildString {
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}
