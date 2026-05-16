package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uniandes.travelhub.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/**
 * Read-only text field that opens a [DatePickerDialog] when tapped (anywhere on
 * the surface, not just the trailing icon). Emits dates back to the caller as
 * ISO YYYY-MM-DD, which is what the search, reservation and pricing endpoints
 * expect.
 *
 * The field is rendered with `enabled = false` so that touch events flow up to
 * the wrapping Box; the disabled colours are restored to look enabled.
 *
 * @param value Current ISO date (YYYY-MM-DD) or empty when not set.
 * @param minDate Earliest selectable date. Defaults to today (good for booking
 *  flows). Pass `LocalDate.MIN` (or any past date) when the use case allows
 *  picking historical ranges — e.g. seasonal pricing demos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    minDate: LocalDate = LocalDate.now(),
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            // Disabled so the click is intercepted by the surrounding Box; the
            // colours below mask that the field is not "enabled" in the visual sense.
            enabled = false,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text("YYYY-MM-DD") },
            isError = isError,
            supportingText = supportingText,
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = label) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showDialog) {
        val initialMillis = runCatching { LocalDate.parse(value, ISO_DATE) }
            .getOrNull()
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val minMillis = minDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= minMillis
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atOffset(ZoneOffset.UTC)
                                .toLocalDate()
                            onValueChange(picked.format(ISO_DATE))
                        }
                        showDialog = false
                    },
                ) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
