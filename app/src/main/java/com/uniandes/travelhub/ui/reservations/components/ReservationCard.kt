package com.uniandes.travelhub.ui.reservations.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationStatus
import com.uniandes.travelhub.models.reservations.ReservationWithDetailsResponse
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.sanitizeDisplayText

@Composable
fun ReservationCard(
    reservation: ReservationWithDetailsResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = reservation.reservation
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            reservation.propertyCoverImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = sanitizeDisplayText(reservation.propertyName.orEmpty()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
            }
            Text(
                text = sanitizeDisplayText(reservation.propertyName.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = stringResource(
                    R.string.reservation_card_dates,
                    formatReservationDate(r.checkInDate),
                    formatReservationDate(r.checkOutDate),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            r.numberOfGuests?.let { guests ->
                Text(
                    text = stringResource(
                        if (guests == 1) R.string.reservation_card_guests_singular
                        else R.string.reservation_card_guests_plural,
                        guests,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${r.totalPrice} ${r.currency}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                AssistChip(
                    onClick = {},
                    label = { Text(reservationStatusLabel(r.status)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipColor(r.status),
                    ),
                )
            }
        }
    }
}

@Composable
private fun chipColor(status: String) = when (status) {
    ReservationStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
    ReservationStatus.PENDING_PAYMENT -> MaterialTheme.colorScheme.secondaryContainer
    ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}
