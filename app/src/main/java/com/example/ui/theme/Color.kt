package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ============================================================================
// Ad-Deen design tokens: a time-aware palette.
//   Day   (Fajr -> Maghrib): warm cream and amber glow, brown ink.
//   Night (Maghrib -> Fajr, or system dark mode): deep indigo and gold.
// AppColors reads from a snapshot state, so every composable that uses AppColors.X
// recomposes when the palette changes (animated in AdDinTheme).
// ============================================================================

data class AdDeenPalette(
    val background: Color,
    val backgroundGradientTop: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val warmTintContainer: Color,
    val warmTintAlternate: Color,
    val borderSubtle: Color,
    val borderMedium: Color,
    val divider: Color,
    val shadow: Color,
    val glow: Color,
    val disabledTrack: Color,
    val progressTrack: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val skyline: Color,
    val isNight: Boolean
)

// Day: soft warm paper, amber glow, brown ink (Deen-style reference). Depth comes from tone, not borders.
val DayPalette = AdDeenPalette(
    background = Color(0xFFFBF6EC),
    backgroundGradientTop = Color(0xFFFCEBD0),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFDF8),
    primary = Color(0xFFF2A93B),
    onPrimary = Color(0xFF3A2410),
    secondary = Color(0xFF874D14),
    onSecondary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF2B2118),
    textSecondary = Color(0xFF8A7A69),
    warmTintContainer = Color(0xFFF6EEE0),
    warmTintAlternate = Color(0xFFF1E4CF),
    borderSubtle = Color(0x14874D14),
    borderMedium = Color(0x2E874D14),
    divider = Color(0x12874D14),
    shadow = Color(0x1A9C6A2C),
    glow = Color(0xFFF6C77A),
    disabledTrack = Color(0xFFE7DDCB),
    progressTrack = Color(0xFFEFE4D0),
    heroTop = Color(0xFFF7C98A),
    heroBottom = Color(0xFFFBF6EC),
    skyline = Color(0xFFEFC184),
    isNight = false
)

// Night: the same warm language in dark: espresso surfaces, amber light, cream ink.
val NightPalette = AdDeenPalette(
    background = Color(0xFF17120E),
    backgroundGradientTop = Color(0xFF2A1E12),
    surface = Color(0xFF221B15),
    surfaceElevated = Color(0xFF2A221B),
    primary = Color(0xFFF2B35A),
    onPrimary = Color(0xFF2A1A08),
    secondary = Color(0xFFE9C38A),
    onSecondary = Color(0xFF1A120A),
    textPrimary = Color(0xFFF4EBDD),
    textSecondary = Color(0xFFB5A690),
    warmTintContainer = Color(0xFF2C241C),
    warmTintAlternate = Color(0xFF352B21),
    borderSubtle = Color(0x1AF2B35A),
    borderMedium = Color(0x33F2B35A),
    divider = Color(0x14FFFFFF),
    shadow = Color(0x66000000),
    glow = Color(0xFFC98A3A),
    disabledTrack = Color(0xFF3A3027),
    progressTrack = Color(0xFF33291F),
    heroTop = Color(0xFF7A4E1E),
    heroBottom = Color(0xFF17120E),
    skyline = Color(0xFF5C4026),
    isNight = true
)

fun lerpPalette(a: AdDeenPalette, b: AdDeenPalette, t: Float): AdDeenPalette = AdDeenPalette(
    background = lerp(a.background, b.background, t),
    backgroundGradientTop = lerp(a.backgroundGradientTop, b.backgroundGradientTop, t),
    surface = lerp(a.surface, b.surface, t),
    surfaceElevated = lerp(a.surfaceElevated, b.surfaceElevated, t),
    primary = lerp(a.primary, b.primary, t),
    onPrimary = lerp(a.onPrimary, b.onPrimary, t),
    secondary = lerp(a.secondary, b.secondary, t),
    onSecondary = lerp(a.onSecondary, b.onSecondary, t),
    textPrimary = lerp(a.textPrimary, b.textPrimary, t),
    textSecondary = lerp(a.textSecondary, b.textSecondary, t),
    warmTintContainer = lerp(a.warmTintContainer, b.warmTintContainer, t),
    warmTintAlternate = lerp(a.warmTintAlternate, b.warmTintAlternate, t),
    borderSubtle = lerp(a.borderSubtle, b.borderSubtle, t),
    borderMedium = lerp(a.borderMedium, b.borderMedium, t),
    divider = lerp(a.divider, b.divider, t),
    shadow = lerp(a.shadow, b.shadow, t),
    glow = lerp(a.glow, b.glow, t),
    disabledTrack = lerp(a.disabledTrack, b.disabledTrack, t),
    progressTrack = lerp(a.progressTrack, b.progressTrack, t),
    heroTop = lerp(a.heroTop, b.heroTop, t),
    heroBottom = lerp(a.heroBottom, b.heroBottom, t),
    skyline = lerp(a.skyline, b.skyline, t),
    isNight = t >= 0.5f
)

object AppColors {
    /** Current palette (set by AdDinTheme). Reading any token subscribes the caller to palette changes. */
    var palette by mutableStateOf(DayPalette)

    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceElevated get() = palette.surfaceElevated

    val Primary get() = palette.primary
    val OnPrimary get() = palette.onPrimary

    val Secondary get() = palette.secondary
    val OnSecondary get() = palette.onSecondary

    val TextPrimary get() = palette.textPrimary
    val TextSecondary get() = palette.textSecondary

    val WarmTintContainer get() = palette.warmTintContainer
    val WarmTintAlternate get() = palette.warmTintAlternate

    val BorderSubtle get() = palette.borderSubtle
    val BorderMedium get() = palette.borderMedium
    val BorderFull get() = palette.secondary
    val Divider get() = palette.divider

    val CardShadow get() = palette.shadow
    val Glow get() = palette.glow

    val DisabledTrack get() = palette.disabledTrack
    val ProgressTrack get() = palette.progressTrack

    val IsNight get() = palette.isNight

    /** Page background: a soft vertical wash instead of a flat fill. */
    val BackgroundBrush: Brush get() = Brush.verticalGradient(0f to palette.backgroundGradientTop, 0.35f to palette.background, 1f to palette.background)
    /** Signature amber (day) / gold (night) gradient for primary actions and highlights. */
    val PrimaryGradient: Brush get() = Brush.horizontalGradient(listOf(palette.primary, lerp(palette.primary, palette.secondary, 0.18f)))
    /** Filled action colour (Save, Continue): brand brown by day, amber at night. */
    val Action get() = if (palette.isNight) palette.primary else palette.secondary
    val OnAction get() = if (palette.isNight) palette.onPrimary else palette.onSecondary
}

// Legacy aliases kept for older call sites; they follow the live palette.
val JamatGreen get() = AppColors.Primary
val AloneBlue get() = AppColors.Primary
val QazaAmber get() = AppColors.Secondary
val MissedRed get() = AppColors.Secondary
val PerfectGold get() = AppColors.Primary
