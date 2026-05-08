package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.pricing.HotelPricingApplyResponse
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingRevertResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.repositories.HotelPricingRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HotelPricingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: HotelPricingRepository = mockk()

    @Test
    fun `refresh preselects first target and loads history`() = runTest {
        coEvery { repository.listTargets() } returns Result.success(listOf(sampleTarget()))
        coEvery { repository.listHistory() } returns Result.success(listOf(sampleHistory()))

        val viewModel = HotelPricingViewModel(repository)
        advanceUntilIdle()

        assertEquals("rp-1", viewModel.form.value.selectedRatePlanId)
        assertEquals(1, viewModel.uiState.value.history.size)
        assertTrue(viewModel.uiState.value.targets.isNotEmpty())
    }

    @Test
    fun `preview success exposes preview response`() = runTest {
        coEvery { repository.listTargets() } returns Result.success(listOf(sampleTarget()))
        coEvery { repository.listHistory() } returns Result.success(emptyList())
        coEvery { repository.preview(any()) } returns Result.success(samplePreview())

        val viewModel = HotelPricingViewModel(repository)
        advanceUntilIdle()

        viewModel.preview()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.preview)
        assertEquals(184.0, viewModel.uiState.value.preview?.finalPrice)
    }

    @Test
    fun `apply success exposes success message`() = runTest {
        coEvery { repository.listTargets() } returns Result.success(listOf(sampleTarget()))
        coEvery { repository.listHistory() } returns Result.success(emptyList())
        coEvery { repository.preview(any()) } returns Result.success(samplePreview())
        coEvery { repository.apply(any()) } returns Result.success(
            HotelPricingApplyResponse(
                preview = samplePreview(),
                historyEntry = sampleHistory(),
            )
        )

        val viewModel = HotelPricingViewModel(repository)
        advanceUntilIdle()

        viewModel.preview()
        advanceUntilIdle()
        viewModel.apply(confirmationAcknowledged = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.success is ErrorMessage.Resource)
    }

    private fun sampleTarget() = HotelPricingTargetOption(
        propertyId = "p-1",
        propertyName = "Hotel Riviera",
        roomTypeId = "rt-1",
        roomTypeName = "Suite Presidencial",
        ratePlanId = "rp-1",
        ratePlanName = "Torre A",
        currency = "USD",
        basePrice = 245.0,
    )

    private fun samplePreview() = HotelPricingPreviewResponse(
        propertyId = "p-1",
        propertyName = "Hotel Riviera",
        roomTypeId = "rt-1",
        roomTypeName = "Suite Presidencial",
        ratePlanId = "rp-1",
        ratePlanName = "Torre A",
        currency = "USD",
        startDate = "2026-11-24",
        endDate = "2026-11-30",
        daysAffected = 7,
        currentBasePrice = 245.0,
        proposedBasePrice = 230.0,
        discountType = "percentage",
        discountValue = 20.0,
        finalPrice = 184.0,
        projectedRevenueBefore = 2205.0,
        projectedRevenueAfter = 1656.0,
        projectedRevenueDelta = -549.0,
        sellableUnits = 3,
        requiresConfirmation = true,
        impactSummary = "Impact summary",
    )

    private fun sampleHistory() = HotelPricingHistoryItem(
        id = "h-1",
        propertyId = "p-1",
        propertyName = "Hotel Riviera",
        roomTypeName = "Suite Presidencial",
        ratePlanName = "Torre A",
        currency = "USD",
        ruleName = "Black Friday",
        startDate = "2026-11-24",
        endDate = "2026-11-30",
        previousBasePrice = 245.0,
        newBasePrice = 230.0,
        discountType = "percentage",
        discountValue = 20.0,
        finalPrice = 184.0,
        projectedRevenueBefore = 2205.0,
        projectedRevenueAfter = 1656.0,
        actorUserId = "user-1",
        actorEmail = "hotel@travelhub.demo",
        deviceLabel = "Pixel 9",
        devicePlatform = "Android API 36",
        createdAt = "2026-05-06T12:00:00Z",
        revertedAt = null,
        canRevert = true,
    )
}
