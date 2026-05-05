package com.uniandes.travelhub.viewmodels

import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.SearchPagination
import com.uniandes.travelhub.models.search.SearchQuery
import com.uniandes.travelhub.models.search.SearchResponse
import com.uniandes.travelhub.repositories.SearchException
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: SearchRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        repository = mockk()
        every { repository.lastResult() } returns null
        viewModel = SearchViewModel(repository)
    }

    @Test
    fun `submit with empty form populates validation errors and skips network call`() = runTest {
        viewModel.submit()

        val form = viewModel.form.value
        assertEquals(R.string.search_error_city_required, (form.validation.cityError as ErrorMessage.Resource).id)
        assertEquals(R.string.search_error_check_in_required, (form.validation.checkInError as ErrorMessage.Resource).id)
        assertEquals(R.string.search_error_check_out_required, (form.validation.checkOutError as ErrorMessage.Resource).id)
        coVerify(exactly = 0) { repository.search(any()) }
    }

    @Test
    fun `submit with checkout before checkin flags only checkout`() = runTest {
        viewModel.onCityChange("Bogotá")
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-05")

        viewModel.submit()

        val form = viewModel.form.value
        assertEquals(null, form.validation.cityError)
        assertEquals(null, form.validation.checkInError)
        assertEquals(R.string.search_error_check_out_after_check_in, (form.validation.checkOutError as ErrorMessage.Resource).id)
    }

    @Test
    fun `successful submit emits Success and stores response`() = runTest {
        val response = SearchResponse(
            items = emptyList(),
            pagination = SearchPagination(total = 0, page = 1, pageSize = 8, totalPages = 0),
            emptyState = emptyList(),
        )
        coEvery { repository.search(any()) } returns Result.success(response)

        viewModel.onCityChange("Bogotá")
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")
        viewModel.onGuestsChange(2)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.results.value is SearchResultsState.Success)
        assertEquals(response, (viewModel.results.value as SearchResultsState.Success).response)
        coVerify {
            repository.search(match<SearchQuery> { it.city == "Bogotá" && it.guests == 2 && it.page == 1 })
        }
    }

    @Test
    fun `failed submit surfaces a Plain error message from the exception`() = runTest {
        coEvery { repository.search(any()) } returns Result.failure(SearchException("backend down"))

        viewModel.onCityChange("Bogotá")
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.results.value
        assertTrue(state is SearchResultsState.Error)
        assertEquals(ErrorMessage.Plain("backend down"), (state as SearchResultsState.Error).message)
    }

    @Test
    fun `loadNextPage requests next page when not on last page`() = runTest {
        val firstPage = SearchResponse(
            items = emptyList(),
            pagination = SearchPagination(total = 16, page = 1, pageSize = 8, totalPages = 2),
        )
        val secondPage = firstPage.copy(pagination = firstPage.pagination.copy(page = 2))
        coEvery { repository.search(match<SearchQuery> { it.page == 1 }) } returns Result.success(firstPage)
        coEvery { repository.search(match<SearchQuery> { it.page == 2 }) } returns Result.success(secondPage)

        viewModel.onCityChange("Bogotá")
        viewModel.onCheckInChange("2026-06-10")
        viewModel.onCheckOutChange("2026-06-15")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, ((viewModel.results.value as SearchResultsState.Success).response.pagination.page))
    }
}
