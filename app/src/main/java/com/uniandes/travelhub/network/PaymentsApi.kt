package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.payments.ChargeResponse
import com.uniandes.travelhub.models.payments.CheckoutSession
import com.uniandes.travelhub.models.payments.CheckoutSessionStatus
import com.uniandes.travelhub.models.payments.CreateChargeRequest
import com.uniandes.travelhub.models.payments.CreateIntentRequest
import com.uniandes.travelhub.models.payments.FinalizePaymentRequest
import com.uniandes.travelhub.models.payments.FinalizePaymentResponse
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.payments.PaymentsConfig
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentsApi {

    @GET("api/v1/payments/config")
    suspend fun getConfig(): PaymentsConfig

    @POST("api/v1/payments/charges")
    suspend fun charge(@Body body: CreateChargeRequest): ChargeResponse

    @POST("api/v1/payments/create-intent")
    suspend fun createIntent(@Body body: CreateIntentRequest): CheckoutSession

    @POST("api/v1/payments/finalize")
    suspend fun finalize(@Body body: FinalizePaymentRequest): FinalizePaymentResponse

    @GET("api/v1/payments/checkout/{transactionId}")
    suspend fun getCheckoutStatus(@Path("transactionId") transactionId: String): CheckoutSessionStatus

    @GET("api/v1/payments/{paymentId}/confirmation")
    suspend fun getConfirmationSummary(@Path("paymentId") paymentId: String): PaymentConfirmationSummary
}
