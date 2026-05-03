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
    private var authTokenStore: AuthTokenStore? = null

    /**
     * Initializes the factory with an [AuthTokenStore] to enable JWT authentication
     * and 401-driven session clearing. Call once from `MainActivity`.
     */
    fun init(tokenStore: AuthTokenStore) {
        this.authTokenStore = tokenStore
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        authTokenStore?.let {
            builder.addInterceptor(AuthInterceptor(it))
            builder.authenticator(UnauthorizedAuthenticator(it))
        }

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

    private inline fun <reified T> build(baseUrl: String): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(T::class.java)
}
