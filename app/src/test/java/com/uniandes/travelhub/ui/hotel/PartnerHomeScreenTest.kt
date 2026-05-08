package com.uniandes.travelhub.ui.hotel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class PartnerHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders pricing entry and opens module`() {
        var opened = false
        composeRule.setContent {
            TravelhubTheme {
                PartnerHomeScreen(
                    onOpenPricing = { opened = true },
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("Gestión de tarifas").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir gestión de precios").performClick()

        assertTrue(opened)
    }
}
