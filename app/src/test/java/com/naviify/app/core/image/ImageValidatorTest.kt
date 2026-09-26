package com.naviify.app.core.image

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `valid jpeg with eoi passes validation`() {
        val file = tempFolder.newFile("valid.jpg")
        val jpegBytes = ByteArray(500)
        // SOI (FF D8 FF E0)
        jpegBytes[0] = 0xFF.toByte()
        jpegBytes[1] = 0xD8.toByte()
        jpegBytes[2] = 0xFF.toByte()
        jpegBytes[3] = 0xE0.toByte()
        // EOI at end (FF D9)
        jpegBytes[498] = 0xFF.toByte()
        jpegBytes[499] = 0xD9.toByte()

        file.writeBytes(jpegBytes)

        assertTrue(ImageValidator.isValidImage(file))
        assertTrue(ImageValidator.isValidImageBytes(jpegBytes))
    }

    @Test
    fun `truncated jpeg missing eoi fails validation`() {
        val file = tempFolder.newFile("truncated.jpg")
        // Simulates a 35% cut-off JPEG download
        val truncatedBytes = ByteArray(300)
        truncatedBytes[0] = 0xFF.toByte()
        truncatedBytes[1] = 0xD8.toByte()
        truncatedBytes[2] = 0xFF.toByte()
        truncatedBytes[3] = 0xE0.toByte()
        // Fill the rest with random data, no FF D9
        for (i in 4 until 300) {
            truncatedBytes[i] = (i % 127).toByte()
        }

        file.writeBytes(truncatedBytes)

        assertFalse(ImageValidator.isValidImage(file))
        assertFalse(ImageValidator.isValidImageBytes(truncatedBytes))
    }

    @Test
    fun `valid png with iend chunk passes validation`() {
        val file = tempFolder.newFile("valid.png")
        val pngBytes = ByteArray(200)
        // Magic
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        System.arraycopy(magic, 0, pngBytes, 0, magic.size)
        // IEND chunk at end
        val iend = byteArrayOf(0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte())
        System.arraycopy(iend, 0, pngBytes, pngBytes.size - iend.size, iend.size)

        file.writeBytes(pngBytes)

        assertTrue(ImageValidator.isValidImage(file))
        assertTrue(ImageValidator.isValidImageBytes(pngBytes))
    }

    @Test
    fun `truncated png missing iend fails validation`() {
        val file = tempFolder.newFile("truncated.png")
        val pngBytes = ByteArray(200)
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        System.arraycopy(magic, 0, pngBytes, 0, magic.size)

        file.writeBytes(pngBytes)

        assertFalse(ImageValidator.isValidImage(file))
        assertFalse(ImageValidator.isValidImageBytes(pngBytes))
    }

    @Test
    fun `valid webp passes validation`() {
        val file = tempFolder.newFile("valid.webp")
        val length = 250
        val bytes = ByteArray(length)
        // RIFF
        bytes[0] = 'R'.code.toByte()
        bytes[1] = 'I'.code.toByte()
        bytes[2] = 'F'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        // size = length - 8
        val riffSize = length - 8
        bytes[4] = (riffSize and 0xFF).toByte()
        bytes[5] = ((riffSize shr 8) and 0xFF).toByte()
        bytes[6] = ((riffSize shr 16) and 0xFF).toByte()
        bytes[7] = ((riffSize shr 24) and 0xFF).toByte()
        // WEBP
        bytes[8] = 'W'.code.toByte()
        bytes[9] = 'E'.code.toByte()
        bytes[10] = 'B'.code.toByte()
        bytes[11] = 'P'.code.toByte()

        file.writeBytes(bytes)

        assertTrue(ImageValidator.isValidImage(file))
        assertTrue(ImageValidator.isValidImageBytes(bytes))
    }

    @Test
    fun `truncated webp fails validation`() {
        val file = tempFolder.newFile("truncated.webp")
        val length = 150
        val bytes = ByteArray(length)
        bytes[0] = 'R'.code.toByte()
        bytes[1] = 'I'.code.toByte()
        bytes[2] = 'F'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        // riff size declares 500 bytes, but actual length is only 150
        val riffSize = 500
        bytes[4] = (riffSize and 0xFF).toByte()
        bytes[5] = ((riffSize shr 8) and 0xFF).toByte()
        bytes[6] = ((riffSize shr 16) and 0xFF).toByte()
        bytes[7] = ((riffSize shr 24) and 0xFF).toByte()
        bytes[8] = 'W'.code.toByte()
        bytes[9] = 'E'.code.toByte()
        bytes[10] = 'B'.code.toByte()
        bytes[11] = 'P'.code.toByte()

        file.writeBytes(bytes)

        assertFalse(ImageValidator.isValidImage(file))
        assertFalse(ImageValidator.isValidImageBytes(bytes))
    }

    @Test
    fun `empty or tiny files fail validation`() {
        val empty = tempFolder.newFile("empty.jpg")
        assertFalse(ImageValidator.isValidImage(empty))

        val tiny = tempFolder.newFile("tiny.jpg")
        tiny.writeBytes(ByteArray(50))
        assertFalse(ImageValidator.isValidImage(tiny))

        assertFalse(ImageValidator.isValidImage(null))
        assertFalse(ImageValidator.isValidImage(File("/non/existent/path.jpg")))
    }
}
