package com.resilience.app.ui.theme

import androidx.compose.ui.graphics.Color

// Metro 2033 Tactical Survival Palette
val TacticalBackground = Color(0xFF1A1A1A)
val TacticalSurface = Color(0xFF2A2A2A)
val TacticalMutedBackground = Color(0xFF383838)  // --muted: disabled/subtle backgrounds
val TacticalBorder = Color(0xFF3A3A3A)            // --border: card/panel borders
val TacticalPrimaryRust = Color(0xFFB85C38)
val TacticalSecondaryOlive = Color(0xFF4A5A3A)
val TacticalAccentAmber = Color(0xFFD4A017)
val TacticalDestructive = Color(0xFFC42B2B)       // --destructive: HSL 0 70% 45%
val TacticalText = Color(0xFFD4D0C8)
val TacticalMuted = Color(0xFF8A8A7A)

// SafeReach Palette (Legacy)
val SafeReachDarkGray = Color(0xFF2D2D2D)
val SafeReachOffWhite = Color(0xFFF5F5F5)
val SafeReachBackground = Color(0xFFFFFFFF)
val SafeReachSurface = Color(0xFF2D2D2D)
val SafeReachOnSurface = Color(0xFFFFFFFF)
val SafeReachLinkGray = Color(0xFF757575)

// Normal Mode Palette
val NormalPrimary = TacticalPrimaryRust
val NormalSecondary = TacticalSecondaryOlive
val NormalTertiary = TacticalAccentAmber
val NormalBackground = TacticalBackground
val NormalSurface = TacticalSurface

// Crisis Mode Palette (High Contrast & Monochrome)
val CrisisBackground = Color(0xFF000000)
val CrisisSurface = Color(0xFF121212)
val CrisisPrimary = TacticalAccentAmber
val CrisisSecondary = TacticalSecondaryOlive
val CrisisTertiary = Color(0xFFBDBDBD)
val CrisisError = TacticalDestructive

// Extreme Battery Mode (OLED Black)
val ExtremeBackground = Color(0xFF000000)
val ExtremeSurface = Color(0xFF000000)
val ExtremeText = Color(0xFFE0E0E0)
