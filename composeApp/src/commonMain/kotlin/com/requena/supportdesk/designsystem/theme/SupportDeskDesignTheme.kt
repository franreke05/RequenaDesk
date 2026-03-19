package com.requena.supportdesk.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSpacing
import com.requena.supportdesk.designsystem.tokens.SupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.SupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.SupportDeskSpacing

private val SupportDeskColorScheme = lightColorScheme(
    primary = Color(0xFF1D4D6F),
    onPrimary = Color(0xFFF9FBFC),
    primaryContainer = Color(0xFFD9E8F2),
    onPrimaryContainer = Color(0xFF112736),
    secondary = Color(0xFF435B6B),
    onSecondary = Color(0xFFF7FAFC),
    secondaryContainer = Color(0xFFE2EAF0),
    onSecondaryContainer = Color(0xFF1A2A36),
    tertiary = Color(0xFF6B7280),
    onTertiary = Color.White,
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF5E6B78),
    outline = Color(0xFFCCD6E0),
    outlineVariant = Color(0xFFDCE3EA),
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val SupportDeskDarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5E5),
    onPrimary = Color(0xFF082133),
    primaryContainer = Color(0xFF12344A),
    onPrimaryContainer = Color(0xFFD7ECF8),
    secondary = Color(0xFFB3C3CF),
    onSecondary = Color(0xFF17242E),
    secondaryContainer = Color(0xFF293945),
    onSecondaryContainer = Color(0xFFDDE8F0),
    tertiary = Color(0xFFC2C8D1),
    onTertiary = Color(0xFF1A1E24),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF243041),
    error = Color(0xFFF87171),
    onError = Color(0xFF2E0A0A),
)

private val SupportDeskTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
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

private val SupportDeskShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object SupportDeskThemeTokens {
    val spacing: SupportDeskSpacing
        @Composable get() = LocalSupportDeskSpacing.current

    val semanticColors: SupportDeskSemanticColors
        @Composable get() = LocalSupportDeskSemanticColors.current

    val elevations: SupportDeskElevations
        @Composable get() = LocalSupportDeskElevations.current
}

@Composable
fun SupportDeskDesignTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) SupportDeskDarkColorScheme else SupportDeskColorScheme
    CompositionLocalProvider(
        LocalSupportDeskSpacing provides SupportDeskSpacing(),
        LocalSupportDeskSemanticColors provides SupportDeskSemanticColors(
            success = if (useDarkTheme) Color(0xFF72D19A) else Color(0xFF1E6B3C),
            successContainer = if (useDarkTheme) Color(0xFF153624) else Color(0xFFE8F5EC),
            warning = if (useDarkTheme) Color(0xFFF6C86B) else Color(0xFF8A6115),
            warningContainer = if (useDarkTheme) Color(0xFF3B2D12) else Color(0xFFFCF2D8),
            danger = if (useDarkTheme) Color(0xFFF59B9B) else Color(0xFF9B2C2C),
            dangerContainer = if (useDarkTheme) Color(0xFF421B1B) else Color(0xFFFCE8E8),
            info = if (useDarkTheme) Color(0xFF8CB6FF) else Color(0xFF1D4ED8),
            infoContainer = if (useDarkTheme) Color(0xFF172C52) else Color(0xFFE8F0FF),
        ),
        LocalSupportDeskElevations provides SupportDeskElevations(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SupportDeskTypography,
            shapes = SupportDeskShapes,
        ) {
            Surface(
                color = colorScheme.background,
                content = content,
            )
        }
    }
}
