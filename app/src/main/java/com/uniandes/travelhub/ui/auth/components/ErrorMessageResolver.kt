package com.uniandes.travelhub.ui.auth.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uniandes.travelhub.viewmodels.ErrorMessage

/**
 * Resolves an [ErrorMessage] produced by a ViewModel into a localised string
 * that can be passed to UI components like [ErrorBanner].
 */
@Composable
fun ErrorMessage.asString(): String = when (this) {
    is ErrorMessage.Resource -> if (args.isEmpty()) {
        stringResource(id)
    } else {
        stringResource(id, *args.toTypedArray())
    }
    is ErrorMessage.Plain -> text
}
