package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PropertyDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PropertiesRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    private fun createViewModel(propertyId: String = "42"): PropertyDetailViewModel {
        return PropertyDetailViewModel(repository, propertyId)
    }

    @Test
    fun `init triggers loadPropertyDetail with correct id`() = runTest {
        val property = sampleProperty("42")
        coEvery { repository.getCachedProperty("42") } returns null
        coEvery { repository.getPropertyDetail("42") } returns Result.success(property)

        val viewModel = createViewModel("42")
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyDetailUiState.Success)
        assertEquals(property, (state as PropertyDetailUiState.Success).property)
        coVerify(exactly = 1) { repository.getPropertyDetail("42") }
    }

    @Test
    fun `loadPropertyDetail failure transitions to Error with message`() = runTest {
        coEvery { repository.getCachedProperty(any()) } returns null
        coEvery { repository.getPropertyDetail(any()) } returns Result.failure(
            Exception("not found")
        )

        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyDetailUiState.Error)
        assertEquals(
            ErrorMessage.Plain("not found"),
            (state as PropertyDetailUiState.Error).message
        )
    }

    @Test
    fun `retry after error reloads with same propertyId`() = runTest {
        coEvery { repository.getCachedProperty("42") } returns null
        coEvery { repository.getPropertyDetail("42") } returns Result.failure(Exception("fail"))

        val viewModel = createViewModel("42")
        runCurrent()
        assertTrue(viewModel.uiState.value is PropertyDetailUiState.Error)

        val property = sampleProperty("42")
        coEvery { repository.getPropertyDetail("42") } returns Result.success(property)

        viewModel.loadPropertyDetail()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyDetailUiState.Success)
        assertEquals(property, (state as PropertyDetailUiState.Success).property)
        coVerify(exactly = 2) { repository.getPropertyDetail("42") }
    }

    @Test
    fun `loading state is emitted while repository call is in-flight`() = runTest {
        val gate = CompletableDeferred<Result<Property>>()
        coEvery { repository.getCachedProperty(any()) } returns null
        coEvery { repository.getPropertyDetail(any()) } coAnswers { gate.await() }

        val viewModel = createViewModel()
        runCurrent()
        assertEquals(PropertyDetailUiState.Loading, viewModel.uiState.value)

        gate.complete(Result.success(sampleProperty("42")))
        runCurrent()
        assertTrue(viewModel.uiState.value is PropertyDetailUiState.Success)
    }

    @Test
    fun `cached property is shown immediately while detail refreshes`() = runTest {
        val cached = sampleProperty("42")
        val refreshed = cached.copy(description = "Updated description")
        coEvery { repository.getCachedProperty("42") } returns cached
        coEvery { repository.getPropertyDetail("42") } returns Result.success(refreshed)

        val viewModel = createViewModel("42")
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyDetailUiState.Success)
        assertEquals(refreshed, (state as PropertyDetailUiState.Success).property)
        assertEquals(false, state.isRefreshing)
    }

    private fun sampleProperty(id: String) = Property(
        id = id,
        name = "Test Property $id",
        description = "A test property",
        location = "Bogotá",
        latitude = 4.6,
        longitude = -74.1,
        pricePerNight = 100.0,
        currency = "COP",
        rating = 4.5,
        reviewCount = 10,
        bedrooms = 2,
        bathrooms = 1.0,
        maxGuests = 4,
        amenities = listOf("WiFi"),
        images = emptyList(),
        reviews = emptyList(),
        status = 1
    )
}
