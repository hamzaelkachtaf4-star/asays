package com.naviify.app.core.theme

enum class AppTheme(
    val label: String,
    val subtitle: String,
    val defaultAccentHex: String,
) {
    SPOTIFY("ASAYS Signature", "Deep obsidian with official ASAYS wave green", "#69B987"),
    GLASS("Frosted Glass", "Translucent glassmorphism with ice cyan", "#38BDF8"),
    OLED("OLED Pure Black", "True #000000 black with ASAYS green", "#69B987"),
    SUNSET("Sunset Synthwave", "Twilight violet with warm neon coral", "#FF5E7E"),
    CYBERPUNK("Cyberpunk Neon", "Midnight night with vibrant magenta", "#E040FB"),
    OCEANIC("Deep Oceanic", "Abyssal marine blue with electric cyan", "#00D2FF"),
    EMERALD("Emerald Forest", "Deep jade green with vivid emerald", "#10B981"),
    AMETHYST("Royal Amethyst", "Imperial plum with radiant orchid", "#C084FC"),
    MIDNIGHT("Midnight Blue", "Deep navy with modern azure blue", "#3B82F6"),
    TITANIUM("Luxury Titanium", "Precision matte titanium with slate platinum", "#E2E8F0"),
    APPLE_DARK("Apple Music Dark", "Ultra-deep true black with Apple Crimson Red", "#FA2D48"),
    APPLE_LIGHT("Apple Music Light", "Clean Cupertino iOS light mode with Apple Crimson Red", "#FA2D48"),
    BITCHORD_CYAN("Electric Cyan", "High-contrast midnight cyber with electric cyan", "#00E5FF"),
    BITCHORD_PURPLE("Neon Violet", "Deep space midnight with radiant violet", "#B388FF"),
    ;

    companion object {
        fun fromStorage(value: String?): AppTheme =
            entries.firstOrNull { it.name == value } ?: SPOTIFY
    }
}
