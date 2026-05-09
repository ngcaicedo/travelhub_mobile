package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
import com.uniandes.travelhub.models.search.SearchBounds
import com.uniandes.travelhub.models.search.SearchPagination
import com.uniandes.travelhub.models.search.SearchResponse
import com.uniandes.travelhub.models.search.SearchResultItem
import com.uniandes.travelhub.network.location.CityGeocoder
import com.uniandes.travelhub.network.location.LocationException
import com.uniandes.travelhub.network.location.LocationProvider
import com.uniandes.travelhub.network.location.UserLocation
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SearchRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var cityGeocoder: CityGeocoder
    private lateinit var viewModel: MapSearchViewModel

    private val bogotaUser = UserLocation(4.7110, -74.0721)
    private val bogotaItem = item("p1", 4.7110, -74.0721)

    @Before
    fun setUp() {
        repository = mockk()
        locationProvider = mockk()
        cityGeocoder = mockk()
        viewModel = MapSearchViewModel(repository, locationProvider, cityGeocoder)
    }

    @Test
    fun `granted permission triggers location lookup and bbox search`() = runTest {
        coEvery { locationProvider.getCurrentLocation() } returns Result.success(bogotaUser)
        coEvery { repository.searchByBounds(any(), any(), any()) } returns
            Result.success(response(listOf(bogotaItem)))

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MapSearchUiState.Loaded)
        val loaded = state as MapSearchUiState.Loaded
        assertEquals(1, loaded.items.size)
        assertEquals(bogotaUser, loaded.userLocation)
        assertEquals(PermissionState.Granted, viewModel.permissionState.value)
        coVerify { repository.searchByBounds(any(), any(), any()) }
    }

    @Test
    fun `denied permission keeps state Idle and emits Denied permissionState`() = runTest {
        viewModel.onPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(PermissionState.Denied, viewModel.permissionState.value)
        // No location call must have happened.
        coVerify(exactly = 0) { locationProvider.getCurrentLocation() }
        coVerify(exactly = 0) { repository.searchByBounds(any(), any(), any()) }
    }

    @Test
    fun `requestLocationPermission emits a one-shot RequestLocationPermission event`() = runTest {
        viewModel.events.test {
            viewModel.requestLocationPermission()
            assertEquals(MapSearchEvent.RequestLocationPermission, awaitItem())
        }
    }

    @Test
    fun `location failure surfaces an Error state`() = runTest {
        coEvery { locationProvider.getCurrentLocation() } returns
            Result.failure(LocationException("timeout"))

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MapSearchUiState.Error)
    }

    @Test
    fun `searchInThisArea uses the supplied viewport bbox and clears the FAB`() = runTest {
        coEvery { repository.searchByBounds(any(), any(), any()) } returns
            Result.success(response(listOf(bogotaItem)))

        val viewport = CameraViewport(
            center = bogotaUser,
            bounds = SearchBounds(4.66, 4.76, -74.12, -74.02),
        )
        viewModel.searchInThisArea(viewport)
        advanceUntilIdle()

        assertFalse(viewModel.showSearchInThisArea.value)
        coVerify { repository.searchByBounds(viewport.bounds, any(), any()) }
    }

    @Test
    fun `onCameraIdle without prior search keeps the FAB hidden`() {
        val viewport = CameraViewport(
            center = bogotaUser,
            bounds = SearchBounds(4.66, 4.76, -74.12, -74.02),
        )
        viewModel.onCameraIdle(viewport)
        assertFalse(viewModel.showSearchInThisArea.value)
    }

    @Test
    fun `onCameraIdle after sufficient drift shows the FAB`() = runTest {
        coEvery { locationProvider.getCurrentLocation() } returns Result.success(bogotaUser)
        coEvery { repository.searchByBounds(any(), any(), any()) } returns
            Result.success(response(listOf(bogotaItem)))
        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        // Pan ~25% east — drift well above the 15% default threshold.
        val baseline = (viewModel.uiState.value as MapSearchUiState.Loaded).lastSearchedBounds
        val driftedBounds = SearchBounds(
            minLat = baseline.minLat,
            maxLat = baseline.maxLat,
            minLng = baseline.minLng + baseline.lngSpan * 0.30,
            maxLng = baseline.maxLng + baseline.lngSpan * 0.30,
        )
        viewModel.onCameraIdle(CameraViewport(bogotaUser, driftedBounds))

        assertTrue(viewModel.showSearchInThisArea.value)
    }

    @Test
    fun `Loaded state filters out items without coordinates`() = runTest {
        coEvery { locationProvider.getCurrentLocation() } returns Result.success(bogotaUser)
        coEvery { repository.searchByBounds(any(), any(), any()) } returns
            Result.success(
                response(
                    listOf(
                        bogotaItem,
                        item("orphan", null, null),
                    )
                )
            )

        viewModel.onPermissionResult(granted = true)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as MapSearchUiState.Loaded
        assertEquals(1, loaded.items.size)
        assertEquals("p1", loaded.items.first().id)
    }

    @Test
    fun `searchByCityName geocodes, emits MoveCameraTo and runs bbox search`() = runTest {
        coEvery { cityGeocoder.geocode("Bogota") } returns Result.success(bogotaUser)
        coEvery { repository.searchByBounds(any(), any(), any()) } returns
            Result.success(response(listOf(bogotaItem)))

        viewModel.events.test {
            viewModel.searchByCityName("Bogota")
            assertEquals(MapSearchEvent.MoveCameraTo(bogotaUser), awaitItem())
        }
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as MapSearchUiState.Loaded
        assertEquals(1, loaded.items.size)
        coVerify { repository.searchByBounds(any(), any(), any()) }
    }

    @Test
    fun `searchByCityName with unknown city surfaces an Error`() = runTest {
        coEvery { cityGeocoder.geocode("Atlantis") } returns
            Result.failure(RuntimeException("not_found"))

        viewModel.searchByCityName("Atlantis")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MapSearchUiState.Error)
    }

    @Test
    fun `searchByCityName with blank input is a no-op`() = runTest {
        viewModel.searchByCityName("   ")
        advanceUntilIdle()
        assertEquals(MapSearchUiState.Idle, viewModel.uiState.value)
        coVerify(exactly = 0) { cityGeocoder.geocode(any()) }
    }

    private fun item(id: String, lat: Double?, lng: Double?) = SearchResultItem(
        id = id,
        name = "Hotel $id",
        city = "Bogotá",
        country = "Colombia",
        maxCapacity = 4,
        priceFrom = "100",
        currency = "COP",
        latitude = lat,
        longitude = lng,
    )

    private fun response(items: List<SearchResultItem>) = SearchResponse(
        items = items,
        pagination = SearchPagination(
            total = items.size,
            page = 1,
            pageSize = items.size,
            totalPages = 1,
        ),
    )
}
