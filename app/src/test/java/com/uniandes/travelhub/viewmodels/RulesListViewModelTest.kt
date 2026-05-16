package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
class RulesListViewModelTest {

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
    fun `derives percentage and integrity state for each rule`() = runTest {
        val property = property(price = 200.0)
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property))
        coEvery { pricingRepo.list("p1") } returns Result.success(
            listOf(
                rule("a", price = 160.0),                        // -20%
                rule("b", price = 200.0, locked = true),          // 0% but locked
                rule("c", price = 50.0, validFlag = false),       // -75%, compromised
            )
        )

        val vm = RulesListViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state is RulesListUiState.Ready)
        val rows = (state as RulesListUiState.Ready).rows.associateBy { it.rule.id }
        assertEquals(20, rows.getValue("a").derivedPercent)
        assertEquals(IntegrityState.Normal, rows.getValue("a").state)
        assertEquals(IntegrityState.Locked, rows.getValue("b").state)
        assertEquals(IntegrityState.Compromised, rows.getValue("c").state)
        assertEquals(75, rows.getValue("c").derivedPercent)
    }

    @Test
    fun `aggregates rules across all properties of the owner`() = runTest {
        val a = property(id = "p1", name = "Renaissance", price = 200.0)
        val b = property(id = "p2", name = "Beachfront", price = 100.0)
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns
            Result.success(listOf(a, b))
        coEvery { pricingRepo.list("p1") } returns Result.success(listOf(rule("r-a", price = 180.0)))
        coEvery { pricingRepo.list("p2") } returns Result.success(listOf(rule("r-b", price = 90.0)))

        val vm = RulesListViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        val state = vm.uiState.value as RulesListUiState.Ready
        assertEquals(2, state.rows.size)
        val byRule = state.rows.associateBy { it.rule.id }
        assertEquals("Renaissance", byRule.getValue("r-a").property.name)
        assertEquals("Beachfront", byRule.getValue("r-b").property.name)
    }

    @Test
    fun `empty rules surfaces Empty state`() = runTest {
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(listOf(property()))
        coEvery { pricingRepo.list(any()) } returns Result.success(emptyList())

        val vm = RulesListViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        assertEquals(RulesListUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `no properties surfaces NoProperties state`() = runTest {
        coEvery { propertiesRepo.getPropertiesByOwner(adminId) } returns Result.success(emptyList())

        val vm = RulesListViewModel(propertiesRepo, pricingRepo, tokenStore)
        runCurrent()

        assertEquals(RulesListUiState.NoProperties, vm.uiState.value)
    }

    private fun property(
        id: String = "p1",
        name: String = "Property",
        price: Double = 100.0,
    ) = Property(
        id = id,
        name = name,
        description = "",
        location = "x",
        pricePerNight = price,
        currency = "COP",
        rating = 0.0,
        reviewCount = 0,
    )

    private fun rule(
        id: String,
        price: Double,
        locked: Boolean = false,
        validFlag: Boolean = true,
    ) = SeasonalPricingResponse(
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
        integrityValid = validFlag,
    )

    private fun fakeJwtFor(sub: String): String {
        val payload = "{\"sub\":\"$sub\"}"
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray())
        return "h.$encoded.s"
    }
}
