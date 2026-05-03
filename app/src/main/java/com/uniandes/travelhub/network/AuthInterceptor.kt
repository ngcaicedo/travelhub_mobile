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
 */
class AuthInterceptor(private val tokenStore: AuthTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
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
