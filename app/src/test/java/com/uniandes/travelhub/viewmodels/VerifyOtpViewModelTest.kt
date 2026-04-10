package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.repositories.AuthException
import com.uniandes.travelhub.repositories.AuthRepository
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
class VerifyOtpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: VerifyOtpViewModel

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = VerifyOtpViewModel(repository, email = "ada@example.com")
    }

    @Test
    fun `initial state is Idle with empty otp`() {
        assertEquals(VerifyOtpUiState.Idle, viewModel.uiState.value)
        assertEquals("", viewModel.otpCode.value)
    }

    @Test
    fun `onOtpChange filters non-digits and clamps to 6 chars`() {
        viewModel.onOtpChange("12 34a567890")
        assertEquals("123456", viewModel.otpCode.value)
    }

    @Test
    fun `submit with short OTP sets Error and never calls repository`() = runTest {
        viewModel.onOtpChange("123")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state is VerifyOtpUiState.Error)
        assertEquals(
            ErrorMessage.Resource(R.string.auth_verify_otp_invalid),
            (state as VerifyOtpUiState.Error).message
        )
        coVerify(exactly = 0) { repository.verifyOtp(any(), any()) }
    }

    @Test
    fun `successful submit ends in Idle and emits NavigateToHome with role`() = runTest {
        coEvery { repository.verifyOtp("ada@example.com", "123456") } returns
            Result.success(UserRole.HOTEL_PARTNER)
        viewModel.onOtpChange("123456")

        viewModel.events.test {
            viewModel.onSubmit()
            runCurrent()

            val event = awaitItem()
            assertTrue(event is VerifyOtpEvent.NavigateToHome)
            assertEquals(UserRole.HOTEL_PARTNER, (event as VerifyOtpEvent.NavigateToHome).role)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(VerifyOtpUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `submit transitions to Loading while the repository is in-flight`() = runTest {
        val gate = CompletableDeferred<Result<UserRole>>()
        coEvery { repository.verifyOtp(any(), any()) } coAnswers { gate.await() }
        viewModel.onOtpChange("123456")

        viewModel.onSubmit()
        runCurrent()
        assertEquals(VerifyOtpUiState.Loading, viewModel.uiState.value)

        gate.complete(Result.success(UserRole.TRAVELER))
        runCurrent()
        assertEquals(VerifyOtpUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `failed submit ends in Error with message from AuthException`() = runTest {
        coEvery { repository.verifyOtp(any(), any()) } returns
            Result.failure(AuthException("Código incorrecto"))
        viewModel.onOtpChange("000000")

        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is VerifyOtpUiState.Error)
        assertEquals(
            ErrorMessage.Plain("Código incorrecto"),
            (state as VerifyOtpUiState.Error).message
        )
    }

    @Test
    fun `editing OTP after error clears the error back to Idle`() = runTest {
        coEvery { repository.verifyOtp(any(), any()) } returns Result.failure(AuthException("bad"))
        viewModel.onOtpChange("000000")
        viewModel.onSubmit()
        runCurrent()

        viewModel.onOtpChange("000001")

        assertEquals(VerifyOtpUiState.Idle, viewModel.uiState.value)
    }
}
