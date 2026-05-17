package com.uniandes.travelhub.ui.properties.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class RatingChipTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders rating with one decimal`() {
        composeRule.setContent {
            TravelhubTheme { RatingChip(rating = 4.5) }
        }

        composeRule.onNodeWithText("4.5").assertIsDisplayed()
    }

    @Test
    fun `pads integer ratings to one decimal`() {
        composeRule.setContent {
            TravelhubTheme { RatingChip(rating = 5.0) }
        }

        composeRule.onNodeWithText("5.0").assertIsDisplayed()
    }

    @Test
    fun `rounds to one decimal`() {
        composeRule.setContent {
            TravelhubTheme { RatingChip(rating = 4.86) }
        }

        composeRule.onNodeWithText("4.9").assertIsDisplayed()
    }
}
