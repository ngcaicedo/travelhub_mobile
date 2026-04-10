package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.ui.theme.Slate200
import com.uniandes.travelhub.ui.theme.Slate400
import com.uniandes.travelhub.ui.theme.TravelhubPillShape
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.spacing

@Composable
fun TravelHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.xs,
                bottom = MaterialTheme.spacing.xs
            )
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = placeholder?.let { { Text(it, color = Slate400) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = TravelhubPillShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Slate200,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                disabledBorderColor = Slate200,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun TravelHubTextFieldPreview() {
    TravelhubTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            TravelHubTextField(
                value = "ada@example.com",
                onValueChange = {},
                label = "Correo electrónico",
                placeholder = "ejemplo@correo.com",
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun TravelHubTextFieldEmptyWithErrorPreview() {
    TravelhubTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TravelHubTextField(
                value = "",
                onValueChange = {},
                label = "Contraseña",
                placeholder = "••••••••",
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                isError = true
            )
        }
    }
}
