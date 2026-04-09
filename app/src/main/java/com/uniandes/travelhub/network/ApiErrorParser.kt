package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import retrofit2.HttpException
import java.io.IOException

object ApiErrorParser {

    private val moshi: Moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    fun getApiErrorMessage(throwable: Throwable, fallback: String): String {
        return when (throwable) {
            is HttpException -> parseHttp(throwable, fallback)
            is IOException -> fallback
            else -> throwable.message ?: fallback
        }
    }

    private fun parseHttp(error: HttpException, fallback: String): String {
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
        if (body.isNullOrBlank()) return fallback

        val parsed = runCatching { mapAdapter.fromJson(body) }.getOrNull() ?: return fallback
        val detail = parsed["detail"] ?: return fallback

        return when (detail) {
            is String -> detail
            is List<*> -> detail
                .mapNotNull { (it as? Map<*, *>)?.get("msg")?.toString() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
                ?: fallback
            else -> fallback
        }
    }
}
