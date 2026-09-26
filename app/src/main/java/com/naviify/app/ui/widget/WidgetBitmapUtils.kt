package com.naviify.app.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.naviify.app.R

object WidgetBitmapUtils {

    /** Fond neutre quand le morceau n'a pas de pochette. */
    const val DEFAULT_WIDGET_COLOR = 0xFF181818.toInt()

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
     * Couleur dominante de [src] (moyenne d'une reduction 8x8), retravaillée pour
     * servir de fond de widget : la moyenne brute d'une pochette est presque
     * toujours un gris terne. null si la pochette est absente/transparente.
     */
    fun dominantColor(src: Bitmap?): Int? {
        if (src == null || src.width == 0 || src.height == 0) return null
        return try {
            val small = Bitmap.createScaledBitmap(src, 8, 8, true)
            var red = 0L
            var green = 0L
            var blue = 0L
            var count = 0
            for (x in 0 until small.width) {
                for (y in 0 until small.height) {
                    val pixel = small.getPixel(x, y)
                    if (Color.alpha(pixel) < 32) continue
                    red += Color.red(pixel)
                    green += Color.green(pixel)
                    blue += Color.blue(pixel)
                    count++
                }
            }
            if (count == 0) return null
            val hsv = FloatArray(3)
            Color.RGBToHSV((red / count).toInt(), (green / count).toInt(), (blue / count).toInt(), hsv)
            // Assez colore pour se voir, assez sombre pour garder le texte lisible.
            hsv[1] = (hsv[1] * 1.35f).coerceIn(0.25f, 0.75f)
            hsv[2] = (hsv[2] * 1.15f).coerceIn(0.22f, 0.55f)
            Color.HSVToColor(hsv)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Fond du widget facon Spotify : degrade vertical de [baseColor] vers une
     * version assombrie, coins arrondis de [cornerRadiusPx]. Genere a une taille
     * de reference puis etire par le widget (fitXY) - un fond ne se voit pas
     * etre agrandi, et ca evite de mesurer chaque widget.
     */
    fun createGradientBackground(
        baseColor: Int,
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float,
    ): Bitmap? {
        val key = "bg-$baseColor-$widthPx-$heightPx-${cornerRadiusPx.toInt()}"
        bitmapCache[key]?.let { return it }

        return try {
            val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    heightPx.toFloat(),
                    baseColor,
                    darken(baseColor, 0.55f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(
                RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
                cornerRadiusPx,
                cornerRadiusPx,
                paint,
            )
            bitmapCache.put(key, output)
            output
        } catch (e: Throwable) {
            null
        }
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )

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
