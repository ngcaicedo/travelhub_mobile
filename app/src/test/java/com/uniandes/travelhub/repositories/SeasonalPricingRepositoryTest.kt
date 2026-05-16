package com.uniandes.travelhub.repositories

import com.uniandes.travelhub.models.properties.SeasonalPricingListResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingPatchPayload
import com.uniandes.travelhub.models.properties.SeasonalPricingResponse
import com.uniandes.travelhub.models.properties.SeasonalPricingWritePayload
import com.uniandes.travelhub.network.PropertiesApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SeasonalPricingRepositoryTest {

    private lateinit var api: PropertiesApi
    private lateinit var repository: SeasonalPricingRepository

    @Before
    fun setUp() {
        api = mockk()
        repository = SeasonalPricingRepository(api = api, parseDetail = { it.message })
    }

    @Test
    fun `list success returns items`() = runTest {
        val item = sampleResponse("sp-1")
        coEvery { api.listSeasonalPricing("prop-1") } returns
            SeasonalPricingListResponse(items = listOf(item), total = 1)

        val result = repository.list("prop-1")

        assertTrue(result.isSuccess)
        assertEquals(listOf(item), result.getOrNull())
    }

    @Test
    fun `create success returns rule`() = runTest {
        val payload = sampleWritePayload()
        val expected = sampleResponse("sp-2")
        coEvery { api.createSeasonalPricing("prop-1", payload) } returns expected

        val result = repository.create("prop-1", payload)

        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `update success returns rule`() = runTest {
        val patch = SeasonalPricingPatchPayload(pricePerNight = 99.0)
        val expected = sampleResponse("sp-3")
        coEvery { api.updateSeasonalPricing("prop-1", "sp-3", patch) } returns expected

        val result = repository.update("prop-1", "sp-3", patch)

        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `update 423 surfaces SeasonalPricingLockedException`() = runTest {
        coEvery { api.updateSeasonalPricing(any(), any(), any()) } throws httpException(423, "locked")

        val result = repository.update("prop-1", "sp-3", SeasonalPricingPatchPayload(pricePerNight = 10.0))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is SeasonalPricingLockedException)
    }

    @Test
    fun `network error is wrapped with parsed detail`() = runTest {
        coEvery { api.listSeasonalPricing("prop-1") } throws IOException("offline")

        val result = repository.list("prop-1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    private fun httpException(code: Int, body: String): HttpException {
        val response = Response.error<Any>(
            code,
            body.toResponseBody("application/json".toMediaType()),
        )
        return HttpException(response)
    }

    private fun sampleResponse(id: String) = SeasonalPricingResponse(
        id = id,
        propertyId = "prop-1",
        seasonStart = "2026-06-01",
        seasonEnd = "2026-08-31",
        pricePerNight = 100.0,
        currency = "COP",
        taxRate = 0.0,
        cleaningFee = 0.0,
        signatureHash = "abc",
        signatureAlgo = "HMAC-SHA256",
        integrityLocked = false,
        integrityCheckedAt = null,
        createdAt = "2026-05-15T00:00:00Z",
        updatedAt = "2026-05-15T00:00:00Z",
        integrityValid = true,
    )

    private fun sampleWritePayload() = SeasonalPricingWritePayload(
        seasonStart = "2026-06-01",
        seasonEnd = "2026-08-31",
        pricePerNight = 80.0,
        currency = "COP",
        taxRate = 0.0,
        cleaningFee = 0.0,
    )
}
