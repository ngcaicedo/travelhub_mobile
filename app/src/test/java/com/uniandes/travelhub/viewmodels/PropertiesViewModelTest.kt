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
class PropertiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PropertiesRepository
    private lateinit var viewModel: PropertiesViewModel

    @Before
    fun setUp() {
        repository = mockk()
    }

    private fun createViewModel(): PropertiesViewModel {
        return PropertiesViewModel(repository).also { viewModel = it }
    }

    @Test
    fun `init triggers loadProperties and transitions to Success`() = runTest {
        val properties = listOf(sampleProperty("1"))
        coEvery { repository.getProperties() } returns Result.success(properties)

        createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyListUiState.Success)
        assertEquals(properties, (state as PropertyListUiState.Success).properties)
        coVerify(exactly = 1) { repository.getProperties() }
    }

    @Test
    fun `loadProperties success with empty list transitions to Success with empty list`() = runTest {
        coEvery { repository.getProperties() } returns Result.success(emptyList())

        createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyListUiState.Success)
        assertEquals(emptyList<Property>(), (state as PropertyListUiState.Success).properties)
    }

    @Test
    fun `loadProperties failure transitions to Error with message`() = runTest {
        coEvery { repository.getProperties() } returns Result.failure(
            Exception("network error")
        )

        createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyListUiState.Error)
        assertEquals(
            ErrorMessage.Plain("network error"),
            (state as PropertyListUiState.Error).message
        )
    }

    @Test
    fun `retry after error transitions Error to Loading to Success`() = runTest {
        coEvery { repository.getProperties() } returns Result.failure(Exception("fail"))
        createViewModel()
        runCurrent()
        assertTrue(viewModel.uiState.value is PropertyListUiState.Error)

        val properties = listOf(sampleProperty("1"))
        coEvery { repository.getProperties() } returns Result.success(properties)

        viewModel.loadProperties()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is PropertyListUiState.Success)
        assertEquals(properties, (state as PropertyListUiState.Success).properties)
    }

    @Test
    fun `loading state is emitted while repository call is in-flight`() = runTest {
        val gate = CompletableDeferred<Result<List<Property>>>()
        coEvery { repository.getProperties() } coAnswers { gate.await() }

        createViewModel()
        runCurrent()
        assertEquals(PropertyListUiState.Loading, viewModel.uiState.value)

        gate.complete(Result.success(listOf(sampleProperty("1"))))
        runCurrent()
        assertTrue(viewModel.uiState.value is PropertyListUiState.Success)
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
