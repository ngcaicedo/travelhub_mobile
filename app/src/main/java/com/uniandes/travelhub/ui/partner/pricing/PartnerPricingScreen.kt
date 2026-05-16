package com.uniandes.travelhub.ui.partner.pricing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.viewmodels.PartnerPricingEvent
import com.uniandes.travelhub.viewmodels.PartnerPricingUiState
import com.uniandes.travelhub.viewmodels.PartnerPricingViewModel
import com.uniandes.travelhub.viewmodels.PendingDiscount
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPricingScreen(
    viewModel: PartnerPricingViewModel,
    onRulesTabClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var banner by remember { mutableStateOf<ErrorMessage?>(null) }
    var successBanner by remember { mutableStateOf<String?>(null) }
    // Bumped on every successful create so the form fields can re-initialise
    // (their `remember(formResetKey)` keys see a new value and reset to empty).
    var formResetKey by remember { mutableStateOf(0) }
    var pendingHighImpact by remember { mutableStateOf<PendingDiscount?>(null) }
    val successCreatedText = stringResource(R.string.partner_pricing_success_created)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PartnerPricingEvent.RequiresHighImpactConfirmation ->
                    pendingHighImpact = event.request
                is PartnerPricingEvent.SubmitError -> {
                    banner = event.message
                    successBanner = null
                }
                is PartnerPricingEvent.SubmitSuccess -> {
                    banner = null
                    successBanner = successCreatedText
                    formResetKey++
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.partner_pricing_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Text("←") }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.partner_pricing_action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Text("🏠") },
                    label = { Text(stringResource(R.string.partner_pricing_nav_home)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onRulesTabClick,
                    icon = { Text("📅") },
                    label = { Text(stringResource(R.string.partner_pricing_nav_rules)) },
                )
            }
        },
    ) { padding ->
        when (val s = uiState) {
            PartnerPricingUiState.Loading -> CenteredLoading(padding)
            is PartnerPricingUiState.Error -> CenteredText(padding, s.message.asString())
            is PartnerPricingUiState.Empty -> CenteredText(padding, s.reason.asString())
            is PartnerPricingUiState.Ready -> ReadyContent(
                state = s,
                onSelectProperty = viewModel::selectProperty,
                onSubmit = viewModel::submitNewDiscount,
                bannerText = banner?.asString(),
                onDismissBanner = { banner = null },
                successText = successBanner,
                onDismissSuccess = { successBanner = null },
                formResetKey = formResetKey,
                padding = padding,
            )
        }
    }

    val pending = pendingHighImpact
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingHighImpact = null },
            title = { Text(stringResource(R.string.partner_pricing_high_impact_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.partner_pricing_high_impact_message,
                        pending.percent.toInt(),
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingHighImpact = null
                    viewModel.confirmAndSubmit(pending)
                }) {
                    Text(stringResource(R.string.partner_pricing_high_impact_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHighImpact = null }) {
                    Text(stringResource(R.string.partner_pricing_action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: PartnerPricingUiState.Ready,
    onSelectProperty: (String) -> Unit,
    onSubmit: (Double, String, String) -> Unit,
    bannerText: String?,
    onDismissBanner: () -> Unit,
    successText: String?,
    onDismissSuccess: () -> Unit,
    formResetKey: Int,
    padding: PaddingValues,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    // Re-keyed on every successful create so the form clears itself.
    var ruleName by remember(formResetKey) { mutableStateOf("") }
    var percentText by remember(formResetKey) { mutableStateOf("") }
    var seasonStart by remember(formResetKey) { mutableStateOf("") }
    var seasonEnd by remember(formResetKey) { mutableStateOf("") }

    val selectedProperty = state.properties.firstOrNull { it.id == state.selectedPropertyId }
    val pct = percentText.toDoubleOrNull()
    val base = selectedProperty?.pricePerNight ?: 0.0
    val finalPrice = if (pct != null && pct in 0.0..100.0) {
        kotlin.math.round(base * (1.0 - pct / 100.0) * 100.0) / 100.0
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        SectionLabel(R.string.partner_pricing_section_property)
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
        ) {
            OutlinedTextField(
                value = selectedProperty?.name
                    ?: stringResource(R.string.partner_pricing_field_property),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
            ) {
                state.properties.forEach { prop ->
                    DropdownMenuItem(
                        text = { Text(prop.name) },
                        onClick = {
                            onSelectProperty(prop.id)
                            dropdownExpanded = false
                        },
                    )
                }
            }
        }

        SectionLabel(R.string.partner_pricing_section_base_rate)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FC))) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                Text(
                    stringResource(
                        R.string.partner_pricing_field_base_rate_label,
                        selectedProperty?.currency ?: "",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "$ %.2f".format(base),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        SectionLabel(R.string.partner_pricing_section_new_discount)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FC)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(stringResource(R.string.partner_pricing_field_name)) },
                    placeholder = {
                        Text(stringResource(R.string.partner_pricing_field_name_hint))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("""^\d{1,2}(\.\d{0,2})?$"""))) {
                            percentText = v
                        }
                    },
                    label = { Text(stringResource(R.string.partner_pricing_field_percent)) },
                    suffix = { Text("%") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    DatePickerField(
                        value = seasonStart,
                        onValueChange = { seasonStart = it },
                        label = stringResource(R.string.partner_pricing_field_season_start),
                        minDate = java.time.LocalDate.of(2020, 1, 1),
                        modifier = Modifier.weight(1f),
                    )
                    DatePickerField(
                        value = seasonEnd,
                        onValueChange = { seasonEnd = it },
                        label = stringResource(R.string.partner_pricing_field_season_end),
                        minDate = java.time.LocalDate.of(2020, 1, 1),
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = {
                        val p = percentText.toDoubleOrNull() ?: return@Button
                        onSubmit(p, seasonStart, seasonEnd)
                    },
                    enabled = !state.isSubmitting && selectedProperty != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.partner_pricing_action_create_rule))
                }
            }
        }

        SectionLabel(R.string.partner_pricing_section_preview)
        PreviewCard(
            base = base,
            pct = pct,
            finalPrice = finalPrice,
            currency = selectedProperty?.currency.orEmpty(),
        )

        if (successText != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                    Text(
                        successText,
                        color = Color(0xFF1E7E34),
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedButton(onClick = onDismissSuccess) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
        if (bannerText != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                    Text(bannerText)
                    OutlinedButton(onClick = onDismissBanner) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    base: Double,
    pct: Double?,
    finalPrice: Double?,
    currency: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    stringResource(R.string.partner_pricing_preview_final_rate),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (finalPrice != null) "$ %.2f %s".format(finalPrice, currency)
                    else "$ %.2f %s".format(base, currency),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (pct != null && pct > 0.0) {
                Text(
                    stringResource(R.string.partner_pricing_preview_savings_format, pct.toInt()),
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    stringResource(R.string.partner_pricing_preview_no_discount),
                    color = Color.LightGray,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(resId: Int) {
    Text(
        stringResource(resId),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun CenteredLoading(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredText(padding: PaddingValues, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(MaterialTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

