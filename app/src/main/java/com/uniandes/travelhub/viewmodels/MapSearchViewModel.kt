package com.uniandes.travelhub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.SearchBounds
import com.uniandes.travelhub.models.search.SearchResponse
import com.uniandes.travelhub.models.search.SearchResultItem
import com.uniandes.travelhub.network.location.CityGeocoder
import com.uniandes.travelhub.network.location.LocationException
import com.uniandes.travelhub.network.location.LocationProvider
import com.uniandes.travelhub.network.location.UserLocation
import com.uniandes.travelhub.repositories.SearchRepository
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whether the user has granted any location permission. */
sealed interface PermissionState {
    data object Unknown : PermissionState
    data object Granted : PermissionState
    data object Denied : PermissionState
}

/**
 * Camera viewport state: centre + bounds the map is currently showing.
 * Reported by the UI when the camera goes idle so we can decide whether to
 * surface the "Search in this area" FAB.
 */
data class CameraViewport(
    val center: UserLocation,
    val bounds: SearchBounds,
)

sealed interface MapSearchUiState {
    data object Idle : MapSearchUiState
    data object LocatingUser : MapSearchUiState
    data object Loading : MapSearchUiState
    data class Loaded(
        val items: List<SearchResultItem>,
        val userLocation: UserLocation?,
        val lastSearchedBounds: SearchBounds,
        val emptyAreaHint: Boolean,
    ) : MapSearchUiState
    data class Error(val message: ErrorMessage) : MapSearchUiState
}

sealed interface MapSearchEvent {
    data object RequestLocationPermission : MapSearchEvent
    data class MoveCameraTo(val location: UserLocation, val zoom: Float = 13f) : MapSearchEvent
}

/**
 * ViewModel for the map view inside SearchScreen.
 */
class MapSearchViewModel(
    private val repository: SearchRepository,
    private val locationProvider: LocationProvider,
    private val cityGeocoder: CityGeocoder,
    private val driftThreshold: Double = 0.15,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapSearchUiState>(MapSearchUiState.Idle)
    val uiState: StateFlow<MapSearchUiState> = _uiState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    /** True once the user has moved the camera far enough to warrant a refresh. */
    private val _showSearchInThisArea = MutableStateFlow(false)
    val showSearchInThisArea: StateFlow<Boolean> = _showSearchInThisArea.asStateFlow()

    private val _events = Channel<MapSearchEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastSearchedBounds: SearchBounds? = null

    /** Called by the host once the permission launcher returns a result. */
    fun onPermissionResult(granted: Boolean) {
        _permissionState.value = if (granted) PermissionState.Granted else PermissionState.Denied
        if (granted) locateAndSearch()
    }

    /** Called by the host the first time the screen is shown if the permission is unknown. */
    fun requestLocationPermission() {
        viewModelScope.launch { _events.send(MapSearchEvent.RequestLocationPermission) }
    }

    /** The map reports the camera went idle. Decide whether to show the FAB. */
    fun onCameraIdle(viewport: CameraViewport) {
        val baseline = lastSearchedBounds ?: return
        val latDrift = abs(viewport.bounds.minLat - baseline.minLat) +
            abs(viewport.bounds.maxLat - baseline.maxLat)
        val lngDrift = abs(viewport.bounds.minLng - baseline.minLng) +
            abs(viewport.bounds.maxLng - baseline.maxLng)
        val drifted = latDrift / baseline.latSpan > driftThreshold ||
            lngDrift / baseline.lngSpan > driftThreshold
        _showSearchInThisArea.value = drifted
    }

    /** User tapped the FAB. */
    fun searchInThisArea(viewport: CameraViewport) {
        runSearch(viewport.bounds, viewport.center)
    }

    /**
     * User typed a city name in the map's search field. Geocodes it, jumps the
     * camera there and runs a bbox search around the resolved coordinate.
     */
    fun searchByCityName(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _uiState.value = MapSearchUiState.Loading
        viewModelScope.launch {
            cityGeocoder.geocode(trimmed).fold(
                onSuccess = { location ->
                    _events.send(MapSearchEvent.MoveCameraTo(location))
                    runSearch(boundsAround(location), location)
                },
                onFailure = {
                    _uiState.value = MapSearchUiState.Error(
                        ErrorMessage.Resource(R.string.map_city_not_found, listOf(trimmed))
                    )
                },
            )
        }
    }

    private fun locateAndSearch() {
        _uiState.value = MapSearchUiState.LocatingUser
        viewModelScope.launch {
            locationProvider.getCurrentLocation().fold(
                onSuccess = { user ->
                    val bounds = boundsAround(user)
                    runSearch(bounds, user, fromUser = user)
                },
                onFailure = { exc ->
                    val msg = if (exc is LocationException && exc.message == "permission_denied") {
                        ErrorMessage.Resource(R.string.map_permission_required_body)
                    } else {
                        ErrorMessage.Resource(R.string.map_location_unavailable)
                    }
                    _uiState.value = MapSearchUiState.Error(msg)
                },
            )
        }
    }

    private fun runSearch(
        bounds: SearchBounds,
        center: UserLocation,
        fromUser: UserLocation? = null,
    ) {
        _uiState.value = MapSearchUiState.Loading
        viewModelScope.launch {
            repository.searchByBounds(bounds).fold(
                onSuccess = { response: SearchResponse ->
                    lastSearchedBounds = bounds
                    _showSearchInThisArea.value = false
                    val items = response.items.filter {
                        it.latitude != null && it.longitude != null
                    }
                    _uiState.value = MapSearchUiState.Loaded(
                        items = items,
                        userLocation = fromUser ?: extractUserLocation(),
                        lastSearchedBounds = bounds,
                        emptyAreaHint = response.items.isEmpty(),
                    )
                },
                onFailure = {
                    _uiState.value = MapSearchUiState.Error(
                        ErrorMessage.Plain(it.message ?: "")
                    )
                },
            )
        }
    }

    private fun extractUserLocation(): UserLocation? =
        (uiState.value as? MapSearchUiState.Loaded)?.userLocation

    /** ~5km box around a point — small enough to load fast, big enough to see clusters. */
    private fun boundsAround(point: UserLocation): SearchBounds {
        val padLat = 0.045
        val padLng = 0.045
        return SearchBounds(
            minLat = point.latitude - padLat,
            maxLat = point.latitude + padLat,
            minLng = point.longitude - padLng,
            maxLng = point.longitude + padLng,
        )
    }

    class Factory(
        private val repository: SearchRepository,
        private val locationProvider: LocationProvider,
        private val cityGeocoder: CityGeocoder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MapSearchViewModel(repository, locationProvider, cityGeocoder) as T
    }
}
