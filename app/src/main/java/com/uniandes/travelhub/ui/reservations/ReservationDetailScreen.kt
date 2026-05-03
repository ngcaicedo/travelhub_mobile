package com.uniandes.travelhub.ui.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationCancellationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationModificationPreviewResponse
import com.uniandes.travelhub.models.reservations.ReservationResponse
import com.uniandes.travelhub.models.reservations.ReservationStatus
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.reservations.components.formatReservationDate
import com.uniandes.travelhub.ui.reservations.components.policyTypeLabel
import com.uniandes.travelhub.ui.reservations.components.refundTypeLabel
import com.uniandes.travelhub.ui.reservations.components.reservationStatusLabel
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.CancelActionState
import com.uniandes.travelhub.viewmodels.ModifyActionState
import com.uniandes.travelhub.viewmodels.ReservationDetailUiState
import com.uniandes.travelhub.viewmodels.ReservationDetailViewModel

@Composable
fun ReservationDetailScreen(
    viewModel: ReservationDetailViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val cancelState by viewModel.cancelState.collectAsState()
    val modifyState by viewModel.modifyState.collectAsState()

    ReservationDetailScreenContent(
        uiState = uiState,
        cancelState = cancelState,
        modifyState = modifyState,
        onRetry = viewModel::load,
        onBackClick = onBackClick,
        onStartCancel = viewModel::startCancel,
        onConfirmCancel = { viewModel.confirmCancel() },
        onDismissCancel = viewModel::dismissCancel,
        onPreviewModify = viewModel::previewModify,
        onConfirmModify = viewModel::confirmModify,
        onDismissModify = viewModel::dismissModify,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailScreenContent(
    uiState: ReservationDetailUiState,
    cancelState: CancelActionState,
    modifyState: ModifyActionState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onStartCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
    onDismissCancel: () -> Unit,
    onPreviewModify: (String, String, Int) -> Unit,
    onConfirmModify: (String, String, Int) -> Unit,
    onDismissModify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reservation_detail_title), fontWeight = FontWeight.Bold) },
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
                .padding(innerPadding),
        ) {
            when (uiState) {
                is ReservationDetailUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ReservationDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(MaterialTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                        TravelHubPrimaryButton(stringResource(R.string.property_retry), onClick = onRetry)
                    }
                }
                is ReservationDetailUiState.Success -> {
                    ReservationDetailBody(
                        reservation = uiState.reservation,
                        onStartCancel = onStartCancel,
                        onPreviewModify = onPreviewModify,
                        modifyState = modifyState,
                        onConfirmModify = onConfirmModify,
                        onDismissModify = onDismissModify,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.md)
                            .verticalScroll(rememberScrollState()),
                    )
                    if (cancelState !is CancelActionState.Idle) {
                        CancelDialog(
                            state = cancelState,
                            onConfirm = onConfirmCancel,
                            onDismiss = onDismissCancel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationDetailBody(
    reservation: ReservationResponse,
    onStartCancel: () -> Unit,
    onPreviewModify: (String, String, Int) -> Unit,
    modifyState: ModifyActionState,
    onConfirmModify: (String, String, Int) -> Unit,
    onDismissModify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canManage = reservation.status == ReservationStatus.CONFIRMED
    var modifyOpen by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Field(stringResource(R.string.payment_confirmation_reservation_id), reservation.id)
        Field(stringResource(R.string.reservation_detail_status), reservationStatusLabel(reservation.status))
        Field(stringResource(R.string.search_field_check_in), formatReservationDate(reservation.checkInDate))
        Field(stringResource(R.string.search_field_check_out), formatReservationDate(reservation.checkOutDate))
        reservation.numberOfGuests?.let { Field(stringResource(R.string.search_field_guests), it.toString()) }
        Field(stringResource(R.string.payment_confirmation_total), "${reservation.totalPrice} ${reservation.currency}")

        reservation.priceBreakdown?.let { bd ->
            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))
            Text(stringResource(R.string.reservation_detail_breakdown), style = MaterialTheme.typography.titleMedium)
            Field(stringResource(R.string.reservation_detail_nights), bd.nights.toString())
            Field(stringResource(R.string.reservation_detail_nightly_rate), formatCents(bd.nightlyRateInCents, bd.currency))
            Field(stringResource(R.string.reservation_detail_accommodation), formatCents(bd.accommodationInCents, bd.currency))
            Field(stringResource(R.string.reservation_detail_cleaning), formatCents(bd.cleaningFeeInCents, bd.currency))
            Field(stringResource(R.string.reservation_detail_service), formatCents(bd.serviceFeeInCents, bd.currency))
            Field(stringResource(R.string.reservation_detail_taxes), formatCents(bd.taxesInCents, bd.currency))
        }

        if (canManage) {
            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm))
            Text(
                text = stringResource(R.string.reservation_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                OutlinedButton(
                    onClick = { modifyOpen = !modifyOpen },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.reservation_action_modify)) }
                Button(
                    onClick = onStartCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.reservation_action_cancel)) }
            }

        }
    }

    if (modifyOpen) {
        ModifyBottomSheet(
            initialCheckIn = reservation.checkInDate.take(10),
            initialCheckOut = reservation.checkOutDate.take(10),
            initialGuests = reservation.numberOfGuests ?: 1,
            state = modifyState,
            onPreview = onPreviewModify,
            onConfirm = onConfirmModify,
            onClose = { modifyOpen = false; onDismissModify() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifyBottomSheet(
    initialCheckIn: String,
    initialCheckOut: String,
    initialGuests: Int,
    state: ModifyActionState,
    onPreview: (String, String, Int) -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var checkIn by rememberSaveable { mutableStateOf(initialCheckIn) }
    var checkOut by rememberSaveable { mutableStateOf(initialCheckOut) }
    var guests by rememberSaveable { mutableIntStateOf(initialGuests) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.md)
                .padding(bottom = MaterialTheme.spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.reservation_action_modify),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            DatePickerField(value = checkIn, onValueChange = { checkIn = it }, label = stringResource(R.string.search_field_check_in))
            DatePickerField(value = checkOut, onValueChange = { checkOut = it }, label = stringResource(R.string.search_field_check_out))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_field_guests),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (guests > 1) guests-- }, enabled = guests > 1) { Text("−") }
                    Text(
                        text = guests.toString(),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedButton(onClick = { guests++ }) { Text("+") }
                }
            }

            when (state) {
                is ModifyActionState.LoadingPreview, is ModifyActionState.Confirming ->
                    Box(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.md)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                is ModifyActionState.Error -> Text(
                    text = state.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                )
                is ModifyActionState.Preview -> ModifyPreviewSummary(state.data)
                is ModifyActionState.Done -> Text(
                    text = stringResource(R.string.reservation_action_modify_done),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                ModifyActionState.Idle -> Unit
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.common_close)) }
                if (state is ModifyActionState.Preview && state.data.changeAllowed) {
                    Button(
                        onClick = { onConfirm(checkIn, checkOut, guests) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.reservation_action_modify_confirm)) }
                } else {
                    Button(
                        onClick = { onPreview(checkIn, checkOut, guests) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.reservation_action_preview)) }
                }
            }
        }
    }
}

