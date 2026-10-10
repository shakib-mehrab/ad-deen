package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Locked Palette for "Ad-Deen" (Material 3 / Jetpack Compose)
// ============================================================================
object AppColors {
    // Canvas / Scaffold
    val Background = Color(0xFFFFFFE4)
    val Surface = Color(0xFFFFFFFF)

    // Primary Accents
    val Primary = Color(0xFFFFB030)
    val OnPrimary = Color(0xFF1F1F1F)

    // Secondary / Brand
    val Secondary = Color(0xFF874D14)
    val OnSecondary = Color(0xFFFFFFFF)

    // Text Tokens
    val TextPrimary = Color(0xFF1F1F1F)
    val TextSecondary = Color(0xFF5A4632)

    // Warm Containers
    val WarmTintContainer = Color(0xFFFFF0CC)
    val WarmTintAlternate = Color(0xFFFFE8D6)

    // Borders & Dividers
    val BorderSubtle = Color(0x4D874D14) // 30% alpha of #874D14
    val BorderMedium = Color(0x66874D14) // 40% alpha of #874D14
    val BorderFull = Color(0xFF874D14)
    val Divider = Color(0x33874D14)      // 20% alpha of #874D14

    // Shadows
    val CardShadow = Color(0x14874D14)   // 8% alpha of #874D14

    // Component States
    val DisabledTrack = Color(0xFFE3D9C0)
    val ProgressTrack = Color(0xFFF0E6C8)
}

// ColorScheme mappings
val BackgroundLight = AppColors.Background
val OnBackgroundLight = AppColors.TextPrimary
val SurfaceLight = AppColors.Surface
val OnSurfaceLight = AppColors.TextPrimary
val SurfaceVariantLight = AppColors.WarmTintContainer
val OnSurfaceVariantLight = AppColors.TextSecondary

val PrimaryLight = AppColors.Primary
val OnPrimaryLight = AppColors.OnPrimary
val PrimaryContainerLight = AppColors.WarmTintContainer
val OnPrimaryContainerLight = AppColors.Secondary

val SecondaryLight = AppColors.Secondary
val OnSecondaryLight = AppColors.OnSecondary
val SecondaryContainerLight = AppColors.WarmTintAlternate
val OnSecondaryContainerLight = AppColors.TextPrimary

val TertiaryLight = AppColors.Secondary
val OnTertiaryLight = AppColors.OnSecondary
val TertiaryContainerLight = AppColors.WarmTintContainer
val OnTertiaryContainerLight = AppColors.Secondary

val OutlineLight = AppColors.BorderSubtle
val OutlineVariantLight = AppColors.Divider

// Legacy aliases mapped strictly to locked palette
val JamatGreen = AppColors.Primary
val AloneBlue = AppColors.Primary
val QazaAmber = AppColors.Secondary
val MissedRed = AppColors.Secondary
val PerfectGold = AppColors.Primary
val PaletteBackground = AppColors.Background
val PalettePrimary = AppColors.Primary
val PaletteSecondary = AppColors.Secondary
val PaletteBlack = AppColors.TextPrimary
