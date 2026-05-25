package com.calorietracker.presentation.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Dark palette
// =============================================================================
//
// Layered surface system (M3 dark convention: higher elevation = lighter):
//   background          #0F1923  the page canvas
//   surface             #19273A  cards, app bar
//   surfaceVariant      #233247  raised tonal surfaces (chips, picker rows)
//   surfaceContainer    #1F2D40  intermediate
//   surfaceContainerHigh #283A52  selection backgrounds
internal val DarkBackground = Color(0xFF0F1923)
internal val DarkSurface = Color(0xFF19273A)
internal val DarkSurfaceVariant = Color(0xFF233247)
internal val DarkSurfaceContainer = Color(0xFF1F2D40)
internal val DarkSurfaceContainerHigh = Color(0xFF283A52)

internal val DarkOnBackground = Color(0xFFE8EEF4)
internal val DarkOnSurface = Color(0xFFE8EEF4)
internal val DarkOnSurfaceVariant = Color(0xFFA9C0D5)

internal val DarkPrimary = Color(0xFF4DA3F5)
internal val DarkOnPrimary = Color(0xFF0A1929)
internal val DarkPrimaryContainer = Color(0xFF1F3A5F)
internal val DarkOnPrimaryContainer = Color(0xFFD0E4FF)

internal val DarkSecondary = Color(0xFF67E8C9)
internal val DarkOnSecondary = Color(0xFF002820)
internal val DarkSecondaryContainer = Color(0xFF1B4138)
internal val DarkOnSecondaryContainer = Color(0xFFB7F5E2)

internal val DarkTertiary = Color(0xFFE5BEFC)
internal val DarkOnTertiary = Color(0xFF2E1444)
internal val DarkTertiaryContainer = Color(0xFF44285C)
internal val DarkOnTertiaryContainer = Color(0xFFF1DBFF)

internal val DarkOutline = Color(0xFF3F556C)
internal val DarkOutlineVariant = Color(0xFF2A3B4F)

internal val DarkError = Color(0xFFEF5350)
internal val DarkOnError = Color(0xFF410002)
internal val DarkErrorContainer = Color(0xFF5C1818)
internal val DarkOnErrorContainer = Color(0xFFFFB4A8)

// Extended dark — macros, status
internal val DarkProtein = Color(0xFFA78BFA)         // violet
internal val DarkOnProtein = Color(0xFF1B0F44)
internal val DarkCarbs = Color(0xFF34D399)           // emerald
internal val DarkOnCarbs = Color(0xFF022C1E)
internal val DarkFat = Color(0xFFFBBF24)             // amber
internal val DarkOnFat = Color(0xFF3A2602)
internal val DarkSuccess = Color(0xFF5EEAD4)         // teal — distinct from carbs emerald
internal val DarkWarning = Color(0xFFFB923C)         // orange — distinct from fat amber

// =============================================================================
// Light palette
// =============================================================================
internal val LightBackground = Color(0xFFF7FAFC)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFEEF3F8)
internal val LightSurfaceContainer = Color(0xFFF1F5F9)
internal val LightSurfaceContainerHigh = Color(0xFFE6EDF5)

internal val LightOnBackground = Color(0xFF0F1923)
internal val LightOnSurface = Color(0xFF0F1923)
internal val LightOnSurfaceVariant = Color(0xFF4A5A6E)

internal val LightPrimary = Color(0xFF1A6FD0)
internal val LightOnPrimary = Color(0xFFFFFFFF)
internal val LightPrimaryContainer = Color(0xFFD9E8FB)
internal val LightOnPrimaryContainer = Color(0xFF0A2D5C)

internal val LightSecondary = Color(0xFF0E7C66)
internal val LightOnSecondary = Color(0xFFFFFFFF)
internal val LightSecondaryContainer = Color(0xFFC5F1E2)
internal val LightOnSecondaryContainer = Color(0xFF002820)

internal val LightTertiary = Color(0xFF7B3FB3)
internal val LightOnTertiary = Color(0xFFFFFFFF)
internal val LightTertiaryContainer = Color(0xFFEEDDFF)
internal val LightOnTertiaryContainer = Color(0xFF2E1444)

internal val LightOutline = Color(0xFFC2CCD9)
internal val LightOutlineVariant = Color(0xFFE2E8F0)

internal val LightError = Color(0xFFD32F2F)
internal val LightOnError = Color(0xFFFFFFFF)
internal val LightErrorContainer = Color(0xFFFFDAD6)
internal val LightOnErrorContainer = Color(0xFF410002)

// Extended light — same hue families, deeper saturation for white background contrast
internal val LightProtein = Color(0xFF7C3AED)        // violet
internal val LightOnProtein = Color(0xFFFFFFFF)
internal val LightCarbs = Color(0xFF059669)          // emerald
internal val LightOnCarbs = Color(0xFFFFFFFF)
internal val LightFat = Color(0xFFD97706)            // amber
internal val LightOnFat = Color(0xFFFFFFFF)
internal val LightSuccess = Color(0xFF0D9488)        // teal
internal val LightWarning = Color(0xFFEA580C)        // orange
