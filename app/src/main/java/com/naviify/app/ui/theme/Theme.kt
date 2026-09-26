package com.naviify.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme

fun paletteFor(theme: AppTheme): NaviifyPalette = when (theme) {
    AppTheme.SPOTIFY -> NaviifyPalette.Spotify
    AppTheme.GLASS -> NaviifyPalette(
        background = Color(0xFF0C101B),
        surface = Color(0xCC162235),
        surfaceHigh = Color(0xDD22334D),
        accent = Color(0xFF38BDF8),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        outline = Color(0x4038BDF8),
        isGlass = true,
    )
    AppTheme.OLED -> NaviifyPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0A),
        surfaceHigh = Color(0xFF141414),
        accent = Color(0xFF69B987),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8E8E8E),
        outline = Color(0xFF222222),
        isGlass = false,
    )
    AppTheme.SUNSET -> NaviifyPalette(
        background = Color(0xFF13091B),
        surface = Color(0xFF1E102A),
        surfaceHigh = Color(0xFF2C183C),
        accent = Color(0xFFFF5E7E),
        textPrimary = Color(0xFFFFF0F5),
        textSecondary = Color(0xFFC792A6),
        outline = Color(0xFF4A2552),
        isGlass = false,
    )
    AppTheme.CYBERPUNK -> NaviifyPalette(
        background = Color(0xFF0C071E),
        surface = Color(0xFF170E38),
        surfaceHigh = Color(0xFF251854),
        accent = Color(0xFFE040FB),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFFB39DDB),
        outline = Color(0xFF4527A0),
        isGlass = false,
    )
    AppTheme.OCEANIC -> NaviifyPalette(
        background = Color(0xFF06101E),
        surface = Color(0xFF0C192D),
        surfaceHigh = Color(0xFF142742),
        accent = Color(0xFF00D2FF),
        textPrimary = Color(0xFFE6F4FE),
        textSecondary = Color(0xFF7FA1BF),
        outline = Color(0xFF1C3A5E),
        isGlass = false,
    )
    AppTheme.EMERALD -> NaviifyPalette(
        background = Color(0xFF08140E),
        surface = Color(0xFF0F1F17),
        surfaceHigh = Color(0xFF172E23),
        accent = Color(0xFF10B981),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFF86A795),
        outline = Color(0xFF1E4331),
        isGlass = false,
    )
    AppTheme.AMETHYST -> NaviifyPalette(
        background = Color(0xFF120B1C),
        surface = Color(0xFF1C122B),
        surfaceHigh = Color(0xFF2A1B40),
        accent = Color(0xFFC084FC),
        textPrimary = Color(0xFFF5F0FF),
        textSecondary = Color(0xFFA693BE),
        outline = Color(0xFF3F2B5E),
        isGlass = false,
    )
    AppTheme.MIDNIGHT -> NaviifyPalette(
        background = Color(0xFF0A0E17),
        surface = Color(0xFF111826),
        surfaceHigh = Color(0xFF1A2436),
        accent = Color(0xFF3B82F6),
        textPrimary = Color(0xFFEAF2FF),
        textSecondary = Color(0xFF93A5C1),
        outline = Color(0xFF2A3A55),
        isGlass = false,
    )
    AppTheme.TITANIUM -> NaviifyPalette(
        background = Color(0xFF121214),
        surface = Color(0xFF1C1C20),
        surfaceHigh = Color(0xFF28282E),
        accent = Color(0xFFE2E8F0),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF94A3B8),
        outline = Color(0xFF3F3F46),
        isGlass = false,
        isLight = false,
    )
    AppTheme.APPLE_DARK -> NaviifyPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0D0D0F),
        surfaceHigh = Color(0xFF1C1C1E),
        accent = Color(0xFFFA2D48),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8E8E93),
        outline = Color(0xFF2C2C2E),
        isGlass = false,
        isLight = false,
    )
    AppTheme.APPLE_LIGHT -> NaviifyPalette(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF7F7F9),
        surfaceHigh = Color(0xFFF2F2F7),
        accent = Color(0xFFFA2D48),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF6E6E73),
        outline = Color(0xFFE5E5EA),
        isGlass = false,
        isLight = true,
    )
    AppTheme.BITCHORD_CYAN -> NaviifyPalette(
        background = Color(0xFF060913),
        surface = Color(0xFF0E1426),
        surfaceHigh = Color(0xFF17203B),
        accent = Color(0xFF00E5FF),
        textPrimary = Color(0xFFF0FDF4),
        textSecondary = Color(0xFF8193B2),
        outline = Color(0xFF233258),
        isGlass = false,
        isLight = false,
    )
    AppTheme.BITCHORD_PURPLE -> NaviifyPalette(
        background = Color(0xFF090614),
        surface = Color(0xFF140D26),
        surfaceHigh = Color(0xFF20163C),
        accent = Color(0xFFB388FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF9E8DB5),
        outline = Color(0xFF35225E),
        isGlass = false,
        isLight = false,
    )
}

fun parseHexColor(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        null
    }
}

@Composable
fun NaviifyTheme(
    theme: AppTheme = AppTheme.SPOTIFY,
    customAccentHex: String = "",
    font: AppFont = AppFont.SYSTEM,
    content: @Composable () -> Unit,
) {
    var palette = paletteFor(theme)
    val customColor = parseHexColor(customAccentHex)
    if (customColor != null) {
        palette = palette.copy(accent = customColor)
    }

    val onPrimaryColor = if (palette.accent.luminance() > 0.6f) Color.Black else Color.White
    val colors = if (palette.isLight) {
        androidx.compose.material3.lightColorScheme(
            primary = palette.accent,
            onPrimary = onPrimaryColor,
            primaryContainer = palette.surfaceHigh,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accent,
            onSecondary = onPrimaryColor,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            error = ErrorRed,
            onError = Color.White,
        )
    } else {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = onPrimaryColor,
            primaryContainer = palette.surfaceHigh,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accent,
            onSecondary = onPrimaryColor,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            error = ErrorRed,
            onError = Color.White,
        )
    }
    val typography = createNaviifyTypography(font)
    CompositionLocalProvider(LocalNaviifyPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            content = content,
        )
    }
}
