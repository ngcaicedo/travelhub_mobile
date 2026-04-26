package com.uniandes.travelhub.ui.auth.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uniandes.travelhub.R
import com.uniandes.travelhub.ui.theme.TravelhubTheme

data class LanguageOption(
    val tag: String,
    @StringRes val labelRes: Int,
)

val DefaultLanguageOptions = listOf(
    LanguageOption(tag = "es", labelRes = R.string.common_language_spanish),
    LanguageOption(tag = "en", labelRes = R.string.common_language_english),
    LanguageOption(tag = "pt", labelRes = R.string.common_language_portuguese),
)

@Composable
fun LanguageSwitcher(
    currentTag: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<LanguageOption> = DefaultLanguageOptions,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = stringResource(R.string.common_change_language),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        expanded = false
                        if (option.tag != currentTag) onLanguageSelected(option.tag)
                    },
                    trailingIcon = {
                        if (option.tag == currentTag) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LanguageSwitcherPreview() {
    TravelhubTheme {
        LanguageSwitcher(
            currentTag = "es",
            onLanguageSelected = {},
        )
    }
}
