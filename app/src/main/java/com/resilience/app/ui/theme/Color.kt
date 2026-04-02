package com.resilience.app.ui.theme

import androidx.compose.ui.graphics.Color

// SafeReach Palette (Modern Flat Design)
val SafeReachDarkGray = Color(0xFF2D2D2D)
val SafeReachOffWhite = Color(0xFFF5F5F5)
val SafeReachBackground = Color(0xFFFFFFFF)
val SafeReachSurface = Color(0xFF2D2D2D)
val SafeReachOnSurface = Color(0xFFFFFFFF)
val SafeReachLinkGray = Color(0xFF757575)

// Normal Mode Palette (Calm & Trustworthy)
val NormalPrimary = SafeReachDarkGray
val NormalSecondary = SafeReachLinkGray
val NormalTertiary = Color(0xFF00C853)
val NormalBackground = SafeReachOffWhite
val NormalSurface = SafeReachBackground

// Crisis Mode Palette (High Contrast & Monochrome)
val CrisisBackground = Color(0xFF000000)
val CrisisSurface = Color(0xFF121212)
val CrisisPrimary = Color(0xFFFFFFFF)
val CrisisSecondary = Color(0xFFE0E0E0)
val CrisisTertiary = Color(0xFFBDBDBD)
val CrisisError = Color(0xFFFFFFFF) // High contrast white for errors in crisis mode

// Extreme Battery Mode (OLED Black)
val ExtremeBackground = Color(0xFF000000)
val ExtremeSurface = Color(0xFF000000)
val ExtremeText = Color(0xFFE0E0E0)
