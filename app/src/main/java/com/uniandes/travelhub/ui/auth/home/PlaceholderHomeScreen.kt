package com.uniandes.travelhub.ui.auth.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.theme.Slate500
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * Temporary post-login destination. Reads the persisted role from the
 * [AuthRepository] and offers a logout button. Will be replaced by the real
 * dashboard once the post-auth feature lands.
 */
@Composable
fun PlaceholderHomeScreen(
    repository: AuthRepository,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    titleRes: Int? = null,
) {
    val role by repository.observeRole()
        .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    PlaceholderHomeContent(
        role = role,
        title = titleRes?.let { stringResource(it) },
        onLogout = {
            scope.launch {
                repository.logout()
                onLoggedOut()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun PlaceholderHomeContent(
    role: UserRole?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title ?: stringResource(R.string.home_placeholder_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    R.string.home_placeholder_subtitle,
                    role?.name ?: "—"
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    top = MaterialTheme.spacing.sm,
                    bottom = MaterialTheme.spacing.lg,
                ),
            )
            TravelHubPrimaryButton(
                text = stringResource(R.string.home_placeholder_logout),
                onClick = onLogout,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PlaceholderHomeContentPreview() {
    TravelhubTheme {
        PlaceholderHomeContent(role = UserRole.TRAVELER, onLogout = {})
    }
}
