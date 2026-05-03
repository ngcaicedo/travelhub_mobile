package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.payments.ChargeResponse
import com.uniandes.travelhub.models.payments.CreateChargeRequest
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.payments.PaymentsConfig
import com.uniandes.travelhub.network.ApiErrorParser
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.PaymentsApi
import kotlinx.coroutines.flow.first

class PaymentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class PaymentsRepository(
    private val paymentsApi: PaymentsApi,
    private val tokenStore: AuthTokenStore,
    private val errorParser: (Throwable, String) -> String = ApiErrorParser::getApiErrorMessage,
) {

    suspend fun getConfig(): Result<PaymentsConfig> = runCatching {
        paymentsApi.getConfig()
    }.recoverFailure("No fue posible cargar la configuración de pago")

    suspend fun charge(
        reservationId: String,
        amountInCents: Long,
        currency: String,
        paymentMethodToken: String,
        idempotencyKey: String,
    ): Result<ChargeResponse> {
        val travelerId = tokenStore.userIdFlow.first()
            ?: return Result.failure(PaymentException("Sesión inválida"))
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
        }.recoverFailure("No fue posible procesar el pago")
    }

    suspend fun getConfirmation(paymentId: String): Result<PaymentConfirmationSummary> = runCatching {
        paymentsApi.getConfirmationSummary(paymentId)
    }.recoverFailure("No fue posible cargar la confirmación")

    private fun <T> Result<T>.recoverFailure(fallback: String): Result<T> = fold(
        onSuccess = { this },
        onFailure = { throwable ->
            val message = errorParser(throwable, fallback)
            Result.failure(PaymentException(message, throwable))
        }
    )
}
