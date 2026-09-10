package dev.milan.cryptogram.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The design pairs a literary serif (Spectral) with a technical monospace
 * (IBM Plex Mono). We approximate with the platform families so no font
 * download / bundled asset is required; the roles are what matter.
 */
val Serif = FontFamily.Serif
val Mono = FontFamily.Monospace

/** Wide-tracked uppercase mono label, the design's workhorse caption style. */
fun monoLabel(size: Double = 10.0, tracking: Double = 0.16, medium: Boolean = false) = TextStyle(
    fontFamily = Mono,
    fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal,
    fontSize = size.sp,
    letterSpacing = tracking.em,
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 46.sp, lineHeight = 47.sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 42.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 25.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 21.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.12.em),
    titleSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.16.em),
    bodyLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.12.em),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.18.em),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.16.em),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 9.sp, letterSpacing = 0.14.em),
)
