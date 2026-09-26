package com.naviify.app.core.network

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Subsonic auth primitives. Per spec, `t = md5(secret + s)` where `s` is a
 * random salt sent alongside, and `secret` is either the user password or a
 * Navidrome Subsonic API token.
 */
internal object SubsonicAuth {

    const val API_VERSION = "1.16.1"
    const val CLIENT_NAME = "ASAYS"

    private val random = SecureRandom()
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun salt(length: Int = 12): String = buildString(length) {
        repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    fun token(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("MD5").digest((secret + salt).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
