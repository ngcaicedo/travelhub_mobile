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
    fun `renders hotel modules and opens each entry`() {
        var pricingOpened = false
        var reservationsOpened = false

        composeRule.setContent {
            TravelhubTheme {
                PartnerHomeScreen(
                    onOpenPricing = { pricingOpened = true },
                    onOpenReservations = { reservationsOpened = true },
                    onLogout = {},
                )
            }
        }

        composeRule.onNodeWithText("Gestión de reservas").assertIsDisplayed()
        composeRule.onNodeWithText("Gestión de tarifas").assertIsDisplayed()

        composeRule.onNodeWithText("Abrir gestión de reservas").performClick()
        composeRule.onNodeWithText("Abrir gestión de precios").performClick()

        assertTrue(reservationsOpened)
        assertTrue(pricingOpened)
    }
}
