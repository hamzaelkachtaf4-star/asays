package com.naviify.app.domain.playback

/**
 * Streaming quality presets. Lossless omits the Subsonic `maxBitRate`
 * parameter so Navidrome serves the original file bit-for-bit (FLAC preserved).
 */
enum class StreamQuality(
    val maxBitRate: Int?,
    val label: String,
) {
    LOSSLESS(null, "Lossless"),
    HIGH(320, "High 320"),
    BALANCED(192, "Balanced 192"),
    DATA_SAVER(128, "Data Saver 128"),
    ;

    companion object {
        fun fromStorage(value: String?): StreamQuality =
            entries.firstOrNull { it.name == value } ?: LOSSLESS
    }
}

/** Picks the quality for the active network type. */
fun resolveQuality(
    wifiQuality: StreamQuality,
    mobileQuality: StreamQuality,
    isWifi: Boolean,
): StreamQuality = if (isWifi) wifiQuality else mobileQuality
