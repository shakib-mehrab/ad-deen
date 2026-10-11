package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun colorSchemeFor(p: AdDeenPalette) = if (p.isNight) {
    darkColorScheme(
        primary = p.primary, onPrimary = p.onPrimary,
        primaryContainer = p.warmTintContainer, onPrimaryContainer = p.secondary,
        secondary = p.secondary, onSecondary = p.onSecondary,
        secondaryContainer = p.warmTintAlternate, onSecondaryContainer = p.textPrimary,
        tertiary = p.secondary, onTertiary = p.onSecondary,
        tertiaryContainer = p.warmTintContainer, onTertiaryContainer = p.secondary,
        background = p.background, onBackground = p.textPrimary,
        surface = p.surface, onSurface = p.textPrimary,
        surfaceVariant = p.warmTintContainer, onSurfaceVariant = p.textSecondary,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surfaceElevated, surfaceContainerLow = p.surface,
        outline = p.borderMedium, outlineVariant = p.divider
    )
} else {
    lightColorScheme(
        // Filled buttons, radios, sliders use the brand brown by day (amber stays an accent).
        primary = p.secondary, onPrimary = p.onSecondary,
        primaryContainer = p.warmTintContainer, onPrimaryContainer = p.secondary,
        secondary = p.secondary, onSecondary = p.onSecondary,
        secondaryContainer = p.warmTintAlternate, onSecondaryContainer = p.textPrimary,
        tertiary = p.secondary, onTertiary = p.onSecondary,
        tertiaryContainer = p.warmTintContainer, onTertiaryContainer = p.secondary,
        background = p.background, onBackground = p.textPrimary,
        surface = p.surface, onSurface = p.textPrimary,
        surfaceVariant = p.warmTintContainer, onSurfaceVariant = p.textSecondary,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surfaceElevated, surfaceContainerLow = p.surface,
        outline = p.borderMedium, outlineVariant = p.divider
    )
}

/**
 * Time-aware theme. [isNight] comes only from the prayer schedule (Maghrib -> Fajr), not the system dark mode.
 * The switch cross-fades over ~1.2 s so the whole app "turns to night" smoothly.
 * [lang] picks the font family: Poppins for English, Hind Siliguri for Bangla.
 */
@Composable
fun AdDinTheme(
    isNight: Boolean = false,
    lang: String = "en",
    content: @Composable () -> Unit
) {
    val t by animateFloatAsState(if (isNight) 1f else 0f, animationSpec = tween(1200), label = "day_night")
    val palette = when (t) {
        0f -> DayPalette
        1f -> NightPalette
        else -> lerpPalette(DayPalette, NightPalette, t)
    }
    AppColors.palette = palette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.isNight
                isAppearanceLightNavigationBars = !palette.isNight
            }
        }
    }

    MaterialTheme(colorScheme = colorSchemeFor(palette), typography = androidx.compose.runtime.remember(lang) { appTypography(lang) }, content = content)
}
