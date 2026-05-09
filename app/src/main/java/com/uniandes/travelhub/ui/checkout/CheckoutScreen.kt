package com.uniandes.travelhub.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.sanitizeDisplayText
import com.uniandes.travelhub.utils.sortPropertyImages
import com.uniandes.travelhub.viewmodels.CheckoutEvent
import com.uniandes.travelhub.viewmodels.CheckoutFormState
import com.uniandes.travelhub.viewmodels.CheckoutPricingState
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
    val pricingState by viewModel.pricingState.collectAsState()
    val summary = remember(form, property, pricingState) { viewModel.computeSummary() }

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
        pricingState = pricingState,
        property = property,
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
    pricingState: CheckoutPricingState,
    property: Property?,
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
                title = {
                    Text(
                        text = stringResource(R.string.checkout_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.property_detail_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            CheckoutBottomBar(
                summary = summary,
                isSubmitting = uiState is CheckoutUiState.Submitting,
                onSubmit = onSubmit,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            property?.let { PropertySummaryCard(it) }

            SectionTitle(stringResource(R.string.search_field_check_in) + " / " + stringResource(R.string.search_field_check_out))
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
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
            }

            SectionTitle(stringResource(R.string.search_field_guests))
            GuestsStepper(value = form.guests, onChange = onGuestsChange)
            form.guestsError?.let { err ->
                Text(
                    text = err.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            summary?.let {
                SectionTitle(stringResource(R.string.checkout_summary_title))
                EffectivePricingHint(
                    property = property,
                    pricingState = pricingState,
                    summary = it,
                )
                PriceSummaryCard(it)
            }

            if (uiState is CheckoutUiState.Error) {
                Text(
                    text = uiState.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }
    }
}

@Composable
private fun EffectivePricingHint(
    property: Property?,
    pricingState: CheckoutPricingState,
    summary: PriceSummary,
) {
    when (pricingState) {
        CheckoutPricingState.Loading -> Text(
            text = stringResource(R.string.checkout_price_verification_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is CheckoutPricingState.Available -> {
            val baseRate = property?.pricePerNight
            if (baseRate != null && kotlin.math.abs(pricingState.nightlyRate - baseRate) > 0.009) {
                Text(
                    text = stringResource(
                        R.string.checkout_price_adjusted_message,
                        formatMoney(summary.nightlyRate, summary.currency),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is CheckoutPricingState.Error -> Text(
            text = pricingState.message.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        is CheckoutPricingState.Unavailable -> Text(
            text = pricingState.message.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        CheckoutPricingState.Idle -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PropertySummaryCard(property: Property) {
    val coverUrl = remember(property.images) {
        sortPropertyImages(property.images).firstOrNull()?.url
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = sanitizeDisplayText(property.name),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = sanitizeDisplayText(property.propertyType).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                )
                Text(
                    text = sanitizeDisplayText(property.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sanitizeDisplayText(property.location),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutBottomBar(
    summary: PriceSummary?,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary?.let { formatMoney(it.total, it.currency) } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    summary?.let {
                        Text(
                            text = stringResource(R.string.checkout_nights_summary, it.nights),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting && summary != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(48.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.checkout_submit),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestsStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 1,
    max: Int = 16,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
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
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatMoney(amount: Double, currency: String): String =
    "%,.2f %s".format(amount, currency)
