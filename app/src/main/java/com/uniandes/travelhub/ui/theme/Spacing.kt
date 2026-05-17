package com.uniandes.travelhub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp-based spacing scale, exposed as a CompositionLocal so feature code can
 * use `MaterialTheme.spacing.md` instead of hardcoding `16.dp` everywhere.
 */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
