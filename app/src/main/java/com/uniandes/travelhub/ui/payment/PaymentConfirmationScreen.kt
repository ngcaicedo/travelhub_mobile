package com.uniandes.travelhub.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmationScreen(
    confirmation: PaymentConfirmationSummary,
    onSeeReservationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.payment_confirmation_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.payment_confirmation_success),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.payment_confirmation_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))

            ConfirmationRow(
                label = stringResource(R.string.payment_confirmation_reservation_id),
                value = confirmation.reservationId,
            )
            ConfirmationRow(
                label = stringResource(R.string.payment_confirmation_payment_id),
                value = confirmation.paymentId,
            )
            confirmation.propertyName?.let {
                ConfirmationRow(stringResource(R.string.payment_confirmation_property), it)
            }
            confirmation.propertyAddress?.let {
                ConfirmationRow(stringResource(R.string.payment_confirmation_address), it)
            }
            confirmation.checkInDate?.let {
                ConfirmationRow(stringResource(R.string.search_field_check_in), it)
            }
            confirmation.checkOutDate?.let {
                ConfirmationRow(stringResource(R.string.search_field_check_out), it)
            }
            confirmation.guestsCount?.let {
                ConfirmationRow(stringResource(R.string.search_field_guests), it.toString())
            }
            ConfirmationRow(
                label = stringResource(R.string.payment_confirmation_total),
                value = "${confirmation.amountInCents / 100.0} ${confirmation.currency}",
            )
            confirmation.receiptNumber?.let {
                ConfirmationRow(stringResource(R.string.payment_confirmation_receipt), it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))

            TravelHubPrimaryButton(
                text = stringResource(R.string.payment_confirmation_see_reservations),
                onClick = onSeeReservationsClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConfirmationRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
