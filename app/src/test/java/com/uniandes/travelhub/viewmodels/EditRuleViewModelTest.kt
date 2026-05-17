package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingPatchPayload
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditRuleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var propertiesRepo: PropertiesRepository
    private lateinit var pricingRepo: SeasonalPricingRepository

    @Before
    fun setUp() {
        propertiesRepo = mockk()
        pricingRepo = mockk()
    }

    @Test
    fun `loads rule and seeds Ready state`() = runTest {
        coEvery { propertiesRepo.getPropertyDetail("p1") } returns Result.success(property())
        coEvery { pricingRepo.list("p1") } returns Result.success(listOf(rule("r1")))

        val vm = EditRuleViewModel(propertiesRepo, pricingRepo, "p1", "r1")
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state is EditRuleUiState.Ready)
        val ready = state as EditRuleUiState.Ready
        assertEquals("r1", ready.rule.id)
        assertTrue(ready.canEdit)
    }

    @Test
    fun `locked rule disables edit`() = runTest {
        coEvery { propertiesRepo.getPropertyDetail("p1") } returns Result.success(property())
        coEvery { pricingRepo.list("p1") } returns Result.success(listOf(rule("r1", locked = true)))

        val vm = EditRuleViewModel(propertiesRepo, pricingRepo, "p1", "r1")
        runCurrent()

        val ready = vm.uiState.value as EditRuleUiState.Ready
        assertTrue(!ready.canEdit)
    }

    @Test
    fun `partial update only sends fields that changed`() = runTest {
        coEvery { propertiesRepo.getPropertyDetail("p1") } returns Result.success(property(price = 200.0))
        coEvery { pricingRepo.list("p1") } returns Result.success(listOf(rule("r1", price = 160.0)))
        val patchSlot = slot<SeasonalPricingPatchPayload>()
        coEvery { pricingRepo.update("p1", "r1", capture(patchSlot)) } returns Result.success(rule("r1"))

        val vm = EditRuleViewModel(propertiesRepo, pricingRepo, "p1", "r1")
        runCurrent()

        // Only the start date changes — percent and end date stay the same.
        vm.submitUpdate(newPercent = null, newSeasonStart = "2026-07-01", newSeasonEnd = null)
        runCurrent()

        coVerify(exactly = 1) { pricingRepo.update("p1", "r1", any()) }
        assertEquals("2026-07-01", patchSlot.captured.seasonStart)
        assertNull(patchSlot.captured.seasonEnd)
        assertNull(patchSlot.captured.pricePerNight)
    }

    @Test
    fun `update with high impact pct emits confirmation`() = runTest {
        coEvery { propertiesRepo.getPropertyDetail("p1") } returns Result.success(property(price = 200.0))
        coEvery { pricingRepo.list("p1") } returns Result.success(listOf(rule("r1", price = 180.0)))

        val vm = EditRuleViewModel(propertiesRepo, pricingRepo, "p1", "r1")
        runCurrent()

        vm.submitUpdate(newPercent = 50.0, newSeasonStart = null, newSeasonEnd = null)
        runCurrent()

        coVerify(exactly = 0) { pricingRepo.update(any(), any(), any()) }
    }

    private fun property(price: Double = 100.0) = Property(
        id = "p1",
        name = "p",
        description = "",
        location = "x",
        pricePerNight = price,
        currency = "COP",
        rating = 0.0,
        reviewCount = 0,
    )

    private fun rule(id: String, price: Double = 80.0, locked: Boolean = false) =
        SeasonalPricingResponse(
            id = id,
            propertyId = "p1",
            seasonStart = "2026-06-01",
            seasonEnd = "2026-06-15",
            pricePerNight = price,
            currency = "COP",
            taxRate = 0.0,
            cleaningFee = 0.0,
            signatureHash = "h",
            signatureAlgo = "HMAC-SHA256",
            integrityLocked = locked,
            integrityCheckedAt = null,
            createdAt = "2026-05-15T00:00:00Z",
            updatedAt = "2026-05-15T00:00:00Z",
            integrityValid = true,
        )
}
