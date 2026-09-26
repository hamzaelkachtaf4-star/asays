package com.naviify.app.core.theme

enum class AppFont(
    val label: String,
    val subtitle: String,
) {
    SYSTEM("Modern Sans", "Clean & balanced modern sans-serif"),
    ROUNDED("Rounded Geometric", "Modern friendly rounded geometry"),
    SERIF("Editorial Serif", "Classic vinyl & album editorial serif"),
    MONOSPACE("Retro Monospace", "Tech terminal & synthwave monospace"),
    CURSIVE("Artistic Cursive", "Expressive handwritten script aesthetic"),
    ;

    companion object {
        fun fromStorage(value: String?): AppFont =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
