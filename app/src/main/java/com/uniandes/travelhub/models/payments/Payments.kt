package com.uniandes.travelhub.models.payments

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentsConfig(
    @Json(name = "provider") val provider: String,
    @Json(name = "stripe_enabled") val stripeEnabled: Boolean,
    @Json(name = "publishable_key") val publishableKey: String,
)

@JsonClass(generateAdapter = true)
data class CreateChargeRequest(
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "traveler_id") val travelerId: String,
    @Json(name = "payment_method_token") val paymentMethodToken: String,
    @Json(name = "amount_in_cents") val amountInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "idempotency_key") val idempotencyKey: String,
)

@JsonClass(generateAdapter = true)
data class ChargeResponse(
    @Json(name = "payment_id") val paymentId: String,
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "status") val status: String,
    @Json(name = "amount_in_cents") val amountInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "gateway_charge_id") val gatewayChargeId: String? = null,
    @Json(name = "receipt_id") val receiptId: String? = null,
    @Json(name = "receipt_number") val receiptNumber: String? = null,
    @Json(name = "failure_reason") val failureReason: String? = null,
)

/**
 * Pre-canned tokens that the `fake_stripe` provider recognises. These match
 * the web `scenarioPresets` so QA and demos stay in sync across platforms.
 */
object FakePaymentScenarios {
    const val SUCCESS = "pm_tok_visa_ok"
    const val INSUFFICIENT_FUNDS = "pm_fail_insufficient_funds"
    const val CARD_DECLINED = "pm_fail_card_declined"
}

@JsonClass(generateAdapter = true)
data class CreateIntentRequest(
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "traveler_id") val travelerId: String,
    @Json(name = "amount_in_cents") val amountInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "property_name") val propertyName: String,
    @Json(name = "check_in_date") val checkInDate: String,
    @Json(name = "check_out_date") val checkOutDate: String,
)

/**
 * Response from `POST /payments/create-intent`. The web calls this a "checkout session";
 * it carries the publishable key and the `payment_transaction_id` used as the
 * idempotency anchor for `finalize` and the polling key for `/payments/checkout/{id}`.
 */
@JsonClass(generateAdapter = true)
data class CheckoutSession(
    @Json(name = "payment_transaction_id") val paymentTransactionId: String,
    @Json(name = "amount_in_cents") val amountInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "publishable_key") val publishableKey: String,
    @Json(name = "stripe_enabled") val stripeEnabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class FinalizePaymentRequest(
    @Json(name = "payment_transaction_id") val paymentTransactionId: String,
    @Json(name = "confirmation_token_id") val confirmationTokenId: String,
)

@JsonClass(generateAdapter = true)
data class FinalizePaymentResponse(
    @Json(name = "status") val status: String,
    @Json(name = "payment_id") val paymentId: String? = null,
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null,
    @Json(name = "client_secret") val clientSecret: String? = null,
    @Json(name = "error") val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class CheckoutSessionStatus(
    @Json(name = "payment_transaction_id") val paymentTransactionId: String,
    @Json(name = "status") val status: String,
    @Json(name = "payment_id") val paymentId: String? = null,
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentConfirmationSummary(
    @Json(name = "payment_id") val paymentId: String,
    @Json(name = "reservation_id") val reservationId: String,
    @Json(name = "traveler_id") val travelerId: String,
    @Json(name = "status") val status: String,
    @Json(name = "amount_in_cents") val amountInCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "receipt_id") val receiptId: String? = null,
    @Json(name = "receipt_number") val receiptNumber: String? = null,
    @Json(name = "property_name") val propertyName: String? = null,
    @Json(name = "property_address") val propertyAddress: String? = null,
    @Json(name = "check_in_date") val checkInDate: String? = null,
    @Json(name = "check_out_date") val checkOutDate: String? = null,
    @Json(name = "guests_count") val guestsCount: Int? = null,
    @Json(name = "nights") val nights: Int? = null,
    @Json(name = "nightly_rate_in_cents") val nightlyRateInCents: Long? = null,
    @Json(name = "taxes_in_cents") val taxesInCents: Long? = null,
    @Json(name = "total_in_cents") val totalInCents: Long? = null,
    @Json(name = "cancellation_policy") val cancellationPolicy: String? = null,
)

/**
 * Local high-level state used by the UI to render the payment progress.
 */
sealed interface PaymentResultState {
    data object Idle : PaymentResultState
    data object Processing : PaymentResultState
    data class Succeeded(val confirmation: PaymentConfirmationSummary) : PaymentResultState
    data class Failed(val reason: String) : PaymentResultState
    data object TimedOut : PaymentResultState
}
