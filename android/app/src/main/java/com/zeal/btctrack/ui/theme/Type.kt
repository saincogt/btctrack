package com.zeal.btctrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MonoBodyStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    lineHeight = 22.sp,
)

val MonoDisplayStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 38.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 40.sp,
)

val SectionLabelStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 17.sp,
    letterSpacing = 1.5.sp,
)

// Maps DESIGN.md type scale to Material3 typography roles.
// FontFamily.Serif approximates Copernicus/Tiempos for display sizes.
// FontFamily.SansSerif approximates StyreneB/Inter for body/UI.
val AppTypography = Typography(
    // display-md: 36px serif, weight 400, lh 1.15, ls -0.5px
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 36.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp,
    ),
    // display-sm: 28px serif, weight 400, lh 1.2, ls -0.3px
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    // title-lg: 22px sans, weight 500, lh 1.3
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 29.sp,
    ),
    // title-md: 18px sans, weight 500, lh 1.4
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 25.sp,
    ),
    // title-sm: 16px sans, weight 500, lh 1.4
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
    ),
    // body-md: 16px sans, weight 400, lh 1.55
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
    ),
    // body-sm: 14px sans, weight 400, lh 1.55
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    ),
    // caption: 13px sans, weight 500, lh 1.4
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
    ),
    // button: 14px sans, weight 500, lh 1
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
    ),
    // caption: 13px sans, weight 500, lh 1.4
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
    ),
    // caption-uppercase: 12px sans, weight 500, lh 1.4, ls 1.5px
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp,
        letterSpacing = 1.5.sp,
    ),
)
