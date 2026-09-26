package com.naviify.app.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.naviify.app.R

object WidgetBitmapUtils {

    private val bitmapCache = LruCache<String, Bitmap>(24)

    fun getCachedBitmap(key: String): Bitmap? = bitmapCache[key]

    fun putCachedBitmap(key: String, bitmap: Bitmap) {
        bitmapCache.put(key, bitmap)
    }

    /**
     * Center-crops [src] to a square, resizes to [targetSizePx], and applies
     * rounded corners of [cornerRadiusPx] using a BitmapShader.
     */
    fun createSquareRoundedBitmap(src: Bitmap, targetSizePx: Int, cornerRadiusPx: Float): Bitmap? {
        return try {
            val size = minOf(src.width, src.height)
            val x = (src.width - size) / 2
            val y = (src.height - size) / 2
            val cropped = Bitmap.createBitmap(src, x, y, size, size)
            val scaled = if (size != targetSizePx) {
                Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true).also {
                    if (cropped != src && cropped != it) {
                        cropped.recycle()
                    }
                }
            } else {
                cropped
            }

            val output = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, targetSizePx.toFloat(), targetSizePx.toFloat())
            val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.shader = shader
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
            output
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Fallback artwork showing the ASAYS logo neatly centered on a rounded dark card.
     */
    fun getDefaultArtwork(context: Context, targetSizePx: Int, cornerRadiusPx: Float): Bitmap? {
        val cacheKey = "default-$targetSizePx-$cornerRadiusPx"
        bitmapCache[cacheKey]?.let { return it }

        return try {
            val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background dark card
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#282828")
            }
            val rect = RectF(0f, 0f, targetSizePx.toFloat(), targetSizePx.toFloat())
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

            // Logo
            val drawable = ContextCompat.getDrawable(context, R.drawable.app_logo)
            if (drawable != null) {
                val padding = (targetSizePx * 0.18f).toInt()
                drawable.setBounds(padding, padding, targetSizePx - padding, targetSizePx - padding)
                drawable.draw(canvas)
            }

            bitmapCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Throwable) {
            null
        }
    }
}
