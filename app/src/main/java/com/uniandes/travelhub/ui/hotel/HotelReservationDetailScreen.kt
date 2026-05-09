package com.uniandes.travelhub.ui.hotel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.hotelreservations.HotelReservationCancellationReason
import com.uniandes.travelhub.models.hotelreservations.hasAction
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.reservations.components.formatReservationDate
import com.uniandes.travelhub.ui.reservations.components.reservationStatusLabel
import com.uniandes.travelhub.ui.theme.TravelhubPillShape
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.HotelReservationActionState
import com.uniandes.travelhub.viewmodels.HotelReservationDetailUiState
import com.uniandes.travelhub.viewmodels.HotelReservationDetailViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HotelReservationDetailScreen(
    viewModel: HotelReservationDetailViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf(HotelReservationCancellationReason.HOTEL_POLICY) }
    var cancelNote by remember { mutableStateOf("") }

    HotelReservationDetailScreenContent(
        uiState = uiState,
        actionState = actionState,
        onBackClick = onBackClick,
        onConfirmClick = { showConfirmDialog = true },
        onCancelClick = { showCancelDialog = true },
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.hotel_reservations_confirm_title)) },
            text = { Text(stringResource(R.string.hotel_reservations_confirm_body)) },
            confirmButton = {
                TravelHubPrimaryButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirmReservation()
                    },
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.hotel_reservations_cancel_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.hotel_reservations_cancel_body))
                    CancelReasonChooser(selected = cancelReason, onSelected = { cancelReason = it })
                    OutlinedTextField(
                        value = cancelNote,
                        onValueChange = { cancelNote = it },
                        label = { Text(stringResource(R.string.hotel_reservations_cancel_note)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TravelHubPrimaryButton(
                    text = stringResource(R.string.hotel_reservations_cancel_confirm),
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelReservation(cancelReason, cancelNote)
                    },
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelReservationDetailScreenContent(
    uiState: HotelReservationDetailUiState,
    actionState: HotelReservationActionState,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hotel_reservations_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.property_detail_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is HotelReservationDetailUiState.Loading -> Row(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { CircularProgressIndicator() }
            is HotelReservationDetailUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(MaterialTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error)
            }
            is HotelReservationDetailUiState.Success -> {
                val detail = uiState.detail
                val reservation = detail.reservation
                val canConfirm = detail.availableActions.hasAction("confirm")
                val canCancel = detail.availableActions.hasAction("cancel")
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
                ) {
                    if (actionState is HotelReservationActionState.Success) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                text = actionState.message.asString(),
                                modifier = Modifier.padding(MaterialTheme.spacing.md),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    if (actionState is HotelReservationActionState.Error) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                text = actionState.message.asString(),
                                modifier = Modifier.padding(MaterialTheme.spacing.md),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "#${reservation.id.takeLast(8).uppercase()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = reservationStatusLabel(reservation.status), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            detail.guest?.fullName?.let { DetailRow(Icons.Default.Person, it) }
                            detail.guest?.email?.let { DetailRow(Icons.Default.Email, it) }
                            detail.guest?.phone?.let { DetailRow(Icons.Default.Phone, it) }
                            DetailRow(Icons.Default.Hotel, reservation.idRoom ?: "—")
                            DetailRow(Icons.Default.CalendarMonth, "${formatReservationDate(reservation.checkInDate)} • ${formatReservationDate(reservation.checkOutDate)}")
                            DetailRow(Icons.Default.Person, (reservation.numberOfGuests ?: 1).toString())
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(reservation.totalPrice.toDoubleOrNull() ?: 0.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (detail.changeHistory.isNotEmpty()) {
                        SectionHeader(icon = Icons.Default.CheckCircle, title = stringResource(R.string.hotel_reservations_history_title))
                        detail.changeHistory.forEach { change ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(change.action, fontWeight = FontWeight.Bold)
                                    Text(change.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatReservationDate(change.createdAt), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (canConfirm || canCancel) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                        ) {
                            if (canConfirm) {
                                TravelHubPrimaryButton(
                                    text = stringResource(R.string.hotel_reservations_action_confirm),
                                    onClick = onConfirmClick,
                                    loading = actionState is HotelReservationActionState.Working,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (canCancel) {
                                OutlinedButton(
                                    onClick = onCancelClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    enabled = actionState !is HotelReservationActionState.Working,
                                    shape = TravelhubPillShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.hotel_reservations_action_cancel))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelReasonChooser(
    selected: HotelReservationCancellationReason,
    onSelected: (HotelReservationCancellationReason) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            HotelReservationCancellationReason.MAINTENANCE to R.string.hotel_reservations_reason_maintenance,
            HotelReservationCancellationReason.OVERBOOKING to R.string.hotel_reservations_reason_overbooking,
            HotelReservationCancellationReason.HOTEL_POLICY to R.string.hotel_reservations_reason_policy,
            HotelReservationCancellationReason.OTHER to R.string.hotel_reservations_reason_other,
        ).forEach { (reason, labelRes) ->
            val isSelected = selected == reason
            OutlinedButton(
                onClick = { onSelected(reason) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(labelRes),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
