package com.uniandes.travelhub.network

import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.utils.JwtUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <token>` to every outgoing request when a token is present,
 * and `X-Traveler-Id: <sub>` for traveler-role requests so reservation/payment endpoints
 * receive the user id without each repository having to thread it through.
 *
 * The store is read through a `provider` lambda (not stored as a field) so the OkHttp
 * client can be built before [RetrofitFactory.init] runs — the interceptor simply reads
 * `null` and does nothing until a session is set up. Avoids the order-of-initialisation
 * trap where lazy fields evaluated `RetrofitFactory.*Api` before `tokenStore`, baking a
 * cached client without any auth headers for the entire process.
 */
class AuthInterceptor(
    private val tokenStoreProvider: () -> AuthTokenStore?,
) : Interceptor {

    constructor(tokenStore: AuthTokenStore) : this({ tokenStore })

    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenStore = tokenStoreProvider() ?: return chain.proceed(chain.request())
        val token = runBlocking { tokenStore.tokenFlow.first() }
        val role = runBlocking { tokenStore.roleFlow.first() }

        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        if (!token.isNullOrBlank() &&
            role == UserRole.TRAVELER &&
            originalRequest.header(HEADER_TRAVELER_ID) == null
        ) {
            JwtUtils.extractSubject(token)?.takeIf { it.isNotBlank() }?.let { sub ->
                builder.addHeader(HEADER_TRAVELER_ID, sub)
            }
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val HEADER_TRAVELER_ID = "X-Traveler-Id"
    }
}
