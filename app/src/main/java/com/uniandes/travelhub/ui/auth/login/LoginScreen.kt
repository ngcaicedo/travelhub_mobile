package com.uniandes.travelhub.ui.auth.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniandes.travelhub.R
import com.uniandes.travelhub.ui.auth.components.AuthHeroPlaceholder
import com.uniandes.travelhub.ui.auth.components.ErrorBanner
import com.uniandes.travelhub.ui.auth.components.LanguageSwitcher
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.TravelHubTextField
import com.uniandes.travelhub.ui.theme.Slate400
import com.uniandes.travelhub.ui.theme.Slate500
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.LoginEvent
import com.uniandes.travelhub.viewmodels.LoginUiState
import com.uniandes.travelhub.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToOtp: (email: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToOtp -> onNavigateToOtp(event.email)
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        email = email,
        password = password,
        currentLocale = currentLocale,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToRegister = onNavigateToRegister,
        onLocaleChange = onLocaleChange,
        modifier = modifier,
    )
}

@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    email: String,
    password: String,
    currentLocale: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLocaleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoonText = stringResource(R.string.common_coming_soon)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    text = stringResource(R.string.auth_login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
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
                if (uiState is LoginUiState.Error) {
                    ErrorBanner(message = uiState.message)
                }

                TravelHubTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.auth_login_email),
                    placeholder = stringResource(R.string.auth_login_email_placeholder),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = Slate400,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = uiState is LoginUiState.Error,
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.auth_login_forgot_password),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(
                                    end = MaterialTheme.spacing.xs,
                                    bottom = MaterialTheme.spacing.xs,
                                )
                                .clickable {
                                    scope.launch { snackbarHostState.showSnackbar(comingSoonText) }
                                }
                        )
                    }
                    PasswordField(
                        value = password,
                        onValueChange = onPasswordChange,
                        isError = uiState is LoginUiState.Error,
                    )
                }

                TravelHubPrimaryButton(
                    text = stringResource(R.string.auth_login_submit),
                    onClick = onSubmit,
                    loading = uiState is LoginUiState.Loading,
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
                    append(stringResource(R.string.auth_login_no_account))
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    ) {
                        append(stringResource(R.string.auth_login_create_account))
                    }
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate500),
                    modifier = Modifier.clickable { onNavigateToRegister() },
                )
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val showText = stringResource(R.string.auth_login_show_password)
    val hideText = stringResource(R.string.auth_login_hide_password)

    TravelHubTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_login_password),
        placeholder = stringResource(R.string.auth_login_password_placeholder),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Slate400,
            )
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) hideText else showText,
                    modifier = Modifier.size(20.dp),
                    tint = Slate400,
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = isError,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenIdlePreview() {
    TravelhubTheme {
        LoginScreenContent(
            uiState = LoginUiState.Idle,
            email = "",
            password = "",
            currentLocale = "es",
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onNavigateToRegister = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingPreview() {
    TravelhubTheme {
        LoginScreenContent(
            uiState = LoginUiState.Loading,
            email = "ada@example.com",
            password = "Sup3rSecret!",
            currentLocale = "es",
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onNavigateToRegister = {},
            onLocaleChange = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenErrorPreview() {
    TravelhubTheme {
        LoginScreenContent(
            uiState = LoginUiState.Error("Credenciales inválidas"),
            email = "ada@example.com",
            password = "wrong",
            currentLocale = "es",
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onNavigateToRegister = {},
            onLocaleChange = {},
        )
    }
}
