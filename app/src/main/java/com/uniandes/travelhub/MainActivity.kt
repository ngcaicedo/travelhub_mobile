package com.uniandes.travelhub

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.DataStoreCheckInQrCacheStore
import com.uniandes.travelhub.network.DataStorePropertyCacheStore
import com.uniandes.travelhub.network.RetrofitFactory
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.repositories.HotelPricingRepository
import com.uniandes.travelhub.repositories.HotelReservationsRepository
import com.uniandes.travelhub.repositories.PaymentsRepository
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.ui.auth.navigation.AuthNavGraph
import com.uniandes.travelhub.ui.theme.TravelhubTheme

class MainActivity : AppCompatActivity() {

    private val tokenStore: AuthTokenStore by lazy {
        AuthTokenStore.getInstance(applicationContext).also(RetrofitFactory::init)
    }

    private val authRepository: AuthRepository by lazy {
        AuthRepository(
            securityApi = RetrofitFactory.securityApi,
            usersApi = RetrofitFactory.usersApi,
            tokenStore = tokenStore,
        )
    }

    private val propertiesRepository: PropertiesRepository by lazy {
        PropertiesRepository(
            propertiesApi = RetrofitFactory.propertiesApi,
            cacheStore = DataStorePropertyCacheStore.getInstance(applicationContext),
        )
    }

    private val searchRepository: SearchRepository by lazy {
        SearchRepository(searchApi = RetrofitFactory.searchApi)
    }

    private val hotelPricingRepository: HotelPricingRepository by lazy {
        HotelPricingRepository(api = RetrofitFactory.hotelPricingApi)
    }

    private val hotelReservationsRepository: HotelReservationsRepository by lazy {
        HotelReservationsRepository(
            api = RetrofitFactory.hotelReservationsApi,
            pricingApi = RetrofitFactory.hotelPricingApi,
        )
    }

    private val reservationsRepository: ReservationsRepository by lazy {
        ReservationsRepository(
            reservationsApi = RetrofitFactory.reservationsApi,
            tokenStore = tokenStore,
            checkInQrCacheStore = DataStoreCheckInQrCacheStore.getInstance(applicationContext),
        )
    }

    private val paymentsRepository: PaymentsRepository by lazy {
        PaymentsRepository(
            paymentsApi = RetrofitFactory.paymentsApi,
            tokenStore = tokenStore,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelhubTheme {
                val currentLocale = AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags()
                    .takeIf { it.isNotEmpty() }
                    ?.substringBefore('-')
                    ?: "es"

                AuthNavGraph(
                    authRepository = authRepository,
                    propertiesRepository = propertiesRepository,
                    searchRepository = searchRepository,
                    hotelPricingRepository = hotelPricingRepository,
                    hotelReservationsRepository = hotelReservationsRepository,
                    reservationsRepository = reservationsRepository,
                    paymentsRepository = paymentsRepository,
                    tokenStore = tokenStore,
                    currentLocale = currentLocale,
                    onLocaleChange = { tag ->
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(tag)
                        )
                    },
                )
            }
        }
    }
}
