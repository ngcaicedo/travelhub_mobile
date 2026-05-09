package com.uniandes.travelhub.ui.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.uniandes.travelhub.ui.theme.TravelhubPillShape
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
    val canManage = (uiState as? ReservationDetailUiState.Success)
        ?.reservation?.status == ReservationStatus.CONFIRMED
    var modifyOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reservation_detail_title),
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
            if (canManage) {
                ReservationActionsBar(
                    onModifyClick = { modifyOpen = true },
                    onCancelClick = onStartCancel,
                )
            }
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(MaterialTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                        TravelHubPrimaryButton(stringResource(R.string.property_retry), onClick = onRetry)
                    }
                }
                is ReservationDetailUiState.Success -> {
                    ReservationDetailBody(reservation = uiState.reservation)
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

    if (modifyOpen && uiState is ReservationDetailUiState.Success) {
        ModifyBottomSheet(
            initialCheckIn = uiState.reservation.checkInDate.take(10),
            initialCheckOut = uiState.reservation.checkOutDate.take(10),
            initialGuests = uiState.reservation.numberOfGuests ?: 1,
            state = modifyState,
            onPreview = onPreviewModify,
            onConfirm = onConfirmModify,
            onClose = { modifyOpen = false; onDismissModify() },
        )
    }
}

@Composable
private fun ReservationDetailBody(reservation: ReservationResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = MaterialTheme.spacing.md,
            end = MaterialTheme.spacing.md,
            top = MaterialTheme.spacing.md,
            bottom = MaterialTheme.spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        item { StatusHero(reservation) }
        item {
            Section(title = stringResource(R.string.reservation_detail_stay_section)) {
                StayDetailsCard(reservation)
            }
        }
        reservation.priceBreakdown?.let { bd ->
            item {
                Section(title = stringResource(R.string.reservation_detail_pricing_section)) {
                    PricingCard(reservation, bd)
                }
            }
        }
        item {
            Section(title = stringResource(R.string.reservation_detail_info_section)) {
                InfoCard(reservation)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun StatusHero(reservation: ReservationResponse) {
    val (container, content, icon) = statusVisuals(reservation.status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, content.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(content.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = content,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservationStatusLabel(reservation.status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = content,
                )
                Text(
                    text = "#${reservation.id.take(10).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun StayDetailsCard(reservation: ReservationResponse) {
    SurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            IconRow(
                icon = Icons.Filled.CalendarMonth,
                label = stringResource(R.string.search_field_check_in),
                value = formatReservationDate(reservation.checkInDate),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            IconRow(
                icon = Icons.Filled.CalendarMonth,
                label = stringResource(R.string.search_field_check_out),
                value = formatReservationDate(reservation.checkOutDate),
            )
            reservation.numberOfGuests?.let { guests ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IconRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.search_field_guests),
                    value = guests.toString(),
                )
            }
        }
    }
}

@Composable
private fun PricingCard(
    reservation: ReservationResponse,
    bd: com.uniandes.travelhub.models.reservations.ReservationPriceBreakdown,
) {
    SurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            PriceLine(stringResource(R.string.reservation_detail_nights), bd.nights.toString())
            PriceLine(
                stringResource(R.string.reservation_detail_nightly_rate),
                formatCents(bd.nightlyRateInCents, bd.currency),
            )
            PriceLine(
                stringResource(R.string.reservation_detail_accommodation),
                formatCents(bd.accommodationInCents, bd.currency),
            )
            PriceLine(
                stringResource(R.string.reservation_detail_cleaning),
                formatCents(bd.cleaningFeeInCents, bd.currency),
            )
            PriceLine(
                stringResource(R.string.reservation_detail_service),
                formatCents(bd.serviceFeeInCents, bd.currency),
            )
            PriceLine(
                stringResource(R.string.reservation_detail_taxes),
                formatCents(bd.taxesInCents, bd.currency),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.payment_confirmation_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${reservation.totalPrice} ${reservation.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(reservation: ReservationResponse) {
    SurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            InfoLine(
                icon = Icons.Filled.Receipt,
                label = stringResource(R.string.payment_confirmation_reservation_id),
                value = reservation.id,
            )
            reservation.createdAt?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoLine(
                    icon = Icons.Filled.CalendarMonth,
                    label = stringResource(R.string.reservation_detail_created_label),
                    value = formatReservationDate(it),
                )
            }
        }
    }
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }
}

@Composable
private fun IconRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReservationActionsBar(
    onModifyClick: () -> Unit,
    onCancelClick: () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onModifyClick,
                    shape = TravelhubPillShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reservation_action_modify),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onCancelClick,
                    shape = TravelhubPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reservation_action_cancel),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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
            DatePickerField(
                value = checkIn,
                onValueChange = { checkIn = it },
                label = stringResource(R.string.search_field_check_in),
            )
            DatePickerField(
                value = checkOut,
                onValueChange = { checkOut = it },
                label = stringResource(R.string.search_field_check_out),
            )
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.md),
                    ) {
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
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = TravelhubPillShape,
                ) { Text(stringResource(R.string.common_close)) }
                if (state is ModifyActionState.Preview && state.data.changeAllowed) {
                    Button(
                        onClick = { onConfirm(checkIn, checkOut, guests) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = TravelhubPillShape,
                    ) { Text(stringResource(R.string.reservation_action_modify_confirm)) }
                } else {
                    Button(
                        onClick = { onPreview(checkIn, checkOut, guests) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = TravelhubPillShape,
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
        InlineField(stringResource(R.string.reservation_action_delta), data.deltaAmount)
        data.estimatedRefundAmount?.let {
            InlineField(stringResource(R.string.reservation_action_refund_estimate), it)
        }
        if (data.requiresAdditionalCharge) {
            Text(
                text = stringResource(R.string.reservation_action_extra_charge_required),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        data.policyApplied?.let {
            InlineField(stringResource(R.string.reservation_action_policy), policyTypeLabel(it.policyType))
        }
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
        InlineField(stringResource(R.string.reservation_action_refund), data.refundAmount)
        InlineField(stringResource(R.string.reservation_action_penalty), data.penaltyAmount)
        InlineField(stringResource(R.string.reservation_action_refund_type), refundTypeLabel(data.refundType))
        data.policyApplied?.let {
            InlineField(stringResource(R.string.reservation_action_policy), policyTypeLabel(it.policyType))
        }
    }
}

@Composable
private fun InlineField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class StatusVisuals(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
)

@Composable
private fun statusVisuals(status: String): Triple<Color, Color, ImageVector> = when (status) {
    ReservationStatus.CONFIRMED -> Triple(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.primary,
        Icons.Filled.Check,
    )
    ReservationStatus.PENDING_PAYMENT -> Triple(
        Color(0xFFFFF1C2),
        Color(0xFFBF6A02),
        Icons.Filled.HourglassTop,
    )
    ReservationStatus.CANCELLED -> Triple(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        Icons.Filled.Warning,
    )
    else -> Triple(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        Icons.Filled.HourglassTop,
    )
}

private fun formatCents(amount: Long, currency: String): String =
    "%,.2f %s".format(amount / 100.0, currency)
