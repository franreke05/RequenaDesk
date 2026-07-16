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
    primary = Color(0xFF087B67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F2E5),
    onPrimaryContainer = Color(0xFF003D32),
    secondary = Color(0xFF4A5FC5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E7FF),
    onSecondaryContainer = Color(0xFF1E2C76),
    tertiary = Color(0xFF9A5E19),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7C5),
    onTertiaryContainer = Color(0xFF512C00),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF14212C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14212C),
    surfaceVariant = Color(0xFFE8EEF6),
    onSurfaceVariant = Color(0xFF4C5B6A),
    outline = Color(0xFF71808E),
    outlineVariant = Color(0xFFC9D4DF),
    error = Color(0xFFB52832),
    onError = Color.White,
)

private val SupportDeskDarkColorScheme = darkColorScheme(
    primary = Color(0xFF55D9B6),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF075143),
    onPrimaryContainer = Color(0xFFBFF5E3),
    secondary = Color(0xFFBAC6FF),
    onSecondary = Color(0xFF233574),
    secondaryContainer = Color(0xFF313E7D),
    onSecondaryContainer = Color(0xFFE1E5FF),
    tertiary = Color(0xFFFFC776),
    onTertiary = Color(0xFF4D2F00),
    tertiaryContainer = Color(0xFF6B4300),
    onTertiaryContainer = Color(0xFFFFE3B8),
    background = Color(0xFF09131A),
    onBackground = Color(0xFFE5EDF4),
    surface = Color(0xFF101D25),
    onSurface = Color(0xFFE5EDF4),
    surfaceVariant = Color(0xFF1D2B35),
    onSurfaceVariant = Color(0xFFBDCAD4),
    outline = Color(0xFF8999A7),
    outlineVariant = Color(0xFF34444F),
    error = Color(0xFFFFB2B8),
    onError = Color(0xFF650018),
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
            success = if (useDarkTheme) Color(0xFF55D9B6) else Color(0xFF087B67),
            successContainer = if (useDarkTheme) Color(0xFF0B4037) else Color(0xFFC8F2E5),
            warning = if (useDarkTheme) Color(0xFFFFC776) else Color(0xFF9A5E19),
            warningContainer = if (useDarkTheme) Color(0xFF4D340A) else Color(0xFFFFE7C5),
            danger = if (useDarkTheme) Color(0xFFFFB2B8) else Color(0xFFB52832),
            dangerContainer = if (useDarkTheme) Color(0xFF5C2029) else Color(0xFFFFE5E7),
            info = if (useDarkTheme) Color(0xFFBAC6FF) else Color(0xFF4A5FC5),
            infoContainer = if (useDarkTheme) Color(0xFF27346F) else Color(0xFFE2E7FF),
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
