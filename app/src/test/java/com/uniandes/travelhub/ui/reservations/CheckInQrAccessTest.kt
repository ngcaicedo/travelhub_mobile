package com.uniandes.travelhub.ui.reservations

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import com.uniandes.travelhub.ui.reservations.components.ReservationCard
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.viewmodels.CancelActionState
import com.uniandes.travelhub.viewmodels.ModifyActionState
import com.uniandes.travelhub.viewmodels.ReservationDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
class CheckInQrAccessTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `reservation card shows check-in qr cta when reservation is eligible`() {
        composeRule.setContent {
            TravelhubTheme {
                ReservationCard(
                    reservation = reservationSummary(status = "confirmed"),
                    onClick = {},
                    onCheckInQrClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Mostrar QR de check-in").assertIsDisplayed()
    }

    @Test
    fun `reservation card hides check-in qr cta when reservation is not eligible`() {
        composeRule.setContent {
            TravelhubTheme {
                ReservationCard(
                    reservation = reservationSummary(status = "cancelled"),
                    onClick = {},
                    onCheckInQrClick = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Mostrar QR de check-in").assertCountEquals(0)
    }

    @Test
    fun `reservation detail shows check-in qr cta when reservation is eligible`() {
        composeRule.setContent {
            TravelhubTheme {
                ReservationDetailScreenContent(
                    uiState = ReservationDetailUiState.Success(reservation(status = "confirmed")),
                    cancelState = CancelActionState.Idle,
                    modifyState = ModifyActionState.Idle,
                    onRetry = {},
                    onBackClick = {},
                    onCheckInQrClick = {},
                    onStartCancel = {},
                    onConfirmCancel = {},
                    onDismissCancel = {},
                    onPreviewModify = { _, _, _ -> },
                    onConfirmModify = { _, _, _ -> },
                    onDismissModify = {},
                )
            }
        }

        composeRule.onNodeWithText("Mostrar QR de check-in").assertIsDisplayed()
    }

    private fun reservation(status: String) = ReservationResponse(
        id = "r-1",
        status = status,
        totalPrice = "476.00",
        currency = "COP",
        checkInDate = "2026-06-10T00:00:00",
        checkOutDate = "2026-06-12T00:00:00",
        idProperty = "p-1",
        idTraveler = "t-1",
        idRoom = "room-1",
        numberOfGuests = 2,
    )

    private fun reservationSummary(status: String) = ReservationWithDetailsResponse(
        id = "r-1",
        propertyName = "Grand Hotel Riviera",
        propertyCoverImageUrl = null,
        reservation = reservation(status),
    )
}
