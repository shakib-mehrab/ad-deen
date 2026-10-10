package com.example.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** English UI: Poppins (geometric, matches the Deen-style references). Real weights, no fake bold. */
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/** Bangla UI: Hind Siliguri in four real weights (also covers Latin glyphs). */
val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold)
)

/** Tall condensed numerals for the hero clock (the phone's built-in Roboto Condensed). */
val CondensedNumeralsFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

/** Type scale. Calm hierarchy: few sizes, Medium/SemiBold instead of heavy Bold everywhere. */
fun appTypography(lang: String): Typography {
    val f = if (lang == "bn") HindSiliguriFontFamily else PoppinsFontFamily
    return Typography(
        displayLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 60.sp, letterSpacing = (-0.5).sp),
        displayMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 50.sp),
        displaySmall = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
        headlineLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
        headlineSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
        titleLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
        titleSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp)
    )
}

/** Default (English) typography for previews and tests. */
val Typography = appTypography("en")
