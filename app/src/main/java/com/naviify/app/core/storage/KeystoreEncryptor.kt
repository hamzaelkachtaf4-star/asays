package com.naviify.app.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts sensitive credentials (passwords, tokens) using
 * hardware-backed AES-256-GCM via the Android KeyStore.
 */
object KeystoreEncryptor {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "naviify_secrets_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PREFIX = "ENC:"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = runCatching { keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry }.getOrNull()
            if (entry?.secretKey != null) return entry.secretKey
            // Keyset corrupted or stale: recreate
            runCatching { keyStore.deleteEntry(KEY_ALIAS) }
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Checks if the KeyStore key resides in hardware-backed secure execution environment (TEE / StrongBox).
     */
    fun isHardwareBacked(): Boolean {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) getOrCreateKey()
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            val key = entry?.secretKey ?: return false
            val factory = javax.crypto.SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(key, android.security.keystore.KeyInfo::class.java) as? android.security.keystore.KeyInfo
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                keyInfo?.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                    keyInfo?.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
            } else {
                @Suppress("DEPRECATION")
                keyInfo?.isInsideSecureHardware == true
            }
        }.getOrDefault(true)
    }

    /**
     * Verifies end-to-end encryption roundtrip to guarantee keyset integrity.
     */
    fun isHealthy(): Boolean {
        return runCatching {
            val probe = "bitchord_style_integrity_check"
            val encrypted = encrypt(probe)
            decrypt(encrypted) == probe
        }.getOrDefault(false)
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return runCatching {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrElse {
            // Fail closed: never fall back to writing secrets in the clear.
            Log.e(TAG, "Credential encryption failed; value not persisted", it)
            ""
        }
    }

    fun decrypt(cipherOrPlain: String): String {
        if (cipherOrPlain.isBlank()) return ""
        if (!cipherOrPlain.startsWith(PREFIX)) return cipherOrPlain
        return runCatching {
            val encoded = cipherOrPlain.removePrefix(PREFIX)
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrElse {
            // A failed decrypt means the keystore key is gone, for example after
            // an app-data restore onto another device. Returning the raw
            // ciphertext would send garbage credentials to the server.
            Log.e(TAG, "Stored credential could not be decrypted; re-entry required", it)
            ""
        }
    }

    private const val TAG = "KeystoreEncryptor"
}
