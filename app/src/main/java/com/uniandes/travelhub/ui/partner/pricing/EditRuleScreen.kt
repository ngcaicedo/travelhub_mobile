package com.uniandes.travelhub.ui.partner.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.uniandes.travelhub.ui.partner.pricing.components.IntegrityBadge
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.EditRuleEvent
import com.uniandes.travelhub.viewmodels.EditRuleUiState
import com.uniandes.travelhub.viewmodels.EditRuleViewModel
import com.uniandes.travelhub.viewmodels.ErrorMessage
import com.uniandes.travelhub.viewmodels.IntegrityState
import com.uniandes.travelhub.viewmodels.PendingPatch
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleScreen(
    viewModel: EditRuleViewModel,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var banner by remember { mutableStateOf<ErrorMessage?>(null) }
    var pendingHighImpact by remember { mutableStateOf<PendingPatch?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditRuleEvent.RequiresHighImpactConfirmation ->
                    pendingHighImpact = event.pending
                is EditRuleEvent.SubmitError -> banner = event.message
                is EditRuleEvent.SubmitSuccess -> onSaved()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.partner_pricing_edit_title)) },
                navigationIcon = { IconButton(onClick = onBackClick) { Text("←") } },
            )
        },
    ) { padding ->
        when (val s = uiState) {
            EditRuleUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is EditRuleUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.message.asString())
            }
            is EditRuleUiState.Ready -> Form(
                state = s,
                bannerText = banner?.asString(),
                onDismissBanner = { banner = null },
                onSubmit = viewModel::submitUpdate,
                padding = padding,
            )
        }
    }

    val pending = pendingHighImpact
    if (pending != null) {
        val basePrice = (uiState as? EditRuleUiState.Ready)?.basePrice ?: 0.0
        val newPrice = pending.patch.pricePerNight ?: basePrice
        val pct = if (basePrice > 0.0) {
            kotlin.math.round((1.0 - newPrice / basePrice) * 100.0).toInt()
        } else 0
        AlertDialog(
            onDismissRequest = { pendingHighImpact = null },
            title = { Text(stringResource(R.string.partner_pricing_high_impact_title)) },
            text = {
                Text(stringResource(R.string.partner_pricing_high_impact_message, pct))
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
private fun Form(
    state: EditRuleUiState.Ready,
    bannerText: String?,
    onDismissBanner: () -> Unit,
    onSubmit: (Double?, String?, String?) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues,
) {
    val derivedPercent = remember(state.rule.pricePerNight, state.basePrice) {
        if (state.basePrice > 0.0) {
            kotlin.math.round((1.0 - state.rule.pricePerNight / state.basePrice) * 100.0).toInt()
        } else 0
    }
    var percentText by remember(state.rule.id) {
        mutableStateOf(if (derivedPercent > 0) derivedPercent.toString() else "")
    }
    var seasonStart by remember(state.rule.id) { mutableStateOf(state.rule.seasonStart) }
    var seasonEnd by remember(state.rule.id) { mutableStateOf(state.rule.seasonEnd) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.property.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val integrityState = when {
                state.rule.integrityLocked -> IntegrityState.Locked
                !state.rule.integrityValid -> IntegrityState.Compromised
                else -> IntegrityState.Normal
            }
            IntegrityBadge(state = integrityState)
        }
        Text(
            stringResource(
                R.string.partner_pricing_rules_base_price,
                "$ %.2f %s".format(state.basePrice, state.property.currency),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!state.canEdit) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.partner_pricing_locked_warning),
                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                )
            }
        }

        OutlinedTextField(
            value = percentText,
            onValueChange = { v ->
                if (v.isEmpty() || v.matches(Regex("""^\d{1,2}(\.\d{0,2})?$"""))) {
                    percentText = v
                }
            },
            label = { Text(stringResource(R.string.partner_pricing_field_percent)) },
            suffix = { Text("%") },
            enabled = state.canEdit,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            DatePickerField(
                value = seasonStart,
                onValueChange = { if (state.canEdit) seasonStart = it },
                label = stringResource(R.string.partner_pricing_field_season_start),
                minDate = java.time.LocalDate.of(2020, 1, 1),
                modifier = Modifier.weight(1f),
            )
            DatePickerField(
                value = seasonEnd,
                onValueChange = { if (state.canEdit) seasonEnd = it },
                label = stringResource(R.string.partner_pricing_field_season_end),
                minDate = java.time.LocalDate.of(2020, 1, 1),
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = {
                val typed = percentText.toDoubleOrNull()
                val pctParam = typed?.takeIf { it.toInt() != derivedPercent }
                val startParam = seasonStart.takeIf { it != state.rule.seasonStart }
                val endParam = seasonEnd.takeIf { it != state.rule.seasonEnd }
                onSubmit(pctParam, startParam, endParam)
            },
            enabled = state.canEdit && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.partner_pricing_action_save))
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
