package com.uniandes.travelhub

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import com.uniandes.travelhub.ui.auth.login.LoginScreenContent
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.viewmodels.LoginUiState


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelhubTheme {
                var email by rememberSaveable { mutableStateOf("") }
                var password by rememberSaveable { mutableStateOf("") }
                var uiState by remember { mutableStateOf<LoginUiState>(LoginUiState.Idle) }
                val currentLocale = AppCompatDelegate.getApplicationLocales()
                    .toLanguageTags()
                    .takeIf { it.isNotEmpty() }
                    ?.substringBefore('-')
                    ?: "es"

                LoginScreenContent(
                    uiState = uiState,
                    email = email,
                    password = password,
                    currentLocale = currentLocale,
                    onEmailChange = {
                        email = it
                        if (uiState is LoginUiState.Error) uiState = LoginUiState.Idle
                    },
                    onPasswordChange = {
                        password = it
                        if (uiState is LoginUiState.Error) uiState = LoginUiState.Idle
                    },
                    onSubmit = {
                        uiState = if (email.isBlank() || password.isBlank()) {
                            LoginUiState.Error("Completa email y contraseña para probar el banner")
                        } else {
                            LoginUiState.Loading
                        }
                    },
                    onNavigateToRegister = {  },
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
