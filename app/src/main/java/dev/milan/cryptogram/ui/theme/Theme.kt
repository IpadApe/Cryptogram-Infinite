package dev.milan.cryptogram.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Extra roles the Material scheme has no slot for (design canvas). */
@Immutable
data class CryptoColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val accent: Color,
    val accentPressed: Color,
    val bad: Color,
    val onAccent: Color,
    val tileUnsolved: Color,
    val keyboardBg: Color,
    val key: Color,
    val keyUsed: Color,
    val keySpecial: Color,
    val divider: Color,
    val muted: Color,
    val dark: Boolean,
)

private val LightExtra = CryptoColors(
    paper = PaperLight, card = CardLight, ink = InkLight,
    accent = AccentLight, accentPressed = AccentPressedLight, bad = BadLight,
    onAccent = OnAccentLight, tileUnsolved = TileUnsolvedLight,
    keyboardBg = KeyboardBgLight, key = KeyLight, keyUsed = KeyUsedLight,
    keySpecial = KeySpecialLight, divider = DividerLight, muted = MutedLight, dark = false,
)

private val DarkExtra = CryptoColors(
    paper = PaperDark, card = CardDark, ink = InkDark,
    accent = AccentDark, accentPressed = AccentPressedDark, bad = BadDark,
    onAccent = OnAccentDark, tileUnsolved = TileUnsolvedDark,
    keyboardBg = KeyboardBgDark, key = KeyDark, keyUsed = KeyUsedDark,
    keySpecial = KeySpecialDark, divider = DividerDark, muted = MutedDark, dark = true,
)

val LocalCryptoColors = staticCompositionLocalOf { LightExtra }

/** Puzzle-grid cell side length; set by the Play screen once it measures width. */
val LocalCellSize = staticCompositionLocalOf { 40.dp }

/** Convenience accessor: `CryptoTheme.colors.accent`. */
object CryptoTheme {
    val colors: CryptoColors
        @Composable get() = LocalCryptoColors.current
}

private fun scheme(e: CryptoColors) = if (e.dark) {
    darkColorScheme(
        primary = e.accent, onPrimary = e.onAccent,
        primaryContainer = e.accent, onPrimaryContainer = e.onAccent,
        secondary = e.accent, onSecondary = e.onAccent,
        secondaryContainer = e.tileUnsolved, onSecondaryContainer = e.ink,
        background = e.paper, onBackground = e.ink,
        surface = e.paper, onSurface = e.ink,
        surfaceVariant = e.card, onSurfaceVariant = e.muted,
        error = e.bad, onError = e.onAccent,
        errorContainer = e.tileUnsolved, onErrorContainer = e.bad,
        outline = e.divider, outlineVariant = e.divider,
    )
} else {
    lightColorScheme(
        primary = e.accent, onPrimary = e.onAccent,
        primaryContainer = e.accent, onPrimaryContainer = e.onAccent,
        secondary = e.accent, onSecondary = e.onAccent,
        secondaryContainer = e.tileUnsolved, onSecondaryContainer = e.ink,
        background = e.paper, onBackground = e.ink,
        surface = e.paper, onSurface = e.ink,
        surfaceVariant = e.card, onSurfaceVariant = e.muted,
        error = e.bad, onError = e.onAccent,
        errorContainer = e.tileUnsolved, onErrorContainer = e.bad,
        outline = e.divider, outlineVariant = e.divider,
    )
}

@Composable
fun CryptogramInfiniteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extra = if (darkTheme) DarkExtra else LightExtra
    CompositionLocalProvider(LocalCryptoColors provides extra) {
        MaterialTheme(
            colorScheme = scheme(extra),
            typography = Typography,
            content = content,
        )
    }
}
