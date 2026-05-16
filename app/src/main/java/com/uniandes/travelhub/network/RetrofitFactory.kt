package com.uniandes.travelhub.network

import com.squareup.moshi.Moshi
import com.uniandes.travelhub.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {

    private val moshi: Moshi = Moshi.Builder().build()

    /**
     * Held in a volatile reference so [AuthInterceptor]/[UnauthorizedAuthenticator]
     * — which capture the provider at OkHttp build time — see the latest value
     * even when `init` is called after the client is already cached.
     */
    @Volatile
    private var authTokenStoreRef: AuthTokenStore? = null

    /**
     * Registers the [AuthTokenStore] used by the auth interceptor and 401 authenticator.
     * Safe to call before, during or after the first API access — the OkHttp client
     * always carries the interceptors, and they read the store lazily on every request.
     */
    fun init(tokenStore: AuthTokenStore) {
        authTokenStoreRef = tokenStore
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(SecureTransportInterceptor())
            .addInterceptor(AuthInterceptor { authTokenStoreRef })
            .authenticator(UnauthorizedAuthenticator { authTokenStoreRef })

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    val securityApi: SecurityApi by lazy { build(BuildConfig.SECURITY_API_BASE) }
    val usersApi: UsersApi by lazy { build(BuildConfig.USERS_API_BASE) }
    val propertiesApi: PropertiesApi by lazy { build(BuildConfig.PROPERTIES_API_BASE) }
    val searchApi: SearchApi by lazy { build(BuildConfig.SEARCH_API_BASE) }
    val reservationsApi: ReservationsApi by lazy { build(BuildConfig.RESERVATIONS_API_BASE) }
    val paymentsApi: PaymentsApi by lazy { build(BuildConfig.PAYMENTS_API_BASE) }
    val notificationsApi: NotificationsApi by lazy { build(BuildConfig.NOTIFICATIONS_API_BASE) }

    private inline fun <reified T> build(baseUrl: String): T =
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(T::class.java)

    internal fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
