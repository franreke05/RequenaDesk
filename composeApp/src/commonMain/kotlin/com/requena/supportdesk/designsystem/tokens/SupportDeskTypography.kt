package com.requena.supportdesk.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SupportDeskTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

private fun Typography.withFamily(
    family: FontFamily,
    headingWeight: FontWeight,
    headingTracking: Float,
): Typography {
    fun TextStyle.heading() = copy(fontFamily = family, fontWeight = headingWeight, letterSpacing = headingTracking.sp)
    return copy(
        displaySmall = displaySmall.heading(),
        headlineLarge = headlineLarge.heading(),
        headlineMedium = headlineMedium.heading(),
        headlineSmall = headlineSmall.heading(),
        titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    )
}

/** Minimalista: sans everywhere, bolder and tighter headings, tabular-friendly sans labels. */
val MinimalTypography = SupportDeskTypography
    .withFamily(FontFamily.SansSerif, FontWeight.Bold, headingTracking = -0.5f)
    .let { it.copy(labelSmall = it.labelSmall.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.4.sp)) }

/** Glass: sans everywhere with lighter, airier headings. */
val GlassTypography = SupportDeskTypography
    .withFamily(FontFamily.SansSerif, FontWeight.Medium, headingTracking = -0.3f)
    .let { it.copy(labelSmall = it.labelSmall.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.3.sp)) }
