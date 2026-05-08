package com.uniandes.travelhub.ui.hotel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.pricing.HotelDiscountType
import com.uniandes.travelhub.models.pricing.HotelPricingHistoryItem
import com.uniandes.travelhub.models.pricing.HotelPricingPreviewResponse
import com.uniandes.travelhub.models.pricing.HotelPricingTargetOption
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.Success500
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.HotelPricingFormState
import com.uniandes.travelhub.viewmodels.HotelPricingUiState
import com.uniandes.travelhub.viewmodels.HotelPricingViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HotelPricingScreen(
    viewModel: HotelPricingViewModel,
    onBackClick: () -> Unit,
) {
    val form by viewModel.form.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.success, uiState.error) {
        if (uiState.success != null || uiState.error != null) {
            // Keep transient messages user-visible until next interaction.
        }
    }

    HotelPricingScreenContent(
        form = form,
        uiState = uiState,
        selectedTarget = viewModel.selectedTarget(),
        onBackClick = onBackClick,
        onTargetSelected = {
            viewModel.clearMessage()
            viewModel.onTargetSelected(it)
        },
        onBasePriceChange = {
            viewModel.clearMessage()
            viewModel.onBasePriceChange(it)
        },
        onRuleNameChange = {
            viewModel.clearMessage()
            viewModel.onRuleNameChange(it)
        },
        onDiscountTypeChange = {
            viewModel.clearMessage()
            viewModel.onDiscountTypeChange(it)
        },
        onDiscountValueChange = {
            viewModel.clearMessage()
            viewModel.onDiscountValueChange(it)
        },
        onStartDateChange = {
            viewModel.clearMessage()
            viewModel.onStartDateChange(it)
        },
        onEndDateChange = {
            viewModel.clearMessage()
            viewModel.onEndDateChange(it)
        },
        onPreview = {
            viewModel.clearMessage()
            viewModel.preview()
        },
        onApply = {
            if (uiState.preview?.requiresConfirmation == true) {
                showConfirmation = true
            } else {
                viewModel.apply(confirmationAcknowledged = false)
            }
        },
        onRevert = {
            viewModel.clearMessage()
            viewModel.revert(it)
        },
    )

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.hotel_pricing_confirm_title)) },
            text = { Text(stringResource(R.string.hotel_pricing_confirm_body)) },
            confirmButton = {
                TravelHubPrimaryButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = {
                        showConfirmation = false
                        viewModel.apply(confirmationAcknowledged = true)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmation = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelPricingScreenContent(
    form: HotelPricingFormState,
    uiState: HotelPricingUiState,
    selectedTarget: HotelPricingTargetOption?,
    onBackClick: () -> Unit,
    onTargetSelected: (String) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onRuleNameChange: (String) -> Unit,
    onDiscountTypeChange: (HotelDiscountType?) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onPreview: () -> Unit,
    onApply: () -> Unit,
    onRevert: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var targetExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hotel_pricing_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.property_detail_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            SectionHeader(
                icon = Icons.Default.LocalOffer,
                title = stringResource(R.string.hotel_pricing_target_section),
            )

            ExposedDropdownMenuBox(
                expanded = targetExpanded,
                onExpandedChange = { targetExpanded = !targetExpanded },
            ) {
                OutlinedTextField(
                    value = selectedTarget?.displayName.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.hotel_pricing_target_label)) },
                    isError = form.validation.targetError != null,
                    supportingText = form.validation.targetError?.let { { Text(it.asString()) } },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false },
                ) {
                    uiState.targets.forEach { target ->
                        DropdownMenuItem(
                            text = { Text(target.displayName) },
                            onClick = {
                                targetExpanded = false
                                onTargetSelected(target.ratePlanId)
                            },
                        )
                    }
                }
            }

            SectionHeader(
                icon = Icons.Default.LocalOffer,
                title = stringResource(R.string.hotel_pricing_base_price_section),
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    Text(
                        text = stringResource(
                            R.string.hotel_pricing_base_price_label,
                            selectedTarget?.currency ?: "USD",
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = form.proposedBasePrice,
                        onValueChange = onBasePriceChange,
                        prefix = { Text("$ ") },
                        singleLine = true,
                        isError = form.validation.basePriceError != null,
                        supportingText = form.validation.basePriceError?.let { { Text(it.asString()) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionHeader(
                icon = Icons.Default.LocalOffer,
                title = stringResource(R.string.hotel_pricing_discount_section),
            )
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    OutlinedTextField(
                        value = form.ruleName,
                        onValueChange = onRuleNameChange,
                        label = { Text(stringResource(R.string.hotel_pricing_rule_name)) },
                        placeholder = { Text(stringResource(R.string.hotel_pricing_rule_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        AssistChip(
                            onClick = { onDiscountTypeChange(HotelDiscountType.PERCENTAGE) },
                            label = { Text(stringResource(R.string.hotel_pricing_discount_percentage)) },
                            leadingIcon = {
                                if (form.discountType == HotelDiscountType.PERCENTAGE) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null)
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (form.discountType == HotelDiscountType.PERCENTAGE) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                        )
                        AssistChip(
                            onClick = { onDiscountTypeChange(HotelDiscountType.FIXED) },
                            label = { Text(stringResource(R.string.hotel_pricing_discount_fixed)) },
                            leadingIcon = {
                                if (form.discountType == HotelDiscountType.FIXED) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null)
                                }
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (form.discountType == HotelDiscountType.FIXED) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        OutlinedTextField(
                            value = form.discountValue,
                            onValueChange = onDiscountValueChange,
                            label = {
                                Text(
                                    if (form.discountType == HotelDiscountType.FIXED) {
                                        stringResource(R.string.hotel_pricing_discount_value_money)
                                    } else {
                                        stringResource(R.string.hotel_pricing_discount_value_percentage)
                                    }
                                )
                            },
                            suffix = {
                                Text(
                                    if (form.discountType == HotelDiscountType.FIXED) {
                                        selectedTarget?.currency ?: ""
                                    } else {
                                        "%"
                                    }
                                )
                            },
                            singleLine = true,
                            isError = form.validation.discountValueError != null,
                            supportingText = form.validation.discountValueError?.let { { Text(it.asString()) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        DatePickerField(
                            value = form.startDate,
                            onValueChange = onStartDateChange,
                            label = stringResource(R.string.hotel_pricing_start_date),
                            isError = form.validation.startDateError != null,
                            supportingText = form.validation.startDateError?.let { { Text(it.asString()) } },
                            modifier = Modifier.weight(1f),
                            minDate = LocalDate.now(),
                        )
                        DatePickerField(
                            value = form.endDate,
                            onValueChange = onEndDateChange,
                            label = stringResource(R.string.hotel_pricing_end_date),
                            isError = form.validation.endDateError != null,
                            supportingText = form.validation.endDateError?.let { { Text(it.asString()) } },
                            modifier = Modifier.weight(1f),
                            minDate = runCatching { LocalDate.parse(form.startDate) }.getOrDefault(LocalDate.now()),
                        )
                    }
                    form.validation.changeError?.let {
                        Text(
                            text = it.asString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TravelHubPrimaryButton(
                        text = stringResource(R.string.hotel_pricing_preview_button),
                        onClick = onPreview,
                        loading = uiState.isPreviewLoading,
                    )
                }
            }

            SectionHeader(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.hotel_pricing_preview_section),
            )
            if (uiState.preview != null) {
                PreviewCard(preview = uiState.preview)
                TravelHubPrimaryButton(
                    text = stringResource(R.string.hotel_pricing_apply_button),
                    onClick = onApply,
                    loading = uiState.isApplying,
                )
            } else {
                EmptyPanel(text = stringResource(R.string.hotel_pricing_preview_empty))
            }

            SectionHeader(
                icon = Icons.Default.History,
                title = stringResource(R.string.hotel_pricing_history_section),
            )
            if (uiState.history.isEmpty()) {
                EmptyPanel(text = stringResource(R.string.hotel_pricing_history_empty))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                    uiState.history.forEach { entry ->
                        HistoryCard(
                            entry = entry,
                            onRevert = { onRevert(entry.id) },
                        )
                    }
                }
            }

            uiState.error?.let {
                Text(
                    text = it.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            uiState.success?.let {
                Text(
                    text = it.asString(),
                    color = Success500,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreviewCard(preview: HotelPricingPreviewResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.hotel_pricing_preview_final_price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (preview.discountValue != null && preview.discountType == "percentage") {
                    Surface(
                        color = Success500.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.hotel_pricing_preview_savings,
                                preview.discountValue.toInt(),
                            ),
                            color = Success500,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_current_base),
                value = formatMoney(preview.currentBasePrice, preview.currency),
            )
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_proposed_base),
                value = formatMoney(preview.proposedBasePrice, preview.currency),
            )
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_final_price),
                value = formatMoney(preview.finalPrice, preview.currency),
                bold = true,
            )
            HorizontalDivider()
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_revenue_before),
                value = formatMoney(preview.projectedRevenueBefore, preview.currency),
            )
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_revenue_after),
                value = formatMoney(preview.projectedRevenueAfter, preview.currency),
            )
            PriceRow(
                label = stringResource(R.string.hotel_pricing_preview_revenue_delta),
                value = formatMoney(preview.projectedRevenueDelta, preview.currency),
                bold = true,
            )
            Text(
                text = preview.impactSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryCard(
    entry: HotelPricingHistoryItem,
    onRevert: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = entry.ruleName ?: entry.ratePlanName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${entry.roomTypeName} · ${entry.propertyName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatDate(entry.startDate)} → ${formatDate(entry.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.hotel_pricing_history_meta,
                    entry.actorEmail,
                    entry.deviceLabel ?: entry.devicePlatform ?: "Android",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PriceRow(
                label = stringResource(R.string.hotel_pricing_history_final_price),
                value = formatMoney(entry.finalPrice, entry.currency),
            )
            if (entry.canRevert) {
                OutlinedButton(onClick = onRevert, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.hotel_pricing_revert_button))
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(MaterialTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PriceRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatMoney(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "${formatter.format(amount)} $currency"
}

private fun formatDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrDefault(value)
