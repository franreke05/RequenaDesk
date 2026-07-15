package com.requena.supportdesk.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSpacing
import com.requena.supportdesk.designsystem.tokens.SupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.SupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.SupportDeskSpacing
import com.requena.supportdesk.designsystem.tokens.SupportDeskTypography

private val SupportDeskColorScheme = lightColorScheme(
    primary = Color(0xFF1F5E45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EEE3),
    onPrimaryContainer = Color(0xFF123727),
    secondary = Color(0xFF315F8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEAF7),
    onSecondaryContainer = Color(0xFF193A5B),
    tertiary = Color(0xFF8A5A21),
    onTertiary = Color.White,
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF17211D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211D),
    surfaceVariant = Color(0xFFEAEFED),
    onSurfaceVariant = Color(0xFF4C5A54),
    outline = Color(0xFF68736E),
    outlineVariant = Color(0xFFD5DDDA),
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val SupportDeskDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3A7),
    onPrimary = Color(0xFF0D2E20),
    primaryContainer = Color(0xFF244C39),
    onPrimaryContainer = Color(0xFFD9F4E6),
    secondary = Color(0xFF9FC4E7),
    onSecondary = Color(0xFF12324E),
    secondaryContainer = Color(0xFF294963),
    onSecondaryContainer = Color(0xFFDCEEFF),
    tertiary = Color(0xFFE1B574),
    onTertiary = Color(0xFF422D0C),
    background = Color(0xFF111513),
    onBackground = Color(0xFFE7ECE9),
    surface = Color(0xFF181D1A),
    onSurface = Color(0xFFE7ECE9),
    surfaceVariant = Color(0xFF252B27),
    onSurfaceVariant = Color(0xFFC1CBC6),
    outline = Color(0xFF8B9690),
    outlineVariant = Color(0xFF39423E),
    error = Color(0xFFF0A59B),
    onError = Color(0xFF42120E),
)

private val SupportDeskShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

object SupportDeskThemeTokens {
    val spacing: SupportDeskSpacing
        @Composable get() = LocalSupportDeskSpacing.current

    val semanticColors: SupportDeskSemanticColors
        @Composable get() = LocalSupportDeskSemanticColors.current

    val elevations: SupportDeskElevations
        @Composable get() = LocalSupportDeskElevations.current

    val typography get() = SupportDeskTypography
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
            success = if (useDarkTheme) Color(0xFF7DD3A7) else Color(0xFF1F6B47),
            successContainer = if (useDarkTheme) Color(0xFF1D3B2B) else Color(0xFFDDF3E7),
            warning = if (useDarkTheme) Color(0xFFF1CC84) else Color(0xFF8A6A25),
            warningContainer = if (useDarkTheme) Color(0xFF413217) else Color(0xFFF6ECD2),
            danger = if (useDarkTheme) Color(0xFFF0A59B) else Color(0xFF9A3E35),
            dangerContainer = if (useDarkTheme) Color(0xFF4A1F1A) else Color(0xFFF7E4E1),
            info = if (useDarkTheme) Color(0xFF9FC4E7) else Color(0xFF315F8C),
            infoContainer = if (useDarkTheme) Color(0xFF24394C) else Color(0xFFDDEAF7),
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
