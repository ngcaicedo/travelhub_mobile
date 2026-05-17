package com.uniandes.travelhub.ui.partner.pricing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.partner.pricing.components.IntegrityBadge
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.RuleRow
import com.uniandes.travelhub.viewmodels.RulesListUiState
import com.uniandes.travelhub.viewmodels.RulesListViewModel
import androidx.compose.foundation.gestures.detectTapGestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    viewModel: RulesListViewModel,
    onPricingTabClick: () -> Unit,
    onRuleClick: (propertyId: String, ruleId: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh on every entry into the screen so a rule just created from the
    // pricing tab shows up immediately when the partner switches tabs.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.partner_pricing_rules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Text("←") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onPricingTabClick,
                    icon = { Text("🏠") },
                    label = { Text(stringResource(R.string.partner_pricing_nav_home)) },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Text("📅") },
                    label = { Text(stringResource(R.string.partner_pricing_nav_rules)) },
                )
            }
        },
    ) { padding ->
        when (val s = uiState) {
            RulesListUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is RulesListUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message.asString())
                    OutlinedButton(onClick = viewModel::refresh) {
                        Text(stringResource(R.string.property_retry))
                    }
                }
            }
            RulesListUiState.NoProperties -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.partner_pricing_empty_no_properties))
            }
            RulesListUiState.Empty -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.partner_pricing_empty_no_rules))
            }
            is RulesListUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                items(s.rows, key = { it.rule.id }) { row ->
                    RuleCard(
                        row = row,
                        onClick = { onRuleClick(row.property.id, row.rule.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    row: RuleRow,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(row.rule.id) {
                detectTapGestures(onTap = { onClick() })
            },
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            // Property name as the visual anchor of the card so the partner
            // can tell at a glance which property this rule belongs to.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = row.property.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IntegrityBadge(state = row.state)
            }
            Text(
                stringResource(
                    R.string.partner_pricing_rules_range,
                    row.rule.seasonStart,
                    row.rule.seasonEnd,
                ),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    R.string.partner_pricing_rules_final_price,
                    "$ %.2f %s".format(row.rule.pricePerNight, row.property.currency),
                ),
            )
            if (row.derivedPercent > 0) {
                Text(
                    stringResource(
                        R.string.partner_pricing_preview_savings_format,
                        row.derivedPercent,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
