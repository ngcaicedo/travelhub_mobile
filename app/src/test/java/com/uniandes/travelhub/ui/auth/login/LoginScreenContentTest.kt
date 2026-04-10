package com.uniandes.travelhub.ui.auth.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.viewmodels.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class LoginScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        uiState: LoginUiState = LoginUiState.Idle,
        email: String = "",
        password: String = "",
        onEmailChange: (String) -> Unit = {},
        onPasswordChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onNavigateToRegister: () -> Unit = {},
    ) {
        composeRule.setContent {
            TravelhubTheme {
                LoginScreenContent(
                    uiState = uiState,
                    email = email,
                    password = password,
                    currentLocale = "es",
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = onSubmit,
                    onNavigateToRegister = onNavigateToRegister,
                    onLocaleChange = {},
                )
            }
        }
    }

    @Test
    fun `idle state renders welcome title and submit button`() {
        setContent()

        composeRule.onNodeWithText("Bienvenido").assertExists()
        composeRule.onNodeWithText("Iniciar sesión").assertExists()
    }

    @Test
    fun `error state shows backend message in banner`() {
        setContent(uiState = LoginUiState.Error("Credenciales inválidas"))

        composeRule.onNodeWithText("Credenciales inválidas").assertExists()
    }

    @Test
    fun `submit click invokes onSubmit callback`() {
        var submitted = false
        setContent(
            email = "ada@example.com",
            password = "secret",
            onSubmit = { submitted = true },
        )

        composeRule.onNodeWithText("Iniciar sesión").performScrollTo().performClick()

        assertTrue("onSubmit should have been invoked", submitted)
    }

    @Test
    fun `typing in email field forwards text to onEmailChange`() {
        var captured = ""
        setContent(onEmailChange = { captured = it })

        composeRule.onNodeWithText("ejemplo@correo.com").performScrollTo().performTextInput("ada@example.com")

        assertEquals("ada@example.com", captured)
    }

    @Test
    fun `typing in password field forwards text to onPasswordChange`() {
        var captured = ""
        setContent(onPasswordChange = { captured = it })

        composeRule.onNodeWithText("••••••••").performScrollTo().performTextInput("Sup3r!")

        assertEquals("Sup3r!", captured)
    }

    @Test
    fun `tapping register link invokes onNavigateToRegister`() {
        var navigated = false
        setContent(onNavigateToRegister = { navigated = true })

        composeRule
            .onNodeWithText("Regístrate ahora", substring = true)
            .performScrollTo()
            .performClick()

        assertTrue("onNavigateToRegister should have been invoked", navigated)
    }
}
