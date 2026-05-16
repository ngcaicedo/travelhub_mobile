package com.uniandes.travelhub.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.Amenities
import com.uniandes.travelhub.models.search.SearchOrderBy
import com.uniandes.travelhub.models.search.SearchOrderDir
import com.uniandes.travelhub.ui.auth.components.DatePickerField
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.SearchFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilters(
    form: SearchFormState,
    onCityChange: (String) -> Unit,
    onCheckInChange: (String) -> Unit,
    onCheckOutChange: (String) -> Unit,
    onGuestsChange: (Int) -> Unit,
    onMinPriceChange: (Int?) -> Unit,
    onMaxPriceChange: (Int?) -> Unit,
    onAmenityToggle: (String) -> Unit,
    onOrderByChange: (SearchOrderBy?) -> Unit,
    onOrderDirChange: (SearchOrderDir?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        OutlinedTextField(
            value = form.city,
            onValueChange = onCityChange,
            label = { Text(stringResource(R.string.search_field_city)) },
            isError = form.validation.cityError != null,
            supportingText = form.validation.cityError?.let { { Text(it.asString()) } },
            modifier = Modifier.fillMaxWidth(),
        )

        DatePickerField(
            value = form.checkIn,
            onValueChange = onCheckInChange,
            label = stringResource(R.string.search_field_check_in),
            isError = form.validation.checkInError != null,
            supportingText = form.validation.checkInError?.let { { Text(it.asString()) } },
        )
        DatePickerField(
            value = form.checkOut,
            onValueChange = onCheckOutChange,
            label = stringResource(R.string.search_field_check_out),
            isError = form.validation.checkOutError != null,
            supportingText = form.validation.checkOutError?.let { { Text(it.asString()) } },
        )

        OutlinedTextField(
            value = form.guests.toString(),
            onValueChange = { onGuestsChange(it.toIntOrNull() ?: 1) },
            label = { Text(stringResource(R.string.search_field_guests)) },
            isError = form.validation.guestsError != null,
            supportingText = form.validation.guestsError?.let { { Text(it.asString()) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            OutlinedTextField(
                value = form.minPrice?.toString().orEmpty(),
                onValueChange = { onMinPriceChange(it.toIntOrNull()) },
                label = { Text(stringResource(R.string.search_field_min_price)) },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.maxPrice?.toString().orEmpty(),
                onValueChange = { onMaxPriceChange(it.toIntOrNull()) },
                label = { Text(stringResource(R.string.search_field_max_price)) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.search_section_amenities),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            items(Amenities.ALL) { amenity ->
                FilterChip(
                    selected = form.amenities.contains(amenity),
                    onClick = { onAmenityToggle(amenity) },
                    label = { Text(amenityLabel(amenity)) },
                )
            }
        }

        OrderByDropdown(
            value = form.orderBy,
            onChange = onOrderByChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun amenityLabel(amenity: String): String = when (amenity) {
    Amenities.WIFI -> stringResource(R.string.search_amenity_wifi)
    Amenities.POOL -> stringResource(R.string.search_amenity_pool)
    Amenities.BREAKFAST -> stringResource(R.string.search_amenity_breakfast)
    Amenities.AIR_CONDITIONING -> stringResource(R.string.search_amenity_air_conditioning)
    Amenities.PET_FRIENDLY -> stringResource(R.string.search_amenity_pet_friendly)
    Amenities.PARKING -> stringResource(R.string.search_amenity_parking)
    Amenities.GYM -> stringResource(R.string.search_amenity_gym)
    Amenities.SPA -> stringResource(R.string.search_amenity_spa)
    else -> amenity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderByDropdown(
    value: SearchOrderBy?,
    onChange: (SearchOrderBy?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = orderByLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.search_field_order_by)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_order_none)) },
                onClick = { onChange(null); expanded = false },
            )
            SearchOrderBy.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(orderByLabel(option)) },
                    onClick = { onChange(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun orderByLabel(value: SearchOrderBy?): String = when (value) {
    SearchOrderBy.PRICE -> stringResource(R.string.search_order_by_price)
    SearchOrderBy.RATING -> stringResource(R.string.search_order_by_rating)
    SearchOrderBy.NAME -> stringResource(R.string.search_order_by_name)
    null -> stringResource(R.string.search_order_none)
}
