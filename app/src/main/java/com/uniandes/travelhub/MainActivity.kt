package com.uniandes.travelhub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.DataStorePropertyCacheStore
import com.uniandes.travelhub.network.RetrofitFactory
import com.uniandes.travelhub.network.location.AndroidCityGeocoder
import com.uniandes.travelhub.network.location.CityGeocoder
import com.uniandes.travelhub.network.location.FusedLocationProvider
import com.uniandes.travelhub.network.location.LocationProvider
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.repositories.NotificationsRepository
import com.uniandes.travelhub.repositories.PaymentsRepository
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.ui.auth.navigation.AuthNavGraph
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(FCM_TAG, "POST_NOTIFICATIONS granted=$granted")
    }

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

    private val reservationsRepository: ReservationsRepository by lazy {
        ReservationsRepository(
            reservationsApi = RetrofitFactory.reservationsApi,
            tokenStore = tokenStore,
        )
    }

    private val paymentsRepository: PaymentsRepository by lazy {
        PaymentsRepository(
            paymentsApi = RetrofitFactory.paymentsApi,
            tokenStore = tokenStore,
        )
    }

    private val locationProvider: LocationProvider by lazy {
        FusedLocationProvider(applicationContext)
    }

    private val cityGeocoder: CityGeocoder by lazy {
        AndroidCityGeocoder(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        registerFcmDeviceWhenAuthenticated()
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
                    reservationsRepository = reservationsRepository,
                    paymentsRepository = paymentsRepository,
                    tokenStore = tokenStore,
                    locationProvider = locationProvider,
                    cityGeocoder = cityGeocoder,
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun registerFcmDeviceWhenAuthenticated() {
        val notificationsRepository = NotificationsRepository(RetrofitFactory.notificationsApi)
        lifecycleScope.launch {
            tokenStore.tokenFlow
                .filter { !it.isNullOrBlank() }
                .distinctUntilChanged()
                .collect {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(FCM_TAG, "Failed to fetch FCM token", task.exception)
                            return@addOnCompleteListener
                        }
                        val fcmToken = task.result ?: return@addOnCompleteListener
                        lifecycleScope.launch {
                            runCatching {
                                notificationsRepository.registerDevice(
                                    fcmToken,
                                    BuildConfig.VERSION_NAME,
                                )
                            }.onFailure { Log.w(FCM_TAG, "Failed to register device", it) }
                        }
                    }
                }
        }
    }

    private companion object {
        const val FCM_TAG = "TravelHubFcm"
    }
}
