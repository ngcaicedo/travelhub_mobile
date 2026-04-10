package com.uniandes.travelhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material3 only ships slots for `primary`, `secondary`, `tertiary` and `error`,
 * but the TravelHub design system also defines `success`, `warning` and `info`.
 *
 * We expose them via a separate CompositionLocal so feature code can write
 * `MaterialTheme.extendedColors.success` and still get the right value in
 * light vs dark mode.
 */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    success            = Success500,
    onSuccess          = Color.White,
    successContainer   = Success100,
    onSuccessContainer = Success900,
    warning            = Warning500,
    onWarning          = Color.White,
    warningContainer   = Warning100,
    onWarningContainer = Warning900,
    info               = TravelhubBlue500,
    onInfo             = Color.White,
    infoContainer      = TravelhubBlue100,
    onInfoContainer    = TravelhubBlue900,
)

val DarkExtendedColors = ExtendedColors(
    success            = Success400,
    onSuccess          = Success950,
    successContainer   = Success800,
    onSuccessContainer = Success100,
    warning            = Warning400,
    onWarning          = Warning950,
    warningContainer   = Warning800,
    onWarningContainer = Warning100,
    info               = TravelhubBlue400,
    onInfo             = TravelhubBlue950,
    infoContainer      = TravelhubBlue800,
    onInfoContainer    = TravelhubBlue100,
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("ExtendedColors not provided. Wrap your composables in TravelhubTheme { ... }.")
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
