package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.pricing.HotelPricingApplyRequest
import com.uniandes.travelhub.models.pricing.HotelPricingApplyResponse
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewRequest
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingRevertResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.network.HotelPricingApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HotelPricingRepositoryTest {

    private val api: HotelPricingApi = mockk()

    @Test
    fun `listTargets returns backend options`() = runTest {
        val expected = listOf(
            HotelPricingTargetOption(
                propertyId = "p-1",
                propertyName = "Hotel Riviera",
                roomTypeId = "rt-1",
                roomTypeName = "Suite Presidencial",
                ratePlanId = "rp-1",
                ratePlanName = "Torre A",
                currency = "USD",
                basePrice = 245.0,
            )
        )
        coEvery { api.listTargets() } returns expected

        val result = HotelPricingRepository(api).listTargets()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `apply forwards device metadata`() = runTest {
        val captured = slot<HotelPricingApplyRequest>()
        val capturedChecksum = slot<String>()
        coEvery { api.apply(capture(capturedChecksum), capture(captured)) } returns HotelPricingApplyResponse(
            preview = HotelPricingPreviewResponse(
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
            ),
            historyEntry = HotelPricingHistoryItem(
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
        )

        val payload = HotelPricingApplyRequest(
            propertyId = "p-1",
            ratePlanId = "rp-1",
            startDate = "2026-11-24",
            endDate = "2026-11-30",
            proposedBasePrice = 230.0,
            discountType = "percentage",
            discountValue = 20.0,
            ruleName = "Black Friday",
            confirmationAcknowledged = true,
            deviceLabel = "Pixel 9",
            devicePlatform = "Android API 36",
        )

        val result = HotelPricingRepository(api).apply(payload)

        assertTrue(result.isSuccess)
        assertEquals("Pixel 9", captured.captured.deviceLabel)
        assertEquals("Android API 36", captured.captured.devicePlatform)
        assertTrue(capturedChecksum.captured.isNotBlank())
        coVerify(exactly = 1) { api.apply(any(), any()) }
    }

    @Test
    fun `preview sends checksum header`() = runTest {
        val payload = HotelPricingPreviewRequest(
            propertyId = "p-1",
            ratePlanId = "rp-1",
            startDate = "2026-11-24",
            endDate = "2026-11-30",
            proposedBasePrice = 230.0,
            discountType = "percentage",
            discountValue = 20.0,
            ruleName = "Black Friday",
        )
        val capturedChecksum = slot<String>()
        coEvery { api.preview(capture(capturedChecksum), payload) } returns HotelPricingPreviewResponse(
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

        val result = HotelPricingRepository(api).preview(payload)

        assertTrue(result.isSuccess)
        assertTrue(capturedChecksum.captured.isNotBlank())
        coVerify(exactly = 1) { api.preview(any(), payload) }
    }

    @Test
    fun `revert returns success when api succeeds`() = runTest {
        coEvery { api.revert("h-1") } returns HotelPricingRevertResponse(
            revertedChangeId = "h-1",
            revertedAt = "2026-05-06T13:00:00Z",
        )

        val result = HotelPricingRepository(api).revert("h-1")

        assertTrue(result.isSuccess)
        assertEquals("h-1", result.getOrNull()?.revertedChangeId)
    }
}
