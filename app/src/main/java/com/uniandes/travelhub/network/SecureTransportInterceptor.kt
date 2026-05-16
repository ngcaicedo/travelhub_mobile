package com.uniandes.travelhub.network

import com.uniandes.travelhub.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SecureTransportInterceptor(
    private val allowCleartext: Boolean = BuildConfig.DEBUG
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!allowCleartext && !request.url.isHttps) {
            throw IOException("TLS 1.2+ is required for TravelHub API requests")
        }
        return chain.proceed(request)
    }
}
