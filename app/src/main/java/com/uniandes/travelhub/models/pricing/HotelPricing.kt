package com.uniandes.travelhub.models.pricing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HotelPricingTargetOption(
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "property_name") val propertyName: String,
    @Json(name = "room_type_id") val roomTypeId: String,
    @Json(name = "room_type_name") val roomTypeName: String,
    @Json(name = "rate_plan_id") val ratePlanId: String,
    @Json(name = "rate_plan_name") val ratePlanName: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "base_price") val basePrice: Double,
) {
    val displayName: String
        get() = "$propertyName · $roomTypeName · $ratePlanName"
}

enum class HotelDiscountType(val wire: String) {
    PERCENTAGE("percentage"),
    FIXED("fixed"),
}

@JsonClass(generateAdapter = true)
data class HotelPricingPreviewRequest(
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "rate_plan_id") val ratePlanId: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "proposed_base_price") val proposedBasePrice: Double? = null,
    @Json(name = "discount_type") val discountType: String? = null,
    @Json(name = "discount_value") val discountValue: Double? = null,
    @Json(name = "rule_name") val ruleName: String? = null,
)

@JsonClass(generateAdapter = true)
data class HotelPricingPreviewResponse(
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "property_name") val propertyName: String,
    @Json(name = "room_type_id") val roomTypeId: String,
    @Json(name = "room_type_name") val roomTypeName: String,
    @Json(name = "rate_plan_id") val ratePlanId: String,
    @Json(name = "rate_plan_name") val ratePlanName: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "days_affected") val daysAffected: Int,
    @Json(name = "current_base_price") val currentBasePrice: Double,
    @Json(name = "proposed_base_price") val proposedBasePrice: Double,
    @Json(name = "discount_type") val discountType: String? = null,
    @Json(name = "discount_value") val discountValue: Double? = null,
    @Json(name = "final_price") val finalPrice: Double,
    @Json(name = "projected_revenue_before") val projectedRevenueBefore: Double,
    @Json(name = "projected_revenue_after") val projectedRevenueAfter: Double,
    @Json(name = "projected_revenue_delta") val projectedRevenueDelta: Double,
    @Json(name = "sellable_units") val sellableUnits: Int,
    @Json(name = "requires_confirmation") val requiresConfirmation: Boolean,
    @Json(name = "impact_summary") val impactSummary: String,
)

@JsonClass(generateAdapter = true)
data class HotelPricingApplyRequest(
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "rate_plan_id") val ratePlanId: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "proposed_base_price") val proposedBasePrice: Double? = null,
    @Json(name = "discount_type") val discountType: String? = null,
    @Json(name = "discount_value") val discountValue: Double? = null,
    @Json(name = "rule_name") val ruleName: String? = null,
    @Json(name = "confirmation_acknowledged") val confirmationAcknowledged: Boolean,
    @Json(name = "device_label") val deviceLabel: String? = null,
    @Json(name = "device_platform") val devicePlatform: String? = null,
)

@JsonClass(generateAdapter = true)
data class HotelPricingHistoryItem(
    @Json(name = "id") val id: String,
    @Json(name = "property_id") val propertyId: String,
    @Json(name = "property_name") val propertyName: String,
    @Json(name = "room_type_name") val roomTypeName: String,
    @Json(name = "rate_plan_name") val ratePlanName: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "rule_name") val ruleName: String? = null,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    @Json(name = "previous_base_price") val previousBasePrice: Double,
    @Json(name = "new_base_price") val newBasePrice: Double,
    @Json(name = "discount_type") val discountType: String? = null,
    @Json(name = "discount_value") val discountValue: Double? = null,
    @Json(name = "final_price") val finalPrice: Double,
    @Json(name = "projected_revenue_before") val projectedRevenueBefore: Double,
    @Json(name = "projected_revenue_after") val projectedRevenueAfter: Double,
    @Json(name = "actor_user_id") val actorUserId: String,
    @Json(name = "actor_email") val actorEmail: String,
    @Json(name = "device_label") val deviceLabel: String? = null,
    @Json(name = "device_platform") val devicePlatform: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "reverted_at") val revertedAt: String? = null,
    @Json(name = "can_revert") val canRevert: Boolean,
)

@JsonClass(generateAdapter = true)
data class HotelPricingApplyResponse(
    @Json(name = "preview") val preview: HotelPricingPreviewResponse,
    @Json(name = "history_entry") val historyEntry: HotelPricingHistoryItem,
)

@JsonClass(generateAdapter = true)
data class HotelPricingRevertResponse(
    @Json(name = "reverted_change_id") val revertedChangeId: String,
    @Json(name = "reverted_at") val revertedAt: String,
)
