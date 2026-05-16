package com.uniandes.travelhub.ui.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.ui.auth.components.PasswordStrengthMeter
import com.uniandes.travelhub.ui.auth.components.TravelHubTextField
import com.uniandes.travelhub.ui.theme.Slate400
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.RegisterFormState

/**
 * Form body for the Register screen. Renders the fields applicable to the
 * current [RegisterFormState.role]: travelers see name + contact data, hotel
 * partners additionally see a hotel name field at the top.
 */
@Composable
fun RegisterForm(
    state: RegisterFormState,
    isError: Boolean,
    onFullNameChange: (String) -> Unit,
    onHotelNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        if (state.role == UserRole.HOTEL_PARTNER) {
            TravelHubTextField(
                value = state.hotelName,
                onValueChange = onHotelNameChange,
                label = stringResource(R.string.auth_register_hotel_name),
                placeholder = stringResource(R.string.auth_register_hotel_name_placeholder),
                leadingIcon = {
                    Icon(Icons.Filled.Apartment, contentDescription = null, tint = Slate400)
                },
                isError = isError,
            )
        }

        TravelHubTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = stringResource(
                if (state.role == UserRole.HOTEL_PARTNER) R.string.auth_register_contact_name
                else R.string.auth_register_full_name
            ),
            placeholder = stringResource(
                if (state.role == UserRole.HOTEL_PARTNER) R.string.auth_register_contact_name_placeholder
                else R.string.auth_register_full_name_placeholder
            ),
            leadingIcon = {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Slate400)
            },
            isError = isError,
        )

        TravelHubTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_register_email),
            placeholder = stringResource(R.string.auth_register_email_placeholder),
            leadingIcon = {
                Icon(Icons.Filled.Email, contentDescription = null, tint = Slate400)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = isError,
        )

        TravelHubTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.auth_register_phone),
            placeholder = stringResource(R.string.auth_register_phone_placeholder),
            leadingIcon = {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = Slate400)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = isError,
        )

        TravelHubTextField(
            value = state.countryCode,
            onValueChange = onCountryCodeChange,
            label = stringResource(R.string.auth_register_country),
            placeholder = stringResource(R.string.auth_register_country_placeholder),
            leadingIcon = {
                Icon(Icons.Filled.Public, contentDescription = null, tint = Slate400)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            isError = isError,
        )

        Column {
            PasswordField(
                value = state.password,
                onValueChange = onPasswordChange,
                isError = isError,
            )
            if (state.password.isNotEmpty()) {
                PasswordStrengthMeter(
                    password = state.password,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.agreedToTerms,
                onCheckedChange = onTermsChange,
            )
            Text(
                text = stringResource(R.string.auth_register_terms),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = MaterialTheme.spacing.xs),
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val showText = stringResource(R.string.auth_login_show_password)
    val hideText = stringResource(R.string.auth_login_hide_password)

    TravelHubTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_register_password),
        placeholder = stringResource(R.string.auth_register_password_placeholder),
        leadingIcon = {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Slate400)
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) hideText else showText,
                    modifier = Modifier.size(20.dp),
                    tint = Slate400,
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = isError,
    )
}

