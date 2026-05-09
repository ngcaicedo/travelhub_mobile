package com.uniandes.travelhub.ui.hotel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.hotelreservations.HotelReservationListItem
import com.uniandes.travelhub.models.hotelreservations.HotelReservationPropertyOption
import com.uniandes.travelhub.models.hotelreservations.HotelReservationStatusFilter
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.reservations.components.formatReservationDate
import com.uniandes.travelhub.ui.reservations.components.reservationStatusLabel
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.HotelReservationsListUiState
import com.uniandes.travelhub.viewmodels.HotelReservationsListViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HotelReservationsScreen(
    viewModel: HotelReservationsListViewModel,
    onBackClick: () -> Unit,
    onReservationClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    HotelReservationsScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onPropertySelected = viewModel::onPropertySelected,
        onStatusSelected = viewModel::onStatusSelected,
        onReservationClick = { onReservationClick(it.id) },
        onRetry = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HotelReservationsScreenContent(
    uiState: HotelReservationsListUiState,
    onBackClick: () -> Unit,
    onPropertySelected: (String) -> Unit,
    onStatusSelected: (HotelReservationStatusFilter) -> Unit,
    onReservationClick: (HotelReservationListItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var propertyExpanded by remember { mutableStateOf(false) }
    val selectedProperty = uiState.properties.firstOrNull { it.propertyId == uiState.selectedPropertyId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hotel_reservations_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.property_detail_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
        ) {
            SectionHeader(icon = Icons.Default.Apartment, title = stringResource(R.string.hotel_reservations_property_section))

            ExposedDropdownMenuBox(
                expanded = propertyExpanded,
                onExpandedChange = {
                    if (uiState.properties.isNotEmpty()) {
                        propertyExpanded = !propertyExpanded
                    }
                },
            ) {
                OutlinedTextField(
                    value = selectedProperty?.propertyName.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.hotel_reservations_property_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propertyExpanded) },
                    enabled = uiState.properties.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(expanded = propertyExpanded, onDismissRequest = { propertyExpanded = false }) {
                    uiState.properties.forEach { property ->
                        DropdownMenuItem(
                            text = { Text(property.propertyName) },
                            onClick = {
                                propertyExpanded = false
                                onPropertySelected(property.propertyId)
                            },
                        )
                    }
                }
            }

            SectionHeader(icon = Icons.Default.Hotel, title = stringResource(R.string.hotel_reservations_status_section))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    HotelReservationStatusFilter.ALL to R.string.hotel_reservations_status_all,
                    HotelReservationStatusFilter.PENDING_PAYMENT to R.string.hotel_reservations_status_pending,
                    HotelReservationStatusFilter.CONFIRMED to R.string.hotel_reservations_status_confirmed,
                    HotelReservationStatusFilter.CANCELLED to R.string.hotel_reservations_status_cancelled,
                ).forEach { (status, labelRes) ->
                    val isSelected = status == uiState.selectedStatus
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStatusSelected(status) },
                        modifier = Modifier.heightIn(min = 40.dp),
                        label = {
                            Text(
                                text = stringResource(labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.weight(1f))
                }
                uiState.error != null -> {
                    Text(uiState.error.asString(), color = MaterialTheme.colorScheme.error)
                    TravelHubPrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
                }
                uiState.reservations.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.hotel_reservations_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                        items(uiState.reservations, key = { it.id }) { reservation ->
                            HotelReservationCard(reservation = reservation, onClick = { onReservationClick(reservation) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotelReservationCard(
    reservation: HotelReservationListItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.guestFullName ?: "—",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "#${reservation.reservationNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = reservationStatusLabel(reservation.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DetailRow(icon = Icons.Default.Hotel, text = reservation.roomType ?: reservation.idRoom)
            DetailRow(icon = Icons.Default.CalendarMonth, text = "${formatReservationDate(reservation.checkInDate)} • ${formatReservationDate(reservation.checkOutDate)}")
            DetailRow(icon = Icons.Default.Person, text = reservation.numberOfGuests.toString())
            Text(
                text = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(reservation.totalPrice.toDoubleOrNull() ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
