package com.resilience.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NormalColorScheme = lightColorScheme(
    primary = NormalPrimary,
    secondary = NormalSecondary,
    tertiary = NormalTertiary,
    background = NormalBackground,
    surface = NormalSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    surfaceVariant = SafeReachDarkGray,
    onSurfaceVariant = Color.White
)

private val CrisisColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.Gray,
    tertiary = Color.LightGray,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
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
