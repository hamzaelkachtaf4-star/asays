package com.naviify.app.core.image

import android.graphics.BitmapFactory
import java.io.File
import java.io.RandomAccessFile

/**
 * Validates image completeness and container integrity.
 *
 * Prevents corrupted, half-downloaded, or truncated image files (such as JPEGs
 * missing the EOI marker) from entering or remaining in disk caches, which
 * would otherwise cause Android Auto or the system UI to render grey rectangles
 * or torn horizontal glitches.
 */
object ImageValidator {

    private const val MIN_IMAGE_BYTES = 128L
    private const val MAX_TAIL_SCAN_BYTES = 1024

    private val PNG_MAGIC = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
    )

    private val PNG_IEND = byteArrayOf(
        0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(), // IEND
        0xAE.toByte(), 0x42.toByte(), 0x60.toByte(), 0x82.toByte(), // CRC
    )

    /**
     * Checks if the given file is a complete, uncorrupted image (JPEG, PNG, or WebP).
     * Returns false if the file is missing, empty, or truncated.
     */
    fun isValidImage(file: File?): Boolean {
        if (file == null || !file.exists() || !file.isFile) return false
        val length = file.length()
        if (length < MIN_IMAGE_BYTES) return false

        val formatValid = runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(16)
                val readHeader = raf.read(header)
                if (readHeader < 12) return@use false

                when {
                    // JPEG: starts with SOI (FF D8 FF)
                    isJpegHeader(header) -> {
                        validateJpegEoi(raf, length)
                    }

                    // PNG: starts with 8-byte PNG signature
                    isPngHeader(header) -> {
                        validatePngIend(raf, length)
                    }

                    // WebP: starts with RIFF....WEBP
                    isWebpHeader(header) -> {
                        validateWebpLength(header, length)
                    }

                    else -> {
                        // Unknown or unhandled header
                        false
                    }
                }
            }
        }.getOrDefault(false)

        if (!formatValid) return false

        // Supplementary verification via BitmapFactory if running in an Android runtime
        val boundsValid = runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        }.getOrNull()

        // If BitmapFactory ran successfully, ensure dimensions are positive.
        // If it threw (e.g. running under standard JVM JUnit tests), formatValid is sufficient.
        return boundsValid ?: true
    }

    /**
     * Validates an in-memory byte array without disk operations.
     */
    fun isValidImageBytes(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < MIN_IMAGE_BYTES) return false
        val length = bytes.size

        return when {
            isJpegHeader(bytes) -> {
                val scanStart = maxOf(0, length - MAX_TAIL_SCAN_BYTES)
                var foundEoi = false
                for (i in scanStart until length - 1) {
                    if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xD9.toByte()) {
                        foundEoi = true
                        break
                    }
                }
                foundEoi
            }

            isPngHeader(bytes) -> {
                val scanStart = maxOf(0, length - 64)
                containsSubarray(bytes, scanStart, length, PNG_IEND)
            }

            isWebpHeader(bytes) -> {
                val riffSize = (bytes[4].toLong() and 0xFF) or
                    ((bytes[5].toLong() and 0xFF) shl 8) or
                    ((bytes[6].toLong() and 0xFF) shl 16) or
                    ((bytes[7].toLong() and 0xFF) shl 24)
                length >= riffSize + 8
            }

            else -> false
        }
    }

    private fun isJpegHeader(header: ByteArray): Boolean =
        header.size >= 3 &&
            header[0] == 0xFF.toByte() &&
            header[1] == 0xD8.toByte() &&
            header[2] == 0xFF.toByte()

    private fun validateJpegEoi(raf: RandomAccessFile, fileLength: Long): Boolean {
        val checkLen = minOf(fileLength, MAX_TAIL_SCAN_BYTES.toLong()).toInt()
        val tail = ByteArray(checkLen)
        raf.seek(fileLength - checkLen)
        raf.readFully(tail)

        // Trailing scan for EOI marker (0xFF, 0xD9)
        for (i in 0 until checkLen - 1) {
            if (tail[i] == 0xFF.toByte() && tail[i + 1] == 0xD9.toByte()) {
                return true
            }
        }
        return false
    }

    private fun isPngHeader(header: ByteArray): Boolean {
        if (header.size < PNG_MAGIC.size) return false
        for (i in PNG_MAGIC.indices) {
            if (header[i] != PNG_MAGIC[i]) return false
        }
        return true
    }

    private fun validatePngIend(raf: RandomAccessFile, fileLength: Long): Boolean {
        val checkLen = minOf(fileLength, 64L).toInt()
        val tail = ByteArray(checkLen)
        raf.seek(fileLength - checkLen)
        raf.readFully(tail)
        return containsSubarray(tail, 0, checkLen, PNG_IEND)
    }

    private fun isWebpHeader(header: ByteArray): Boolean =
        header.size >= 12 &&
            header[0] == 'R'.code.toByte() &&
            header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() &&
            header[3] == 'F'.code.toByte() &&
            header[8] == 'W'.code.toByte() &&
            header[9] == 'E'.code.toByte() &&
            header[10] == 'B'.code.toByte() &&
            header[11] == 'P'.code.toByte()

    private fun validateWebpLength(header: ByteArray, fileLength: Long): Boolean {
        val riffSize = (header[4].toLong() and 0xFF) or
            ((header[5].toLong() and 0xFF) shl 8) or
            ((header[6].toLong() and 0xFF) shl 16) or
            ((header[7].toLong() and 0xFF) shl 24)
        return fileLength >= riffSize + 8
    }

    private fun containsSubarray(src: ByteArray, start: Int, end: Int, sub: ByteArray): Boolean {
        if (end - start < sub.size) return false
        for (i in start..end - sub.size) {
            var match = true
            for (j in sub.indices) {
                if (src[i + j] != sub[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }
}
