package com.uniandes.travelhub.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.payments.FakePaymentScenarios
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.PaymentEvent
import com.uniandes.travelhub.viewmodels.PaymentUiState
import com.uniandes.travelhub.viewmodels.PaymentViewModel

@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    onBackClick: () -> Unit,
    onNavigateToConfirmation: (PaymentConfirmationSummary) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.NavigateToConfirmation -> onNavigateToConfirmation(event.confirmation)
            }
        }
    }

    PaymentScreenContent(
        uiState = uiState,
        onPayWithToken = viewModel::pay,
        onRetryConfig = viewModel::loadConfig,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreenContent(
    uiState: PaymentUiState,
    onPayWithToken: (String) -> Unit,
    onRetryConfig: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.property_detail_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.md),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                is PaymentUiState.LoadingConfig, is PaymentUiState.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (uiState is PaymentUiState.LoadingConfig)
                                stringResource(R.string.payment_loading_config)
                            else stringResource(R.string.payment_processing),
                        )
                    }
                }
                is PaymentUiState.Ready -> {
                    val realStripe = uiState.config.stripeEnabled
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                        Text(
                            text = if (realStripe) stringResource(R.string.payment_subtitle_real)
                            else stringResource(R.string.payment_subtitle_fake),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TravelHubPrimaryButton(
                            text = stringResource(R.string.payment_pay_success),
                            onClick = { onPayWithToken(FakePaymentScenarios.SUCCESS) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TravelHubPrimaryButton(
                            text = stringResource(R.string.payment_pay_insufficient),
                            onClick = { onPayWithToken(FakePaymentScenarios.INSUFFICIENT_FUNDS) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TravelHubPrimaryButton(
                            text = stringResource(R.string.payment_pay_declined),
                            onClick = { onPayWithToken(FakePaymentScenarios.CARD_DECLINED) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is PaymentUiState.Failed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Text(
                            text = uiState.message.asString(),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TravelHubPrimaryButton(
                            text = stringResource(R.string.property_retry),
                            onClick = onRetryConfig,
                        )
                    }
                }
                is PaymentUiState.Succeeded -> {
                    Text(stringResource(R.string.payment_succeeded))
                }
            }
        }
    }
}
