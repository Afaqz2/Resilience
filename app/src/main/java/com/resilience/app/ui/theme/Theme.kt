package com.resilience.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NormalColorScheme = darkColorScheme(
    primary = TacticalPrimaryRust,
    secondary = TacticalSecondaryOlive,
    tertiary = TacticalAccentAmber,
    background = TacticalBackground,
    surface = TacticalSurface,
    onPrimary = TacticalBackground,
    onSecondary = TacticalBackground,
    onTertiary = TacticalBackground,
    onBackground = TacticalText,
    onSurface = TacticalText,
    surfaceVariant = TacticalSurface,
    onSurfaceVariant = TacticalText,
    error = TacticalDestructive
)

private val CrisisColorScheme = darkColorScheme(
    primary = TacticalAccentAmber,
    secondary = TacticalSecondaryOlive,
    tertiary = TacticalPrimaryRust,
    background = CrisisBackground,
    surface = CrisisSurface,
    onPrimary = CrisisBackground,
    onSecondary = CrisisBackground,
    onTertiary = CrisisBackground,
    onBackground = CrisisPrimary,
    onSurface = CrisisPrimary,
    error = CrisisError
)

@Composable
fun ResilienceTheme(
    isCrisisMode: Boolean = false,
    isExtremeBatteryMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isExtremeBatteryMode -> CrisisColorScheme // Same as crisis but black background
        isCrisisMode -> CrisisColorScheme
        else -> NormalColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Use background color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isCrisisMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
