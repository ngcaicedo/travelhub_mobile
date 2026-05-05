package com.uniandes.travelhub.ui.reservations.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.ReservationStatus
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats a backend timestamp (ISO-8601, possibly with `Z` or just `YYYY-MM-DD`)
 * as a medium-style local date in the active locale (e.g. "3 may 2026"). Falls
 * back to the raw value when parsing fails so we never crash on legacy data.
 */
@Composable
fun formatReservationDate(raw: String): String {
    val locale: Locale = LocalConfiguration.current.locales[0]
    return runCatching {
        val date = when {
            raw.contains('T') -> OffsetDateTime.parse(raw)
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate()
            else -> java.time.LocalDate.parse(raw)
        }
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }.getOrDefault(raw)
}

@Composable
fun refundTypeLabel(value: String): String = when (value) {
    "full_refund" -> stringResource(R.string.reservation_refund_type_full_refund)
    "partial_refund" -> stringResource(R.string.reservation_refund_type_partial_refund)
    "non_refundable" -> stringResource(R.string.reservation_refund_type_non_refundable)
    "none" -> stringResource(R.string.reservation_refund_type_none)
    "partial" -> stringResource(R.string.reservation_refund_type_partial)
    "full" -> stringResource(R.string.reservation_refund_type_full)
    else -> value
}

@Composable
fun policyTypeLabel(value: String): String = when (value) {
    "full_refund", "free_flexible" -> stringResource(R.string.reservation_policy_free_flexible)
    "partial_refund", "moderate" -> stringResource(R.string.reservation_policy_moderate)
    "non_refundable", "strict" -> stringResource(R.string.reservation_policy_strict)
    else -> value
}

/** Maps a backend status string to a localized label. */
@Composable
fun reservationStatusLabel(status: String): String = when (status) {
    ReservationStatus.PENDING_PAYMENT -> stringResource(R.string.reservation_status_pending_payment)
    ReservationStatus.CONFIRMED -> stringResource(R.string.reservation_status_confirmed)
    ReservationStatus.CANCELLED -> stringResource(R.string.reservation_status_cancelled)
    ReservationStatus.COMPLETED -> stringResource(R.string.reservation_status_completed)
    ReservationStatus.REFUND_PENDING -> stringResource(R.string.reservation_status_refund_pending)
    ReservationStatus.REFUND_COMPLETED -> stringResource(R.string.reservation_status_refund_completed)
    ReservationStatus.REFUND_FAILED -> stringResource(R.string.reservation_status_refund_failed)
    "cancel_requested" -> stringResource(R.string.reservation_status_cancel_requested)
    "modification_pending_payment" -> stringResource(R.string.reservation_status_modification_pending_payment)
    "modification_confirmed" -> stringResource(R.string.reservation_status_modification_confirmed)
    "additional_charge_failed" -> stringResource(R.string.reservation_status_additional_charge_failed)
    else -> status
}
