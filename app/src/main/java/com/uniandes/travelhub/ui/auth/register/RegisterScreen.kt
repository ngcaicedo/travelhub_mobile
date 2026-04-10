package com.uniandes.travelhub.ui.auth.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.ui.auth.components.ErrorBanner
import com.uniandes.travelhub.ui.auth.components.LanguageSwitcher
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.ui.theme.Slate500
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.RegisterEvent
import com.uniandes.travelhub.viewmodels.RegisterFormState
import com.uniandes.travelhub.viewmodels.RegisterUiState
import com.uniandes.travelhub.viewmodels.RegisterViewModel

/**
 * Stateful entry point: connects the [RegisterViewModel] to the stateless
 * content composable and forwards navigation events. Tests and previews target
 * [RegisterScreenContent] directly.
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        form = form,
        currentLocale = currentLocale,
        onRoleChange = viewModel::onRoleChange,
        onFullNameChange = viewModel::onFullNameChange,
        onHotelNameChange = viewModel::onHotelNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTermsChange = viewModel::onTermsChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToLogin = onNavigateToLogin,
        onLocaleChange = onLocaleChange,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreenContent(
    uiState: RegisterUiState,
    form: RegisterFormState,
    currentLocale: String,
    onRoleChange: (UserRole) -> Unit,
    onFullNameChange: (String) -> Unit,
    onHotelNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
                    text = stringResource(R.string.auth_register_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.auth_register_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }

            val selectedTab = if (form.role == UserRole.TRAVELER) 0 else 1
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onRoleChange(UserRole.TRAVELER) },
                    text = { Text(stringResource(R.string.auth_register_tab_traveler)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onRoleChange(UserRole.HOTEL_PARTNER) },
                    text = { Text(stringResource(R.string.auth_register_tab_host)) },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.lg,
                        vertical = MaterialTheme.spacing.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                if (uiState is RegisterUiState.Error) {
                    ErrorBanner(message = uiState.message.asString())
                }

                RegisterForm(
                    state = form,
                    isError = uiState is RegisterUiState.Error,
                    onFullNameChange = onFullNameChange,
                    onHotelNameChange = onHotelNameChange,
                    onEmailChange = onEmailChange,
                    onPhoneChange = onPhoneChange,
                    onPasswordChange = onPasswordChange,
                    onTermsChange = onTermsChange,
                )

                TravelHubPrimaryButton(
                    text = stringResource(R.string.auth_register_submit),
                    onClick = onSubmit,
                    loading = uiState is RegisterUiState.Loading,
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
                val annotated = buildAnnotatedString {
                    append(stringResource(R.string.auth_register_have_account))
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(stringResource(R.string.auth_register_sign_in))
                    }
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate500),
                    modifier = Modifier.clickable { onNavigateToLogin() },
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenTravelerPreview() {
    TravelhubTheme {
        RegisterScreenContent(
            uiState = RegisterUiState.Idle,
            form = RegisterFormState(role = UserRole.TRAVELER),
            currentLocale = "es",
            onRoleChange = {},
            onFullNameChange = {},
            onHotelNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onTermsChange = {},
            onSubmit = {},
            onNavigateToLogin = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenHostPreview() {
    TravelhubTheme {
        RegisterScreenContent(
            uiState = RegisterUiState.Idle,
            form = RegisterFormState(
                role = UserRole.HOTEL_PARTNER,
                hotelName = "Hotel Plaza",
                fullName = "Ada Lovelace",
                email = "ada@example.com",
                phone = "+57 300 000 0000",
                password = "Sup3rSecret!",
                agreedToTerms = true,
            ),
            currentLocale = "es",
            onRoleChange = {},
            onFullNameChange = {},
            onHotelNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onTermsChange = {},
            onSubmit = {},
            onNavigateToLogin = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenErrorPreview() {
    TravelhubTheme {
        RegisterScreenContent(
            uiState = RegisterUiState.Error(ErrorMessage.Plain("Correo electrónico inválido")),
            form = RegisterFormState(
                role = UserRole.TRAVELER,
                fullName = "Ada",
                email = "no-arroba",
                phone = "300",
                password = "weak",
            ),
            currentLocale = "es",
            onRoleChange = {},
            onFullNameChange = {},
            onHotelNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onPasswordChange = {},
            onTermsChange = {},
            onSubmit = {},
            onNavigateToLogin = {},
            onLocaleChange = {},
        )
    }
}
