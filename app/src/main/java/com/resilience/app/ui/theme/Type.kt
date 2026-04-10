package com.resilience.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.resilience.app.R

val IBMPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_bold, FontWeight.Bold)
)

val ShareTechMono = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

// Tactical Typography mappings
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ShareTechMono,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ShareTechMono, // Stencil / Labels
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.4.sp  // ~0.15em stencil tracking
    ),
    labelMedium = TextStyle(
        fontFamily = ShareTechMono, // Stencil / Labels
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp  // ~0.15em stencil tracking
    ),
    labelLarge = TextStyle(
        fontFamily = ShareTechMono, // Stencil / Labels
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.8.sp  // ~0.15em stencil tracking
    ),
    titleSmall = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    displayLarge = TextStyle(
        fontFamily = IBMPlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
)
