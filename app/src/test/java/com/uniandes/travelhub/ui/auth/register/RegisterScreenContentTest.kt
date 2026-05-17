package com.uniandes.travelhub.ui.auth.register

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.viewmodels.RegisterFormState
import com.uniandes.travelhub.viewmodels.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class RegisterScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        uiState: RegisterUiState = RegisterUiState.Idle,
        form: RegisterFormState = RegisterFormState(),
        onRoleChange: (UserRole) -> Unit = {},
        onFullNameChange: (String) -> Unit = {},
        onHotelNameChange: (String) -> Unit = {},
        onEmailChange: (String) -> Unit = {},
        onCountryCodeChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onNavigateToLogin: () -> Unit = {},
    ) {
        composeRule.setContent {
            TravelhubTheme {
                RegisterScreenContent(
                    uiState = uiState,
                    form = form,
                    currentLocale = "es",
                    onRoleChange = onRoleChange,
                    onFullNameChange = onFullNameChange,
                    onHotelNameChange = onHotelNameChange,
                    onEmailChange = onEmailChange,
                    onPhoneChange = {},
                    onCountryCodeChange = onCountryCodeChange,
                    onPasswordChange = {},
                    onTermsChange = {},
                    onSubmit = onSubmit,
                    onNavigateToLogin = onNavigateToLogin,
                    onLocaleChange = {},
                )
            }
        }
    }

    @Test
    fun `traveler tab shows full name and hides hotel name field`() {
        setContent(form = RegisterFormState(role = UserRole.TRAVELER))

        composeRule.onNodeWithText("Crea tu cuenta").assertExists()
        composeRule.onNodeWithText("Nombre completo").assertExists()
        composeRule.onAllNodesWithText("Nombre del hotel").assertCountEquals(0)
    }

    @Test
    fun `host tab shows hotel name field`() {
        setContent(form = RegisterFormState(role = UserRole.HOTEL_PARTNER))

        composeRule.onNodeWithText("Nombre del hotel").assertExists()
        composeRule.onNodeWithText("Nombre de contacto").assertExists()
    }

    @Test
    fun `tapping host tab invokes onRoleChange with HOTEL_PARTNER`() {
        var captured: UserRole? = null
        setContent(onRoleChange = { captured = it })

        composeRule.onNodeWithText("Anfitrión").performScrollTo().performClick()

        assertEquals(UserRole.HOTEL_PARTNER, captured)
    }

    @Test
    fun `error state shows backend message in banner`() {
        setContent(uiState = RegisterUiState.Error(ErrorMessage.Plain("Correo electrónico inválido")))

        composeRule.onNodeWithText("Correo electrónico inválido").assertExists()
    }

    @Test
    fun `submit click invokes onSubmit callback`() {
        var submitted = false
        setContent(onSubmit = { submitted = true })

        composeRule.onNodeWithText("Crear cuenta").performScrollTo().performClick()

        assertTrue("onSubmit should have been invoked", submitted)
    }

    @Test
    fun `typing in email field forwards text to onEmailChange`() {
        var captured = ""
        setContent(onEmailChange = { captured = it })

        composeRule
            .onNodeWithText("ejemplo@correo.com")
            .performScrollTo()
            .performTextInput("ada@example.com")

        assertEquals("ada@example.com", captured)
    }

    @Test
    fun `tapping sign in link invokes onNavigateToLogin`() {
        var navigated = false
        setContent(onNavigateToLogin = { navigated = true })

        composeRule
            .onNodeWithText("Inicia sesión", substring = true)
            .performScrollTo()
            .performClick()

        assertTrue("onNavigateToLogin should have been invoked", navigated)
    }
}
