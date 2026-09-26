package com.naviify.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NaviifyPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val isGlass: Boolean = false,
    val isLight: Boolean = false,
) {
    companion object {
        val Spotify = NaviifyPalette(
            background = Color(0xFF121212),
            surface = Color(0xFF181818),
            surfaceHigh = Color(0xFF282828),
            accent = Color(0xFF69B987),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB3B3B3),
            outline = Color(0xFF4D4D4D),
            isGlass = false,
            isLight = false,
        )
    }
}

val AsaysGreen = Color(0xFF69B987)
val AppleAccentRed = Color(0xFFFA2D48)
val BitChordCyan = Color(0xFF00E5FF)
val BitChordPurple = Color(0xFFB388FF)

val LocalNaviifyPalette = staticCompositionLocalOf { NaviifyPalette.Spotify }

// Theme-aware shortcuts read from LocalNaviifyPalette; NaviifyTheme swaps the
// whole palette so every screen follows the selected theme dynamically.
val NaviifyBlack: Color @Composable get() = LocalNaviifyPalette.current.background
val SurfaceCard: Color @Composable get() = LocalNaviifyPalette.current.surface
val SurfaceCardHigh: Color @Composable get() = LocalNaviifyPalette.current.surfaceHigh
val SpotifyGreen: Color @Composable get() = LocalNaviifyPalette.current.accent
val TextPrimary: Color @Composable get() = LocalNaviifyPalette.current.textPrimary
val TextSecondary: Color @Composable get() = LocalNaviifyPalette.current.textSecondary
val ThemeOutline: Color @Composable get() = LocalNaviifyPalette.current.outline
val IsGlassTheme: Boolean @Composable get() = LocalNaviifyPalette.current.isGlass
val IsLightTheme: Boolean @Composable get() = LocalNaviifyPalette.current.isLight
val ErrorRed = Color(0xFFE91429)
