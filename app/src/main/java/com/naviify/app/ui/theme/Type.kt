package com.naviify.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.naviify.app.core.theme.AppFont

fun fontFamilyFor(font: AppFont): FontFamily = when (font) {
    AppFont.SYSTEM -> FontFamily.SansSerif
    AppFont.ROUNDED -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    AppFont.CURSIVE -> FontFamily.Cursive
}

fun createNaviifyTypography(font: AppFont = AppFont.SYSTEM): Typography {
    val family = fontFamilyFor(font)
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = if (font == AppFont.ROUNDED) 0.5.sp else 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = if (font == AppFont.ROUNDED) 0.3.sp else 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
    )
}

val Typography = createNaviifyTypography(AppFont.SYSTEM)
