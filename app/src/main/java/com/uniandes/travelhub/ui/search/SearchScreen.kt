package com.uniandes.travelhub.ui.search

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.uniandes.travelhub.models.search.SearchOrderBy
import com.uniandes.travelhub.models.search.SearchOrderDir
import com.uniandes.travelhub.models.search.SearchResultItem
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.search.components.SearchFilters
import com.uniandes.travelhub.ui.search.components.SearchResultCard
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.SearchFormState
import com.uniandes.travelhub.viewmodels.SearchResultsState
import com.uniandes.travelhub.viewmodels.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (SearchResultItem) -> Unit,
    onLoggedOut: () -> Unit,
    onMyReservationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form by viewModel.form.collectAsState()
    val results by viewModel.results.collectAsState()

    SearchScreenContent(
        form = form,
        results = results,
        onCityChange = viewModel::onCityChange,
        onCheckInChange = viewModel::onCheckInChange,
        onCheckOutChange = viewModel::onCheckOutChange,
        onGuestsChange = viewModel::onGuestsChange,
        onMinPriceChange = viewModel::onMinPriceChange,
        onMaxPriceChange = viewModel::onMaxPriceChange,
        onAmenityToggle = viewModel::toggleAmenity,
        onOrderByChange = viewModel::onOrderByChange,
        onOrderDirChange = viewModel::onOrderDirChange,
        onSubmit = viewModel::submit,
        onLoadMore = viewModel::loadNextPage,
        onResultClick = onResultClick,
        onLoggedOut = onLoggedOut,
        onMyReservationsClick = onMyReservationsClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    form: SearchFormState,
    results: SearchResultsState,
    onCityChange: (String) -> Unit,
    onCheckInChange: (String) -> Unit,
    onCheckOutChange: (String) -> Unit,
    onGuestsChange: (Int) -> Unit,
    onMinPriceChange: (Int?) -> Unit,
    onMaxPriceChange: (Int?) -> Unit,
    onAmenityToggle: (String) -> Unit,
    onOrderByChange: (SearchOrderBy?) -> Unit,
    onOrderDirChange: (SearchOrderDir?) -> Unit,
    onSubmit: () -> Unit,
    onLoadMore: () -> Unit,
    onResultClick: (SearchResultItem) -> Unit,
    onLoggedOut: () -> Unit,
    onMyReservationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onMyReservationsClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.search_my_reservations),
                        )
                    }
                    IconButton(onClick = onLoggedOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.home_placeholder_logout),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            item("filters") {
                SearchFilters(
                    form = form,
                    onCityChange = onCityChange,
                    onCheckInChange = onCheckInChange,
                    onCheckOutChange = onCheckOutChange,
                    onGuestsChange = onGuestsChange,
                    onMinPriceChange = onMinPriceChange,
                    onMaxPriceChange = onMaxPriceChange,
                    onAmenityToggle = onAmenityToggle,
                    onOrderByChange = onOrderByChange,
                    onOrderDirChange = onOrderDirChange,
                )
            }
            item("submit") {
                TravelHubPrimaryButton(
                    text = stringResource(R.string.search_button),
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (results) {
                is SearchResultsState.Idle -> Unit
                is SearchResultsState.Loading -> item("loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                is SearchResultsState.Error -> item("error") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(
                            text = results.message.asString(),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TravelHubPrimaryButton(
                            text = stringResource(R.string.property_retry),
                            onClick = onSubmit,
                        )
                    }
                }
                is SearchResultsState.Success -> {
                    val items = results.response.items
                    if (items.isEmpty()) {
                        item("empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.lg),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.search_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                results.response.emptyState.firstOrNull()?.let { hint ->
                                    Text(
                                        text = hint.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            SearchResultCard(item = item, onClick = { onResultClick(item) })
                        }
                        val pagination = results.response.pagination
                        if (pagination.page < pagination.totalPages) {
                            item("loadMore") {
                                TravelHubPrimaryButton(
                                    text = stringResource(R.string.search_load_more),
                                    onClick = onLoadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
