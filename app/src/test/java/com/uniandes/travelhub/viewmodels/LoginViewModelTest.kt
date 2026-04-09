package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = LoginViewModel(repository)
    }

    @Test
    fun `initial state is Idle with empty fields`() = runTest {
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        assertEquals("", viewModel.email.value)
        assertEquals("", viewModel.password.value)
    }

    @Test
    fun `submit with invalid email sets Error and never calls repository`() = runTest {
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("Sup3rSecret!")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("Correo electrónico inválido", (state as LoginUiState.Error).message)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `submit with blank password sets Error`() = runTest {
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("   ")

        viewModel.onSubmit()

        // Note: blank password also makes the email/password combo invalid; we
        // only assert the error state, not the precise message order, since the
        // password check happens after the email check.
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `successful submit ends in Idle and emits NavigateToOtp`() = runTest {
        coEvery { repository.login("ada@example.com", "Sup3rSecret!") } returns Result.success(Unit)
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("Sup3rSecret!")

        viewModel.events.test {
            viewModel.onSubmit()
            runCurrent()

            val event = awaitItem()
            assertTrue(event is LoginEvent.NavigateToOtp)
            assertEquals("ada@example.com", (event as LoginEvent.NavigateToOtp).email)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
        coVerify(exactly = 1) { repository.login("ada@example.com", "Sup3rSecret!") }
    }

    @Test
    fun `submit transitions to Loading while the repository is in-flight then back to Idle on success`() = runTest {
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { repository.login(any(), any()) } coAnswers { gate.await() }
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("Sup3rSecret!")

        viewModel.onSubmit()
        runCurrent()
        // The launched coroutine is suspended inside repository.login → state must be Loading.
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        gate.complete(Result.success(Unit))
        runCurrent()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `failed submit ends in Error with message from AuthException`() = runTest {
        coEvery { repository.login(any(), any()) } returns Result.failure(
            AuthException("Credenciales inválidas")
        )
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("Sup3rSecret!")

        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("Credenciales inválidas", (state as LoginUiState.Error).message)
    }

    @Test
    fun `editing a field after Error clears the error back to Idle`() = runTest {
        coEvery { repository.login(any(), any()) } returns Result.failure(AuthException("nope"))
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("Sup3rSecret!")
        viewModel.onSubmit()
        // let the failure propagate
        runCurrent()

        viewModel.onPasswordChange("DifferentP4ss!")

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `email is trimmed before validation and submission`() = runTest {
        coEvery { repository.login("ada@example.com", "Sup3rSecret!") } returns Result.success(Unit)

        viewModel.onEmailChange("  ada@example.com  ")
        viewModel.onPasswordChange("Sup3rSecret!")
        viewModel.onSubmit()
        runCurrent()

        coVerify(exactly = 1) { repository.login("ada@example.com", "Sup3rSecret!") }
    }
}
