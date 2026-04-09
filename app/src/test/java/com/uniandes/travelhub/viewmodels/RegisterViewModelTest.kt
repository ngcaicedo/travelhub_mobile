package com.uniandes.travelhub.viewmodels

import app.cash.turbine.test
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.auth.RegisterRequest
import com.uniandes.travelhub.models.auth.UserResponse
import com.uniandes.travelhub.repositories.AuthException
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
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
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        repository = mockk()
        viewModel = RegisterViewModel(repository)
    }

    private fun fillTravelerForm() {
        viewModel.onRoleChange(UserRole.TRAVELER)
        viewModel.onFullNameChange("Ada Lovelace")
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPhoneChange("+573001234567")
        viewModel.onPasswordChange("Sup3rSecret!")
        viewModel.onTermsChange(true)
    }

    private fun fillHotelPartnerForm() {
        viewModel.onRoleChange(UserRole.HOTEL_PARTNER)
        viewModel.onHotelNameChange("Hotel Plaza")
        viewModel.onFullNameChange("Front Desk")
        viewModel.onEmailChange("front@hotelplaza.co")
        viewModel.onPhoneChange("+573009998877")
        viewModel.onPasswordChange("An0therOne!")
        viewModel.onTermsChange(true)
    }

    @Test
    fun `initial form has empty fields and traveler role`() {
        val state = viewModel.form.value
        assertEquals("", state.fullName)
        assertEquals("", state.email)
        assertEquals(UserRole.TRAVELER, state.role)
        assertEquals(false, state.agreedToTerms)
        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `submit without full name sets Error and never calls repository`() = runTest {
        fillTravelerForm()
        viewModel.onFullNameChange("")

        viewModel.onSubmit()

        assertEquals(
            RegisterUiState.Error("El nombre es obligatorio"),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `submit as hotel partner without hotel name sets Error`() = runTest {
        fillHotelPartnerForm()
        viewModel.onHotelNameChange("")

        viewModel.onSubmit()

        assertEquals(
            RegisterUiState.Error("El nombre del hotel es obligatorio"),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `submit with weak password sets Error`() = runTest {
        fillTravelerForm()
        viewModel.onPasswordChange("weak")

        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        val msg = (viewModel.uiState.value as RegisterUiState.Error).message
        assertTrue(msg.contains("contraseña"))
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `submit without accepting terms sets Error`() = runTest {
        fillTravelerForm()
        viewModel.onTermsChange(false)

        viewModel.onSubmit()

        assertEquals(
            RegisterUiState.Error("Debes aceptar los términos y condiciones"),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) { repository.register(any()) }
    }

    @Test
    fun `successful traveler submit sends payload without hotel_name and emits NavigateToLogin`() = runTest {
        val captured = slot<RegisterRequest>()
        coEvery { repository.register(capture(captured)) } returns Result.success(
            UserResponse("u_42", "ada@example.com", "+573001234567", "Ada Lovelace", null, 0)
        )
        fillTravelerForm()

        viewModel.events.test {
            viewModel.onSubmit()
            runCurrent()

            val event = awaitItem()
            assertTrue(event is RegisterEvent.NavigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
        val payload = captured.captured
        assertEquals(UserRole.TRAVELER, payload.role)
        assertEquals("Ada Lovelace", payload.fullName)
        assertNull(payload.hotelName)
    }

    @Test
    fun `submit transitions to Loading while register is in-flight`() = runTest {
        val gate = CompletableDeferred<Result<UserResponse>>()
        coEvery { repository.register(any()) } coAnswers { gate.await() }
        fillTravelerForm()

        viewModel.onSubmit()
        runCurrent()
        assertEquals(RegisterUiState.Loading, viewModel.uiState.value)

        gate.complete(
            Result.success(UserResponse("u_42", "ada@example.com", "+573001234567", "Ada Lovelace", null, 0))
        )
        runCurrent()
        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `successful hotel partner submit includes hotel_name in payload`() = runTest {
        val captured = slot<RegisterRequest>()
        coEvery { repository.register(capture(captured)) } returns Result.success(
            UserResponse("u_99", "front@hotelplaza.co", "+573009998877", "Front Desk", "Hotel Plaza", 0)
        )
        fillHotelPartnerForm()

        viewModel.onSubmit()
        runCurrent()

        val payload = captured.captured
        assertEquals(UserRole.HOTEL_PARTNER, payload.role)
        assertEquals("Hotel Plaza", payload.hotelName)
        assertEquals("Front Desk", payload.fullName)
    }

    @Test
    fun `failed submit ends in Error with parsed message`() = runTest {
        coEvery { repository.register(any()) } returns Result.failure(
            AuthException("El correo ya está registrado")
        )
        fillTravelerForm()

        viewModel.onSubmit()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals("El correo ya está registrado", (state as RegisterUiState.Error).message)
    }

    @Test
    fun `editing a field after Error clears the error back to Idle`() = runTest {
        coEvery { repository.register(any()) } returns Result.failure(AuthException("nope"))
        fillTravelerForm()
        viewModel.onSubmit()
        runCurrent()

        viewModel.onEmailChange("ada2@example.com")

        assertEquals(RegisterUiState.Idle, viewModel.uiState.value)
    }
}