@Composable
private fun ModifyPreviewSummary(data: ReservationModificationPreviewResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        if (!data.changeAllowed) {
            Text(
                text = stringResource(R.string.reservation_action_change_blocked),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            data.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
        Field(stringResource(R.string.reservation_action_delta), data.deltaAmount)
        data.estimatedRefundAmount?.let { Field(stringResource(R.string.reservation_action_refund_estimate), it) }
        if (data.requiresAdditionalCharge) {
            Text(
                stringResource(R.string.reservation_action_extra_charge_required),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        data.policyApplied?.let { Field(stringResource(R.string.reservation_action_policy), policyTypeLabel(it.policyType)) }
    }
}

@Composable
private fun CancelDialog(
    state: CancelActionState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reservation_action_cancel)) },
        text = { CancelDialogBody(state) },
        confirmButton = {
            if (state is CancelActionState.Preview && state.data.changeAllowed) {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.reservation_action_cancel_confirm))
                }
            } else if (state is CancelActionState.Done) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            } else {
                Box(modifier = Modifier.padding(MaterialTheme.spacing.sm)) { /* spacer */ }
            }
        },
        dismissButton = {
            if (state !is CancelActionState.Done) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
            }
        },
    )
}

@Composable
private fun CancelDialogBody(state: CancelActionState) {
    when (state) {
        is CancelActionState.LoadingPreview, is CancelActionState.Confirming -> CircularProgressIndicator()
        is CancelActionState.Error -> Text(state.message.asString(), color = MaterialTheme.colorScheme.error)
        is CancelActionState.Done -> Text(stringResource(R.string.reservation_action_cancel_done))
        is CancelActionState.Preview -> CancelPreviewSummary(state.data)
        CancelActionState.Idle -> Unit
    }
}

@Composable
private fun CancelPreviewSummary(data: ReservationCancellationPreviewResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        if (!data.changeAllowed) {
            Text(
                text = stringResource(R.string.reservation_action_change_blocked),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            data.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
        Field(stringResource(R.string.reservation_action_refund), data.refundAmount)
        Field(stringResource(R.string.reservation_action_penalty), data.penaltyAmount)
        Field(stringResource(R.string.reservation_action_refund_type), refundTypeLabel(data.refundType))
        data.policyApplied?.let { Field(stringResource(R.string.reservation_action_policy), policyTypeLabel(it.policyType)) }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatCents(amount: Long, currency: String): String =
    "${amount / 100.0} $currency"
