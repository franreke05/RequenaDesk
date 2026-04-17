package com.requena.supportdesk.app.mobile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.requena.supportdesk.app.LocalSupportDeskThemeController

private val MobileLightColorScheme = lightColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF03224C),
    secondary = Color(0xFF30B0C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F3F7),
    onSecondaryContainer = Color(0xFF06343C),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color(0xFF3F2400),
    tertiaryContainer = Color(0xFFFFE7BE),
    onTertiaryContainer = Color(0xFF4D2A00),
    background = Color(0xFFF3F5FB),
    onBackground = Color(0xFF101623),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101623),
    surfaceVariant = Color(0xFFE9EEF7),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD0D7E2),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFFF453A),
    onError = Color.White,
)

private val MobileDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF042545),
    primaryContainer = Color(0xFF103A66),
    onPrimaryContainer = Color(0xFFD7EAFF),
    secondary = Color(0xFF70D6E5),
    onSecondary = Color(0xFF00363E),
    secondaryContainer = Color(0xFF114B56),
    onSecondaryContainer = Color(0xFFCCF3F9),
    tertiary = Color(0xFFFFB340),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3A00),
    onTertiaryContainer = Color(0xFFFFE1B2),
    background = Color(0xFF0D111A),
    onBackground = Color(0xFFF4F7FC),
    surface = Color(0xFF171C26),
    onSurface = Color(0xFFF4F7FC),
    surfaceVariant = Color(0xFF232B38),
    onSurfaceVariant = Color(0xFFAAB6C8),
    outline = Color(0xFF435064),
    outlineVariant = Color(0xFF283241),
    error = Color(0xFFFF7B74),
    onError = Color(0xFF470705),
)

private val MobileTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
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
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp,
    ),
)

private val MobileShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun SupportDeskMobileTheme(
    content: @Composable () -> Unit,
) {
    val controller = LocalSupportDeskThemeController.current
    val colorScheme = if (controller.isDarkMode) MobileDarkColorScheme else MobileLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MobileTypography,
        shapes = MobileShapes,
    ) {
        Surface(
            color = colorScheme.background,
            content = content,
        )
    }
}
