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

// Warm "editorial paper" palette — matches the OryKai portfolio's design language:
// warm paper surfaces, true-ink text, one confident vermillion accent, hairline
// borders instead of glassy elevation. See Porfolio/src/app/globals.css for the
// source tokens this scheme mirrors (--background, --primary, --border, etc).
private val SupportDeskColorScheme = lightColorScheme(
    primary = Color(0xFFC1391F),
    onPrimary = Color(0xFFF8F4EA),
    primaryContainer = Color(0xFFF3D9CE),
    onPrimaryContainer = Color(0xFF6B1D0F),
    secondary = Color(0xFF8A6E3E),
    onSecondary = Color(0xFFF8F4EA),
    secondaryContainer = Color(0xFFEAE0C8),
    onSecondaryContainer = Color(0xFF4A3A1D),
    tertiary = Color(0xFF6B6152),
    onTertiary = Color(0xFFF8F4EA),
    tertiaryContainer = Color(0xFFE4DCC6),
    onTertiaryContainer = Color(0xFF2E2A22),
    background = Color(0xFFF3EFE4),
    onBackground = Color(0xFF1B1712),
    surface = Color(0xFFECE5D4),
    onSurface = Color(0xFF1B1712),
    surfaceVariant = Color(0xFFE4DCC6),
    onSurfaceVariant = Color(0xFF6B6152),
    outline = Color(0xFFBFB193),
    outlineVariant = Color(0xFFD8CFBA),
    error = Color(0xFFA5291A),
    onError = Color(0xFFF8F4EA),
)

// The portfolio has no full dark mode by design (only one inverted "ink" section),
// so this dark scheme extrapolates from its --ink-bg/--ink-fg tokens while keeping
// the same warm, monochrome-plus-one-accent identity instead of switching to cool grays.
private val SupportDeskDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2603A),
    onPrimary = Color(0xFF2A0E06),
    primaryContainer = Color(0xFF4A2013),
    onPrimaryContainer = Color(0xFFF6C9B8),
    secondary = Color(0xFFC9A968),
    onSecondary = Color(0xFF2E2408),
    secondaryContainer = Color(0xFF4A3A18),
    onSecondaryContainer = Color(0xFFF0E2BE),
    tertiary = Color(0xFFA99A82),
    onTertiary = Color(0xFF241E14),
    tertiaryContainer = Color(0xFF3A332A),
    onTertiaryContainer = Color(0xFFE4DCC6),
    background = Color(0xFF17130F),
    onBackground = Color(0xFFF3EFE4),
    surface = Color(0xFF201A14),
    onSurface = Color(0xFFF3EFE4),
    surfaceVariant = Color(0xFF2A2118),
    onSurfaceVariant = Color(0xFFC7BBA3),
    outline = Color(0xFF4A3F30),
    outlineVariant = Color(0xFF3A332A),
    error = Color(0xFFE0584A),
    onError = Color(0xFF2E0A06),
)

// Modest radii read as printed panels rather than app chrome — the portfolio's
// card-surface utility uses 0.5rem (8dp) with a hairline border, never a large pill radius.
private val SupportDeskShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
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
            success = if (useDarkTheme) Color(0xFF7FB88C) else Color(0xFF3F6B4A),
            successContainer = if (useDarkTheme) Color(0xFF243B29) else Color(0xFFDCE8DD),
            warning = if (useDarkTheme) Color(0xFFE0B563) else Color(0xFF8A6115),
            warningContainer = if (useDarkTheme) Color(0xFF4A3814) else Color(0xFFFCF2D8),
            danger = if (useDarkTheme) Color(0xFFE0584A) else Color(0xFFA5291A),
            dangerContainer = if (useDarkTheme) Color(0xFF4A2016) else Color(0xFFF5DAD3),
            // Kept warm/ink-toned rather than blue, in keeping with the portfolio's
            // "monochrome + one accent" palette (no second bright/cold hue).
            info = if (useDarkTheme) Color(0xFFC9A968) else Color(0xFF6B5636),
            infoContainer = if (useDarkTheme) Color(0xFF3A2E14) else Color(0xFFEAE0C8),
            // Reuse existing onPrimary/onError values rather than inventing new colors -
            // success/danger at full strength need the same cream-on-dark / ink-on-light
            // contrast onPrimary and onError already solve.
            onSuccess = if (useDarkTheme) Color(0xFF2A0E06) else Color(0xFFF8F4EA),
            onDanger = if (useDarkTheme) Color(0xFF2E0A06) else Color(0xFFF8F4EA),
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
