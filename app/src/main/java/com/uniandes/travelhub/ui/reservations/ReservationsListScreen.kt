package com.uniandes.travelhub.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationStatusGroup
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.reservations.components.ReservationCard
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.ReservationsListUiState
import com.uniandes.travelhub.viewmodels.ReservationsListViewModel

@Composable
fun ReservationsListScreen(
    viewModel: ReservationsListViewModel,
    onReservationClick: (ReservationWithDetailsResponse) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()

    ReservationsListScreenContent(
        uiState = uiState,
        selectedGroup = selectedGroup,
        onSelectGroup = viewModel::selectGroup,
        onReservationClick = onReservationClick,
        onBackClick = onBackClick,
        onSearchClick = onSearchClick,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsListScreenContent(
    uiState: ReservationsListUiState,
    selectedGroup: ReservationStatusGroup?,
    onSelectGroup: (ReservationStatusGroup?) -> Unit,
    onReservationClick: (ReservationWithDetailsResponse) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reservations_list_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.property_detail_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            FilterRow(
                selectedGroup = selectedGroup,
                onSelectGroup = onSelectGroup,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is ReservationsListUiState.Loading ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is ReservationsListUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(MaterialTheme.spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        ) {
                            Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                            TravelHubPrimaryButton(stringResource(R.string.property_retry), onClick = onRetry)
                        }
                    }
                    is ReservationsListUiState.Success -> {
                        if (uiState.reservations.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(MaterialTheme.spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                            ) {
                                Text(
                                    text = stringResource(R.string.reservations_list_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                TravelHubPrimaryButton(
                                    text = stringResource(R.string.reservations_list_search_cta),
                                    onClick = onSearchClick,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(MaterialTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                            ) {
                                items(uiState.reservations, key = { it.id }) { reservation ->
                                    ReservationCard(
                                        reservation = reservation,
                                        onClick = { onReservationClick(reservation) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selectedGroup: ReservationStatusGroup?,
    onSelectGroup: (ReservationStatusGroup?) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        item {
            FilterChip(
                selected = selectedGroup == ReservationStatusGroup.ACTIVE,
                onClick = { onSelectGroup(ReservationStatusGroup.ACTIVE) },
                label = { Text(stringResource(R.string.reservations_filter_active)) },
            )
        }
        item {
            FilterChip(
                selected = selectedGroup == ReservationStatusGroup.PAST,
                onClick = { onSelectGroup(ReservationStatusGroup.PAST) },
                label = { Text(stringResource(R.string.reservations_filter_past)) },
            )
        }
        item {
            FilterChip(
                selected = selectedGroup == ReservationStatusGroup.CANCELLED,
                onClick = { onSelectGroup(ReservationStatusGroup.CANCELLED) },
                label = { Text(stringResource(R.string.reservations_filter_cancelled)) },
            )
        }
        item {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                label = { Text(stringResource(R.string.reservations_filter_all)) },
            )
        }
    }
}
