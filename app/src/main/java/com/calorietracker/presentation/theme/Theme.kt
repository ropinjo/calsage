package com.calorietracker.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemePreference {
    System,
    Light,
    Dark;

    companion object {
        fun fromKey(key: String?): ThemePreference = when (key) {
            "light" -> Light
            "system" -> System
            else -> Dark
        }
    }

    val key: String
        get() = when (this) {
            System -> "system"
            Light -> "light"
            Dark -> "dark"
        }
}

/**
 * Non-M3 semantic colors. Macros use a triadic palette (violet / emerald / amber) so each
 * macro is distinguishable at a glance and from primary blue. Status colors sit outside
 * those hue bands to avoid macro/status collisions on shared screens.
 */
@Immutable
data class ExtendedColors(
    val protein: Color,
    val onProtein: Color,
    val carbs: Color,
    val onCarbs: Color,
    val fat: Color,
    val onFat: Color,
    val success: Color,
    val warning: Color
)

private val DarkExtendedColors = ExtendedColors(
    protein = DarkProtein,
    onProtein = DarkOnProtein,
    carbs = DarkCarbs,
    onCarbs = DarkOnCarbs,
    fat = DarkFat,
    onFat = DarkOnFat,
    success = DarkSuccess,
    warning = DarkWarning
)

private val LightExtendedColors = ExtendedColors(
    protein = LightProtein,
    onProtein = LightOnProtein,
    carbs = LightCarbs,
    onCarbs = LightOnCarbs,
    fat = LightFat,
    onFat = LightOnFat,
    success = LightSuccess,
    warning = LightWarning
)

val LocalExtendedColors = staticCompositionLocalOf {
    DarkExtendedColors
}

private val CalSageDarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color.Black
)

private val CalSageLightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color.Black
)

@Composable
fun CalSageTheme(
    themePreference: ThemePreference = ThemePreference.Dark,
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) CalSageDarkScheme else CalSageLightScheme
    val extended = if (useDark) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDark
            insetsController.isAppearanceLightNavigationBars = !useDark
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CalSageTypography,
            content = content
        )
    }
}

/**
 * Convenience accessor: `MaterialTheme.extendedColors.protein` etc.
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current
