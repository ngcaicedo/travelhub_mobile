package com.uniandes.travelhub.ui.search

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.SearchBounds
import com.uniandes.travelhub.models.search.SearchResultItem
import com.uniandes.travelhub.network.location.UserLocation
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.CameraViewport
import com.uniandes.travelhub.viewmodels.MapSearchEvent
import com.uniandes.travelhub.viewmodels.MapSearchUiState
import com.uniandes.travelhub.viewmodels.MapSearchViewModel
import com.uniandes.travelhub.viewmodels.PermissionState

private val DEFAULT_CENTER = LatLng(4.7110, -74.0721)
private const val DEFAULT_ZOOM = 12f

@Composable
fun MapSearchContent(
    viewModel: MapSearchViewModel,
    onResultClick: (SearchResultItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.uiState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val showSearchHere by viewModel.showSearchInThisArea.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MapSearchEvent.RequestLocationPermission -> permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
                is MapSearchEvent.MoveCameraTo -> {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(event.location.latitude, event.location.longitude),
                        event.zoom,
                    )
                }
            }
        }
    }
    LaunchedEffect(permissionState) {
        if (permissionState is PermissionState.Unknown) viewModel.requestLocationPermission()
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            buildViewport(cameraPositionState)?.let(viewModel::onCameraIdle)
        }
    }
    val loaded = ui as? MapSearchUiState.Loaded
    LaunchedEffect(loaded?.userLocation) {
        loaded?.userLocation?.let { user ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(user.latitude, user.longitude),
                13f,
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionState is PermissionState.Granted,
            ),
        ) {
            loaded?.items?.forEach { item ->
                val lat = item.latitude ?: return@forEach
                val lng = item.longitude ?: return@forEach
                val markerState = remember(item.id) { MarkerState(LatLng(lat, lng)) }
                Marker(
                    state = markerState,
                    title = item.name,
                    snippet = item.city,
                    onInfoWindowClick = { onResultClick(item) },
                )
            }
        }

        when (ui) {
            MapSearchUiState.LocatingUser -> CenterMessage(R.string.map_locating_user)
            MapSearchUiState.Loading -> CenterMessage(R.string.map_loading_hotels)
            is MapSearchUiState.Error -> {
                val msg = (ui as MapSearchUiState.Error).message
                CenterCard(text = msg.asString())
            }
            is MapSearchUiState.Loaded -> {
                if ((ui as MapSearchUiState.Loaded).emptyAreaHint) {
                    CenterCard(text = stringResource(R.string.map_no_results_in_area))
                }
            }
            else -> Unit
        }

        if (permissionState is PermissionState.Denied) {
            LocationPermissionGate(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md)
                    .align(Alignment.TopCenter),
                onAllowClick = { viewModel.requestLocationPermission() },
            )
        } else {
            CitySearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md)
                    .align(Alignment.TopCenter),
                onSubmit = { query -> viewModel.searchByCityName(query) },
            )
        }

        if (showSearchHere) {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(MaterialTheme.spacing.lg),
                onClick = {
                    buildViewport(cameraPositionState)?.let(viewModel::searchInThisArea)
                },
                text = { Text(stringResource(R.string.map_search_in_this_area)) },
                icon = {},
            )
        }
    }
}

@Composable
private fun CenterMessage(messageRes: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                CircularProgressIndicator()
                Text(stringResource(messageRes))
            }
        }
    }
}

@Composable
private fun CenterCard(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card { Text(text = text, modifier = Modifier.padding(MaterialTheme.spacing.md)) }
    }
}

@Composable
private fun CitySearchBar(
    modifier: Modifier = Modifier,
    onSubmit: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            keyboard?.hide()
            onSubmit(trimmed)
        }
    }
    Card(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.map_city_search_hint)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                IconButton(onClick = submit) {
                    Text(stringResource(R.string.map_city_search_action))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.sm),
        )
    }
}

@Composable
fun LocationPermissionGate(
    modifier: Modifier = Modifier,
    onAllowClick: () -> Unit,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.map_permission_required_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.map_permission_required_body))
            TravelHubPrimaryButton(
                text = stringResource(R.string.map_permission_cta),
                onClick = onAllowClick,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onAllowClick) {
                Text(stringResource(R.string.map_permission_denied_cta))
            }
        }
    }
}

private fun buildViewport(state: CameraPositionState): CameraViewport? {
    val region = state.projection?.visibleRegion ?: return null
    val nearLeft = region.nearLeft
    val farRight = region.farRight
    val center = state.position.target
    return runCatching {
        CameraViewport(
            center = UserLocation(center.latitude, center.longitude),
            bounds = SearchBounds(
                minLat = minOf(nearLeft.latitude, farRight.latitude),
                maxLat = maxOf(nearLeft.latitude, farRight.latitude),
                minLng = minOf(nearLeft.longitude, farRight.longitude),
                maxLng = maxOf(nearLeft.longitude, farRight.longitude),
            ),
        )
    }.getOrNull()
}
