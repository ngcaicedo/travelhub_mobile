package com.uniandes.travelhub.ui.auth.verifyotp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.ui.auth.components.AuthHeroPlaceholder
import com.uniandes.travelhub.ui.auth.components.ErrorBanner
import com.uniandes.travelhub.ui.auth.components.LanguageSwitcher
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.TravelHubTextField
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.ui.theme.Slate400
import com.uniandes.travelhub.ui.theme.Slate500
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.VerifyOtpEvent
import com.uniandes.travelhub.viewmodels.VerifyOtpUiState
import com.uniandes.travelhub.viewmodels.VerifyOtpViewModel

/**
 * Stateful entry point: connects the [VerifyOtpViewModel] to the stateless
 * content composable and forwards navigation events. Tests and previews target
 * [VerifyOtpScreenContent] directly.
 */
@Composable
fun VerifyOtpScreen(
    viewModel: VerifyOtpViewModel,
    email: String,
    onNavigateToHome: (UserRole) -> Unit,
    onNavigateBackToLogin: () -> Unit,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otpCode by viewModel.otpCode.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VerifyOtpEvent.NavigateToHome -> onNavigateToHome(event.role)
            }
        }
    }

    VerifyOtpScreenContent(
        uiState = uiState,
        email = email,
        otpCode = otpCode,
        currentLocale = currentLocale,
        onOtpChange = viewModel::onOtpChange,
        onSubmit = viewModel::onSubmit,
        onNavigateBackToLogin = onNavigateBackToLogin,
        onLocaleChange = onLocaleChange,
        modifier = modifier,
    )
}

@Composable
fun VerifyOtpScreenContent(
    uiState: VerifyOtpUiState,
    email: String,
    otpCode: String,
    currentLocale: String,
    onOtpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.spacing.md,
                        end = MaterialTheme.spacing.sm,
                        top = MaterialTheme.spacing.md,
                        bottom = MaterialTheme.spacing.sm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(modifier = Modifier.weight(1f))
                LanguageSwitcher(
                    currentTag = currentLocale,
                    onLanguageSelected = onLocaleChange,
                )
            }

            AuthHeroPlaceholder()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.lg,
                        vertical = MaterialTheme.spacing.md,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.auth_verify_otp_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.auth_verify_otp_subtitle, email),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.lg,
                        vertical = MaterialTheme.spacing.sm,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                if (uiState is VerifyOtpUiState.Error) {
                    ErrorBanner(message = uiState.message.asString())
                }

                TravelHubTextField(
                    value = otpCode,
                    onValueChange = onOtpChange,
                    label = stringResource(R.string.auth_verify_otp_code_label),
                    placeholder = stringResource(R.string.auth_verify_otp_code_placeholder),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Pin,
                            contentDescription = null,
                            tint = Slate400,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = uiState is VerifyOtpUiState.Error,
                )

                TravelHubPrimaryButton(
                    text = stringResource(R.string.auth_verify_otp_submit),
                    onClick = onSubmit,
                    loading = uiState is VerifyOtpUiState.Loading,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.lg,
                        vertical = MaterialTheme.spacing.lg,
                    ),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.auth_verify_otp_back_to_login),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateBackToLogin() },
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VerifyOtpScreenIdlePreview() {
    TravelhubTheme {
        VerifyOtpScreenContent(
            uiState = VerifyOtpUiState.Idle,
            email = "ada@example.com",
            otpCode = "",
            currentLocale = "es",
            onOtpChange = {},
            onSubmit = {},
            onNavigateBackToLogin = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VerifyOtpScreenLoadingPreview() {
    TravelhubTheme {
        VerifyOtpScreenContent(
            uiState = VerifyOtpUiState.Loading,
            email = "ada@example.com",
            otpCode = "123456",
            currentLocale = "es",
            onOtpChange = {},
            onSubmit = {},
            onNavigateBackToLogin = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VerifyOtpScreenErrorPreview() {
    TravelhubTheme {
        VerifyOtpScreenContent(
            uiState = VerifyOtpUiState.Error(ErrorMessage.Plain("El código debe tener 6 dígitos")),
            email = "ada@example.com",
            otpCode = "123",
            currentLocale = "es",
            onOtpChange = {},
            onSubmit = {},
            onNavigateBackToLogin = {},
            onLocaleChange = {},
        )
    }
}
