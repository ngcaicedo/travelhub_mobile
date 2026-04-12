package com.uniandes.travelhub.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that adds the JWT token to the Authorization header of every request
 * and handles authentication errors (401, 403).
 */
class AuthInterceptor(private val tokenStore: AuthTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            tokenStore.tokenFlow.first()
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        when (response.code) {
            401 -> {
                // Token expired or manipulated (invalid signature)
                runBlocking {
                    tokenStore.clear()
                }
                // In a real app, you might want to trigger a navigation event to Login here
            }
            403 -> {
                // Forbidden: Role-based access denied
                // We let the UI layer handle the specific 403 error message
            }
        }

        return response
    }
}
