package com.naviify.app.core.network

/**
 * Normalizes a user-provided server URL. Adds a default scheme, trims a
 * trailing slash, and drops a trailing `/rest` segment so the client never
 * produces `/rest/rest/...` requests.
 */
fun normalizeServerUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val schemeMatch = Regex("""^([a-zA-Z][a-zA-Z0-9+.-]*)://""").find(trimmed)
    if (schemeMatch != null) {
        val scheme = schemeMatch.groupValues[1].lowercase()
        if (scheme != "http" && scheme != "https") return ""
    }
    val withScheme = if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "http://$trimmed"
    }
    return withScheme.trimEnd('/').removeSuffix("/rest")
}
