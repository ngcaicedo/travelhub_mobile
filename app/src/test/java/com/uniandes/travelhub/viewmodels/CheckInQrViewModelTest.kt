package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import com.uniandes.travelhub.models.reservations.CheckInQrArtifact
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInQrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ReservationsRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    @Test
    fun `load exposes available qr when repository succeeds`() = runTest {
        coEvery { repository.getCheckInQr("r-1") } returns Result.success(
            CheckInQrArtifact(
                cache = sampleCache(),
                isOffline = false,
            )
        )

        val viewModel = CheckInQrViewModel("r-1", repository)
        runCurrent()

        assertTrue(viewModel.uiState.value is CheckInQrUiState.Available)
    }

    @Test
    fun `load exposes invalidated state when repository marks qr stale`() = runTest {
        coEvery { repository.getCheckInQr("r-1") } returns Result.success(
            CheckInQrArtifact(
                cache = sampleCache(),
                isOffline = false,
                requiresRefresh = true,
            )
        )

        val viewModel = CheckInQrViewModel("r-1", repository)
        runCurrent()

        assertTrue(viewModel.uiState.value is CheckInQrUiState.Invalidated)
    }

    private fun sampleCache() = CachedCheckInQr(
        reservationId = "r-1",
        reservationStatus = "confirmed",
        propertyName = "Grand Hotel Riviera",
        propertyCoverImageUrl = null,
        checkInDate = "2026-06-10",
        checkOutDate = "2026-06-12",
        numberOfGuests = 2,
        reservationFingerprint = "confirmed|2026-06-10|2026-06-12|2",
        encryptedPayload = "thci1.sample",
        cachedAtEpochMs = 1L,
        holderEmail = "ada@example.com",
        travelerId = "user-42",
    )
}
