package com.uniandes.travelhub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary              = TravelhubBlue500,
    onPrimary            = Color.White,
    primaryContainer     = TravelhubBlue100,
    onPrimaryContainer   = TravelhubBlue900,

    secondary            = TravelhubBlue700,
    onSecondary          = Color.White,
    secondaryContainer   = TravelhubBlue50,
    onSecondaryContainer = TravelhubBlue900,

    // Green is used as the accent / "success" hue in the brand.
    tertiary             = Success500,
    onTertiary           = Color.White,
    tertiaryContainer    = Success100,
    onTertiaryContainer  = Success900,

    background           = Color.White,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate100,
    onSurfaceVariant     = Slate700,
    surfaceTint          = TravelhubBlue500,

    outline              = Slate300,
    outlineVariant       = Slate200,

    error                = Error500,
    onError              = Color.White,
    errorContainer       = Error100,
    onErrorContainer     = Error900,
)

private val DarkColorScheme = darkColorScheme(
    primary              = TravelhubBlue400,
    onPrimary            = TravelhubBlue950,
    primaryContainer     = TravelhubBlue800,
    onPrimaryContainer   = TravelhubBlue100,

    secondary            = TravelhubBlue300,
    onSecondary          = TravelhubBlue950,
    secondaryContainer   = TravelhubBlue900,
    onSecondaryContainer = TravelhubBlue100,

    tertiary             = Success400,
    onTertiary           = Success950,
    tertiaryContainer    = Success800,
    onTertiaryContainer  = Success100,

    background           = Slate950,
    onBackground         = Slate50,
    surface              = Slate900,
    onSurface            = Slate50,
    surfaceVariant       = Slate800,
    onSurfaceVariant     = Slate300,
    surfaceTint          = TravelhubBlue400,

    outline              = Slate700,
    outlineVariant       = Slate800,

    error                = Error400,
    onError              = Error950,
    errorContainer       = Error800,
    onErrorContainer     = Error100,
)

@Composable
fun TravelhubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TravelhubShapes,
            content = content,
        )
    }
}
