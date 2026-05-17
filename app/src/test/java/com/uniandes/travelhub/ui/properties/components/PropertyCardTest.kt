package com.uniandes.travelhub.ui.properties.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class PropertyCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleProperty = Property(
        id = "p-1",
        name = "Eco-Lodge en el Bosque Nuboso",
        description = "Una cabaña entre las nubes",
        location = "Monteverde, Puntarenas, Costa Rica",
        pricePerNight = 250.0,
        currency = "USD",
        rating = 4.9,
    )

    @Test
    fun `renders name, location, price and rating`() {
        composeRule.setContent {
            TravelhubTheme {
                PropertyCard(property = sampleProperty, onClick = {})
            }
        }

        composeRule.onNodeWithText("Eco-Lodge en el Bosque Nuboso").assertIsDisplayed()
        composeRule.onNodeWithText("Monteverde, Puntarenas, Costa Rica").assertIsDisplayed()
        composeRule.onNodeWithText("USD 250.0").assertIsDisplayed()
        composeRule.onNodeWithText("noche", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("4.9").assertIsDisplayed()
    }

    @Test
    fun `tap on card invokes onClick`() {
        var clicked = false
        composeRule.setContent {
            TravelhubTheme {
                PropertyCard(property = sampleProperty, onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("Eco-Lodge en el Bosque Nuboso").performClick()

        assertTrue("onClick should have been invoked", clicked)
    }
}
