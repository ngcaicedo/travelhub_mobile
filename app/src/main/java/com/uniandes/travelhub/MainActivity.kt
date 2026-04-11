package com.uniandes.travelhub

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.RetrofitFactory
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.ui.auth.navigation.AuthNavGraph
import com.uniandes.travelhub.ui.theme.TravelhubTheme

class MainActivity : AppCompatActivity() {

    private val repository: AuthRepository by lazy {
        val tokenStore = AuthTokenStore.getInstance(applicationContext)
        RetrofitFactory.init(tokenStore)
        AuthRepository(
            securityApi = RetrofitFactory.securityApi,
            usersApi = RetrofitFactory.usersApi,
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
                    repository = repository,
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
