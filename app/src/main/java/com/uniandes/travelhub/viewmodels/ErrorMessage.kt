package com.uniandes.travelhub.viewmodels

import androidx.annotation.StringRes

/**
 * Wraps a user-facing error message produced by a ViewModel so the UI layer
 * can resolve it against the current locale. ViewModels run on JVM-only tests
 * and have no Context, so they cannot call `stringResource` directly. Instead
 * they emit one of:
 *
 *  - [Resource]: a [StringRes] id (and optional args) for translated copy.
 *  - [Plain]: an opaque string already produced by an external system (e.g.
 *    a backend error body) that should be displayed as-is.
 */
sealed interface ErrorMessage {
    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : ErrorMessage

    data class Plain(val text: String) : ErrorMessage
}
