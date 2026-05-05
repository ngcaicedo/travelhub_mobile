package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
 * Read-only text field that opens a [DatePickerDialog] when tapped (or via the
 * trailing calendar icon). Emits dates back to the caller as ISO YYYY-MM-DD,
 * which is what the search and reservation endpoints expect.
 *
 * @param value Current ISO date (YYYY-MM-DD) or empty when not set.
 * @param minDate Earliest selectable date. Defaults to today, so users can't
 *  pick check-in/check-out in the past.
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

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        isError = isError,
        supportingText = supportingText,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.DateRange, contentDescription = label)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .let { mod ->
                // Surface a click anywhere on the field, not only on the trailing icon.
                mod
            },
    )

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
