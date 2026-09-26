package com.naviify.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Spotify-style marquee text that scrolls continuously in a loop when the text
 * is too long to fit in the available width. When the text fits, it renders
 * as normal static text.
 *
 * The animation scrolls: [text]  ·  [text]  ·  … seamlessly, with a
 * separator between repetitions.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    separator: String = "     ·     ",
    /** Pixels per second for the scroll speed. */
    scrollSpeedPxPerSec: Float = 40f,
    /** Delay in ms before the scroll starts (and between loops). */
    initialDelayMs: Int = 1500,
) {
    val resolvedStyle = style.let { s ->
        s.copy(
            fontWeight = fontWeight ?: s.fontWeight,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else s.fontSize,
            color = if (color != Color.Unspecified) color else s.color,
            textAlign = textAlign ?: s.textAlign,
        )
    }

    val textMeasurer = rememberTextMeasurer()

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val textWidthPx = remember(text, resolvedStyle) {
        textMeasurer.measure(text, resolvedStyle).size.width.toFloat()
    }
    val separatorWidthPx = remember(separator, resolvedStyle) {
        textMeasurer.measure(separator, resolvedStyle).size.width.toFloat()
    }

    val shouldScroll = containerWidthPx > 0f && textWidthPx > containerWidthPx

    if (!shouldScroll) {
        // Text fits — render static
        Text(
            text = text,
            style = resolvedStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.onSizeChanged { containerWidthPx = it.width.toFloat() },
        )
    } else {
        // One "segment" = text + separator
        val segmentWidthPx = textWidthPx + separatorWidthPx
        // Duration to scroll one full segment
        val durationMs = ((segmentWidthPx / scrollSpeedPxPerSec) * 1000).toInt().coerceAtLeast(2000)

        val infiniteTransition = rememberInfiniteTransition(label = "marquee")
        val offsetFraction by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durationMs,
                    delayMillis = initialDelayMs,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "marquee_offset",
        )

        val offsetPx = offsetFraction * segmentWidthPx

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { containerWidthPx = it.width.toFloat() },
        ) {
            // Render [text + separator + text] shifted left; as the first copy scrolls
            // off screen, the second copy slides in seamlessly.
            Text(
                text = text + separator + text,
                style = resolvedStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.graphicsLayer { translationX = -offsetPx },
            )
        }
    }
}
