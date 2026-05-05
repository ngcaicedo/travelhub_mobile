package com.uniandes.travelhub.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.CheckoutEvent
import com.uniandes.travelhub.viewmodels.CheckoutFormState
import com.uniandes.travelhub.viewmodels.CheckoutUiState
import com.uniandes.travelhub.viewmodels.CheckoutViewModel
import com.uniandes.travelhub.viewmodels.PriceSummary

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onBackClick: () -> Unit,
    onNavigateToPayment: (ReservationResponse) -> Unit,
) {
    val form by viewModel.form.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val property by viewModel.property.collectAsState()
    val summary = remember(form, property) { viewModel.computeSummary() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CheckoutEvent.NavigateToPayment -> onNavigateToPayment(event.reservation)
            }
        }
    }

    CheckoutScreenContent(
        form = form,
        uiState = uiState,
        summary = summary,
        onCheckInChange = viewModel::onCheckInChange,
        onCheckOutChange = viewModel::onCheckOutChange,
        onGuestsChange = viewModel::onGuestsChange,
        onSubmit = viewModel::submit,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenContent(
    form: CheckoutFormState,
    uiState: CheckoutUiState,
    summary: PriceSummary?,
    onCheckInChange: (String) -> Unit,
    onCheckOutChange: (String) -> Unit,
    onGuestsChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.property_detail_back))
                    }
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.checkout_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )

            DatePickerField(
                value = form.checkIn,
                onValueChange = onCheckInChange,
                label = stringResource(R.string.search_field_check_in),
                isError = form.checkInError != null,
                supportingText = form.checkInError?.let { { Text(it.asString()) } },
            )
            DatePickerField(
                value = form.checkOut,
                onValueChange = onCheckOutChange,
                label = stringResource(R.string.search_field_check_out),
                isError = form.checkOutError != null,
                supportingText = form.checkOutError?.let { { Text(it.asString()) } },
            )

            GuestsStepper(
                value = form.guests,
                onChange = onGuestsChange,
            )

            summary?.let { PriceSummaryCard(it) }

            when (uiState) {
                is CheckoutUiState.Idle, is CheckoutUiState.Success -> Unit
                is CheckoutUiState.Submitting -> CircularProgressIndicator()
                is CheckoutUiState.Error -> Text(
                    text = uiState.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            TravelHubPrimaryButton(
                text = stringResource(R.string.checkout_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Replaces the raw text input — on a phone, typing into a number field is
 * fiddly (the previous "1" sticks, the IME hides itself, etc). A stepper
 * with -/+ buttons is unambiguous and keeps `form.guests` always in a
 * valid state without parsing strings.
 */
@Composable
private fun GuestsStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 1,
    max: Int = 16,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.search_field_guests),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { onChange((value - 1).coerceAtLeast(min)) },
                    enabled = value > min,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "−")
                }
                Text(
                    text = value.toString(),
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.spacing.md)
                        .widthIn(min = 24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                FilledIconButton(
                    onClick = { onChange((value + 1).coerceAtMost(max)) },
                    enabled = value < max,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+")
                }
            }
        }
    }
}

@Composable
private fun PriceSummaryCard(summary: PriceSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.checkout_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SummaryLine(
                label = stringResource(
                    R.string.checkout_summary_accommodation,
                    formatMoney(summary.nightlyRate, summary.currency),
                    summary.nights,
                    summary.guests,
                ),
                value = formatMoney(summary.accommodation, summary.currency),
            )
            SummaryLine(
                label = stringResource(R.string.checkout_summary_service_fee),
                value = formatMoney(summary.serviceFee, summary.currency),
            )
            SummaryLine(
                label = stringResource(R.string.reservation_detail_cleaning),
                value = formatMoney(summary.cleaningFee, summary.currency),
            )
            SummaryLine(
                label = stringResource(R.string.reservation_detail_taxes),
                value = formatMoney(summary.taxes, summary.currency),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs))
            SummaryLine(
                label = stringResource(R.string.checkout_summary_total),
                value = formatMoney(summary.total, summary.currency),
                bold = true,
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatMoney(amount: Double, currency: String): String =
    "%,.2f %s".format(amount, currency)
