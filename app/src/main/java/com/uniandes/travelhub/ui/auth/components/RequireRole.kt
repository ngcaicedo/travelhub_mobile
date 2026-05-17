package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.ui.theme.spacing
import kotlinx.coroutines.flow.combine

/**
 * Snapshot of "do we have a session?" derived from [AuthTokenStore]. Using a
 * sealed type instead of plain nullable role lets the guard distinguish
 * `Loading` (the DataStore flow hasn't emitted yet) from `Unauthenticated`
 * (it emitted and there is no token), so we never mistake the initial null
 * for a missing session.
 */
private sealed interface SessionState {
    data object Loading : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val role: UserRole?) : SessionState
}

@Composable
fun RequireRole(
    tokenStore: AuthTokenStore,
    requiredRole: UserRole,
    onUnauthorized: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state by produceState<SessionState>(SessionState.Loading, tokenStore) {
        combine(tokenStore.tokenFlow, tokenStore.roleFlow) { token, role ->
            if (token.isNullOrBlank()) SessionState.Unauthenticated
            else SessionState.Authenticated(role)
        }.collect { value = it }
    }

    when (val s = state) {
        is SessionState.Loading -> Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is SessionState.Unauthenticated -> {
            LaunchedEffect(Unit) { onUnauthorized() }
            ForbiddenScreen()
        }
        is SessionState.Authenticated -> {
            if (s.role == requiredRole) content() else ForbiddenScreen()
        }
    }
}

@Composable
fun ForbiddenScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.forbidden_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.forbidden_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
