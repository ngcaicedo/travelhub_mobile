package com.uniandes.travelhub.ui.auth.verifyotp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.viewmodels.VerifyOtpUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class VerifyOtpScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        uiState: VerifyOtpUiState = VerifyOtpUiState.Idle,
        email: String = "ada@example.com",
        otpCode: String = "",
        onOtpChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onNavigateBackToLogin: () -> Unit = {},
    ) {
        composeRule.setContent {
            TravelhubTheme {
                VerifyOtpScreenContent(
                    uiState = uiState,
                    email = email,
                    otpCode = otpCode,
                    currentLocale = "es",
                    onOtpChange = onOtpChange,
                    onSubmit = onSubmit,
                    onNavigateBackToLogin = onNavigateBackToLogin,
                    onLocaleChange = {},
                )
            }
        }
    }

    @Test
    fun `idle state renders title subtitle with email and submit button`() {
        setContent()

        composeRule.onNodeWithText("Verifica tu correo").assertExists()
        composeRule.onNodeWithText("ada@example.com", substring = true).assertExists()
        composeRule.onNodeWithText("Verificar").assertExists()
    }

    @Test
    fun `error state shows backend message in banner`() {
        setContent(uiState = VerifyOtpUiState.Error(ErrorMessage.Plain("El código debe tener 6 dígitos")))

        composeRule.onNodeWithText("El código debe tener 6 dígitos").assertExists()
    }

    @Test
    fun `submit click invokes onSubmit callback`() {
        var submitted = false
        setContent(otpCode = "123456", onSubmit = { submitted = true })

        composeRule.onNodeWithText("Verificar").performScrollTo().performClick()

        assertTrue("onSubmit should have been invoked", submitted)
    }

    @Test
    fun `typing in otp field forwards text to onOtpChange`() {
        var captured = ""
        setContent(onOtpChange = { captured = it })

        composeRule.onNodeWithText("123456").performScrollTo().performTextInput("987654")

        assertEquals("987654", captured)
    }

    @Test
    fun `tapping back link invokes onNavigateBackToLogin`() {
        var navigated = false
        setContent(onNavigateBackToLogin = { navigated = true })

        composeRule
            .onNodeWithText("Volver al inicio de sesión")
            .performScrollTo()
            .performClick()

        assertTrue("onNavigateBackToLogin should have been invoked", navigated)
    }
}
