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
    primary = Color(0xFFB38349),
    onPrimary = Color(0xFF15110C),
    primaryContainer = Color(0xFFD7E0D4),
    onPrimaryContainer = Color(0xFF142219),
    secondary = Color(0xFFD8B47A),
    onSecondary = Color(0xFF15110C),
    secondaryContainer = Color(0xFFE7D9C7),
    onSecondaryContainer = Color(0xFF342619),
    tertiary = Color(0xFF6A4A3C),
    onTertiary = Color(0xFFFFF8F5),
    background = Color(0xFFF7F1E8),
    onBackground = Color(0xFF191612),
    surface = Color(0xFFFFF9F0),
    onSurface = Color(0xFF211B15),
    surfaceVariant = Color(0xFFE5DCCF),
    onSurfaceVariant = Color(0xFF5E544B),
    outline = Color(0xFFD8C5AB),
    outlineVariant = Color(0xFFD6CABA),
    error = Color(0xFFA43A32),
    onError = Color.White,
)

private val SupportDeskDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC39356),
    onPrimary = Color(0xFF0E0B08),
    primaryContainer = Color(0xFF274B35),
    onPrimaryContainer = Color(0xFFE0F2E5),
    secondary = Color(0xFF8F6A3D),
    onSecondary = Color(0xFF0E0B08),
    secondaryContainer = Color(0xFF243A42),
    onSecondaryContainer = Color(0xFFDDEBF0),
    tertiary = Color(0xFFD3B5D8),
    onTertiary = Color(0xFF291B2E),
    background = Color(0xFF08080F),
    onBackground = Color(0xFFF3E8D7),
    surface = Color(0xFF121820),
    onSurface = Color(0xFFE8D7BD),
    surfaceVariant = Color(0xFF242A31),
    onSurfaceVariant = Color(0xFFB8C2CC),
    outline = Color(0xFF2E261C),
    outlineVariant = Color(0xFF3A424A),
    error = Color(0xFFCF665C),
    onError = Color(0xFF42120E),
)

private val SupportDeskTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.sp,
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
        letterSpacing = 0.sp,
    ),
)

private val SupportDeskShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
            success = if (useDarkTheme) Color(0xFF9FD7B0) else Color(0xFF2E6A44),
            successContainer = if (useDarkTheme) Color(0xFF1C3326) else Color(0xFFE2EFE6),
            warning = if (useDarkTheme) Color(0xFFF1CC84) else Color(0xFF8A6A25),
            warningContainer = if (useDarkTheme) Color(0xFF3A321F) else Color(0xFFF6ECD2),
            danger = if (useDarkTheme) Color(0xFFF0A59B) else Color(0xFF9A3E35),
            dangerContainer = if (useDarkTheme) Color(0xFF44211F) else Color(0xFFF7E4E1),
            info = if (useDarkTheme) Color(0xFF9FC2C5) else Color(0xFF3D6468),
            infoContainer = if (useDarkTheme) Color(0xFF20343A) else Color(0xFFE1ECED),
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
