package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingWritePayload
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerPricingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var propertiesRepo: PropertiesRepository
    private lateinit var pricingRepo: SeasonalPricingRepository
    private lateinit var tokenStore: AuthTokenStore

    private val adminId = "11111111-1111-1111-1111-111111111111"

    @Before
    fun setUp() {
        propertiesRepo = mockk()
        pricingRepo = mockk()
        tokenStore = mockk()
        every { tokenStore.tokenFlow } returns flowOf(fakeJwtFor(adminId))
    }

    @Test
    fun `init with N properties sets first as selected and loads rules`() = runTest {
        val properties = listOf(sampleProperty("p1"), sampleProperty("p2"))
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(properties)
        coEvery { pricingRepo.list("p1") } returns Result.success(emptyList())

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state is PartnerPricingUiState.Ready)
        val ready = state as PartnerPricingUiState.Ready
        assertEquals("p1", ready.selectedPropertyId)
        assertEquals(2, ready.properties.size)
        coVerify { pricingRepo.list("p1") }
    }

    @Test
    fun `init with zero properties shows Empty state`() = runTest {
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(emptyList())

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        assertTrue(vm.uiState.value is PartnerPricingUiState.Empty)
    }

    @Test
    fun `submitNewDiscount valid percent posts price computed from base`() = runTest {
        val property = sampleProperty("p1", price = 200.0)
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property))
        coEvery { pricingRepo.list("p1") } returns Result.success(emptyList())
        val payloadSlot = slot<SeasonalPricingWritePayload>()
        coEvery { pricingRepo.create("p1", capture(payloadSlot)) } returns Result.success(
            sampleRule(price = 160.0)
        )

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        vm.submitNewDiscount(percent = 20.0, seasonStart = "2026-06-01", seasonEnd = "2026-06-15")
        runCurrent()

        coVerify(exactly = 1) { pricingRepo.create("p1", any()) }
        // price = 200 × (1 - 0.20) = 160
        assertEquals(160.0, payloadSlot.captured.pricePerNight, 0.001)
    }

    @Test
    fun `submitNewDiscount above threshold emits high impact confirmation`() = runTest {
        val property = sampleProperty("p1", price = 100.0)
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property))
        coEvery { pricingRepo.list("p1") } returns Result.success(emptyList())

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        vm.submitNewDiscount(percent = 35.0, seasonStart = "2026-06-01", seasonEnd = "2026-06-15")
        runCurrent()

        // No create call until the user confirms.
        coVerify(exactly = 0) { pricingRepo.create(any(), any()) }
    }

    @Test
    fun `submitNewDiscount rejects invalid percent`() = runTest {
        val property = sampleProperty("p1")
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property))
        coEvery { pricingRepo.list("p1") } returns Result.success(emptyList())

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        vm.submitNewDiscount(percent = 150.0, seasonStart = "2026-06-01", seasonEnd = "2026-06-15")
        runCurrent()

        coVerify(exactly = 0) { pricingRepo.create(any(), any()) }
    }

    @Test
    fun `submitNewDiscount rejects bad date range`() = runTest {
        val property = sampleProperty("p1")
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property))
        coEvery { pricingRepo.list("p1") } returns Result.success(emptyList())

        val vm = PartnerPricingViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        vm.submitNewDiscount(percent = 10.0, seasonStart = "2026-06-15", seasonEnd = "2026-06-01")
        runCurrent()

        coVerify(exactly = 0) { pricingRepo.create(any(), any()) }
    }

    private fun fakeJwtFor(sub: String): String {
        // Minimal token with a valid base64url payload containing {"sub":"<id>"}.
        val payload = "{\"sub\":\"$sub\"}"
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray())
        return "header.$encoded.signature"
    }

    private fun sampleProperty(id: String, price: Double = 100.0): Property = Property(
        id = id,
        name = "Property $id",
        description = "",
        location = "Bogotá",
        pricePerNight = price,
        currency = "COP",
        rating = 0.0,
        reviewCount = 0,
    )

    private fun sampleRule(price: Double): SeasonalPricingResponse = SeasonalPricingResponse(
        id = "sp-x",
        propertyId = "p1",
        seasonStart = "2026-06-01",
        seasonEnd = "2026-06-15",
        pricePerNight = price,
        currency = "COP",
        taxRate = 0.0,
        cleaningFee = 0.0,
        signatureHash = "h",
        signatureAlgo = "HMAC-SHA256",
        integrityLocked = false,
        integrityCheckedAt = null,
        createdAt = "2026-05-15T00:00:00Z",
        updatedAt = "2026-05-15T00:00:00Z",
        integrityValid = true,
    )
}
