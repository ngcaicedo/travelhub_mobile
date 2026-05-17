package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.payments.ChargeResponse
import com.uniandes.travelhub.models.payments.CreateChargeRequest
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.payments.PaymentsConfig
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.PaymentsApi
import kotlinx.coroutines.flow.first

class PaymentException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

class PaymentsRepository(
    private val paymentsApi: PaymentsApi,
    private val tokenStore: AuthTokenStore,
    private val parseDetail: (Throwable) -> String? = ApiErrorParser::parseBackendDetail,
) {

    suspend fun getConfig(): Result<PaymentsConfig> = runCatching {
        paymentsApi.getConfig()
    }.recoverFailure()

    suspend fun charge(
        reservationId: String,
        amountInCents: Long,
        currency: String,
        paymentMethodToken: String,
        idempotencyKey: String,
    ): Result<ChargeResponse> {
        val travelerId = tokenStore.userIdFlow.first()
            ?: return Result.failure(PaymentException(message = null))
        return runCatching {
            paymentsApi.charge(
                CreateChargeRequest(
                    reservationId = reservationId,
                    travelerId = travelerId,
                    paymentMethodToken = paymentMethodToken,
                    amountInCents = amountInCents,
                    currency = currency,
                    idempotencyKey = idempotencyKey,
                )
            )
        }.recoverFailure()
    }

    suspend fun getConfirmation(paymentId: String): Result<PaymentConfirmationSummary> = runCatching {
        paymentsApi.getConfirmationSummary(paymentId)
    }.recoverFailure()

    private fun <T> Result<T>.recoverFailure(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { Result.failure(PaymentException(parseDetail(it), it)) }
    )
}
