package com.requena.supportdesk.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.designsystem.tokens.EmphasisTreatment
import com.requena.supportdesk.designsystem.tokens.GlassTypography
import com.requena.supportdesk.designsystem.tokens.MinimalTypography
import com.requena.supportdesk.designsystem.tokens.SupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.SupportDeskSurfaceStyle
import com.requena.supportdesk.designsystem.tokens.SupportDeskTypography

/** Everything that changes between visual styles, resolved for one style + light/dark. */
@Immutable
data class SupportDeskStyleTokens(
    val colorScheme: ColorScheme,
    val semanticColors: SupportDeskSemanticColors,
    val shapes: Shapes,
    val typography: Typography,
    val surfaceStyle: SupportDeskSurfaceStyle,
)

fun supportDeskStyleTokens(style: SupportDeskStyle, dark: Boolean): SupportDeskStyleTokens = when (style) {
    SupportDeskStyle.ARTESANAL -> SupportDeskStyleTokens(
        colorScheme = if (dark) ArtesanalDark else ArtesanalLight,
        semanticColors = artesanalSemantics(dark),
        shapes = ArtesanalShapes,
        typography = SupportDeskTypography,
        surfaceStyle = SupportDeskSurfaceStyle(emphasis = EmphasisTreatment.HARD_OFFSET),
    )
    SupportDeskStyle.MINIMAL -> SupportDeskStyleTokens(
        colorScheme = if (dark) MinimalDark else MinimalLight,
        semanticColors = minimalSemantics(dark),
        shapes = MinimalShapes,
        typography = MinimalTypography,
        surfaceStyle = SupportDeskSurfaceStyle(emphasis = EmphasisTreatment.OUTLINE),
    )
    SupportDeskStyle.GLASS -> SupportDeskStyleTokens(
        colorScheme = if (dark) GlassDark else GlassLight,
        semanticColors = artesanalSemantics(dark),
        shapes = GlassShapes,
        typography = GlassTypography,
        surfaceStyle = SupportDeskSurfaceStyle(
            emphasis = EmphasisTreatment.GLOW,
            backdropColors = if (dark) GlassDarkBackdrop else GlassLightBackdrop,
            backdropGlows = if (dark) GlassDarkGlows else GlassLightGlows,
            translucentSurfaces = true,
        ),
    )
}

// ---------------------------------------------------------------------------------------
// Artesanal — warm "editorial paper" palette (the original theme, unchanged). Matches the
// OryKai portfolio: warm paper surfaces, true-ink text, one confident vermillion accent,
// hairline borders instead of glassy elevation. Dark extrapolates from its ink tokens.
// ---------------------------------------------------------------------------------------
internal val ArtesanalLight = lightColorScheme(
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

internal val ArtesanalDark = darkColorScheme(
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

// Modest radii read as printed panels rather than app chrome.
internal val ArtesanalShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(5.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

private fun artesanalSemantics(dark: Boolean) = SupportDeskSemanticColors(
    success = if (dark) Color(0xFF7FB88C) else Color(0xFF3F6B4A),
    successContainer = if (dark) Color(0xFF243B29) else Color(0xFFDCE8DD),
    warning = if (dark) Color(0xFFE0B563) else Color(0xFF8A6115),
    warningContainer = if (dark) Color(0xFF4A3814) else Color(0xFFFCF2D8),
    danger = if (dark) Color(0xFFE0584A) else Color(0xFFA5291A),
    dangerContainer = if (dark) Color(0xFF4A2016) else Color(0xFFF5DAD3),
    // Warm/ink-toned rather than blue: "monochrome + one accent".
    info = if (dark) Color(0xFFC9A968) else Color(0xFF6B5636),
    infoContainer = if (dark) Color(0xFF3A2E14) else Color(0xFFEAE0C8),
    onSuccess = if (dark) Color(0xFF2A0E06) else Color(0xFFF8F4EA),
    onDanger = if (dark) Color(0xFF2E0A06) else Color(0xFFF8F4EA),
)

// ---------------------------------------------------------------------------------------
// Minimalista — neutral surfaces, ink text, vermillion as the only color. No shadows.
// Container roles are set explicitly so Material components never fall back to the
// baseline purple-tinted defaults.
// ---------------------------------------------------------------------------------------
internal val MinimalLight = lightColorScheme(
    primary = Color(0xFFC1391F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBE3DC),
    onPrimaryContainer = Color(0xFF6B1D0F),
    inversePrimary = Color(0xFFFF8A65),
    secondary = Color(0xFF3F3F3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDEB),
    onSecondaryContainer = Color(0xFF1F1F1D),
    tertiary = Color(0xFF5F5F5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F0EE),
    onTertiaryContainer = Color(0xFF2A2A28),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F2F0),
    onSurfaceVariant = Color(0xFF55554F),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF4F4F2),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE7E4),
    onErrorContainer = Color(0xFF6A1209),
    outline = Color(0xFFBDBDB8),
    outlineVariant = Color(0xFFE4E4E1),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE9E9E7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFBFA),
    surfaceContainer = Color(0xFFF6F6F5),
    surfaceContainerHigh = Color(0xFFF2F2F0),
    surfaceContainerHighest = Color(0xFFEDEDEB),
)

internal val MinimalDark = darkColorScheme(
    primary = Color(0xFFE2603A),
    onPrimary = Color(0xFF1A0A05),
    primaryContainer = Color(0xFF4A2013),
    onPrimaryContainer = Color(0xFFF6C9B8),
    inversePrimary = Color(0xFFC1391F),
    secondary = Color(0xFFC8C8C4),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2A2A28),
    onSecondaryContainer = Color(0xFFEDEDEB),
    tertiary = Color(0xFFA5A5A0),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF262624),
    onTertiaryContainer = Color(0xFFE0E0DD),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFF4F4F2),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF4F4F2),
    surfaceVariant = Color(0xFF242422),
    onSurfaceVariant = Color(0xFFB4B4AE),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF4F4F2),
    inverseOnSurface = Color(0xFF1A1A1A),
    error = Color(0xFFF07060),
    onError = Color(0xFF2E0A06),
    errorContainer = Color(0xFF4A1A12),
    onErrorContainer = Color(0xFFFFD9D2),
    outline = Color(0xFF4A4A46),
    outlineVariant = Color(0xFF2E2E2C),
    surfaceBright = Color(0xFF2E2E2E),
    surfaceDim = Color(0xFF0F0F0F),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

internal val MinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

private fun minimalSemantics(dark: Boolean) = SupportDeskSemanticColors(
    success = if (dark) Color(0xFF6FCF8E) else Color(0xFF1F7A3F),
    successContainer = if (dark) Color(0xFF16301F) else Color(0xFFE3F4E8),
    warning = if (dark) Color(0xFFE8B95A) else Color(0xFF8A5A00),
    warningContainer = if (dark) Color(0xFF3A2C0E) else Color(0xFFFFF4DB),
    danger = if (dark) Color(0xFFF07060) else Color(0xFFB42318),
    dangerContainer = if (dark) Color(0xFF4A1A12) else Color(0xFFFDE7E4),
    info = if (dark) Color(0xFFC8C8C4) else Color(0xFF3F3F3C),
    infoContainer = if (dark) Color(0xFF2A2A28) else Color(0xFFEDEDEB),
    onSuccess = if (dark) Color(0xFF0A1F10) else Color(0xFFFFFFFF),
    onDanger = if (dark) Color(0xFF2E0A06) else Color(0xFFFFFFFF),
)

// ---------------------------------------------------------------------------------------
// Glass — translucent layers over a warm gradient with two soft light spots. Cards are
// translucent; dialogs, menus and inputs use near-opaque surfaceContainer* roles so text
// keeps ≥4.5:1 contrast.
// ---------------------------------------------------------------------------------------
internal val GlassLightBackdrop = listOf(Color(0xFFF6E7D8), Color(0xFFF1E4E6), Color(0xFFEADCF2))
internal val GlassLightGlows = listOf(Color(0x73F2B58A), Color(0x66B9A3E3))
internal val GlassDarkBackdrop = listOf(Color(0xFF14110E), Color(0xFF1A1512), Color(0xFF16121C))
internal val GlassDarkGlows = listOf(Color(0x38E2603A), Color(0x406B4FA8))

internal val GlassLight = lightColorScheme(
    primary = Color(0xFFC1391F),
    onPrimary = Color(0xFFFFF8F2),
    primaryContainer = Color(0xFFF6DDD2),
    onPrimaryContainer = Color(0xFF6B1D0F),
    inversePrimary = Color(0xFFFF9A7A),
    secondary = Color(0xFF7D6235),
    onSecondary = Color(0xFFFFF8F2),
    secondaryContainer = Color(0xCCF1E6D2),
    onSecondaryContainer = Color(0xFF4A3A1D),
    tertiary = Color(0xFF63576B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xCCEAE1F2),
    onTertiaryContainer = Color(0xFF2E2636),
    background = Color(0xFFF2E7E5),
    onBackground = Color(0xFF1B1712),
    surface = Color(0x9EFFFFFF),
    onSurface = Color(0xFF1B1712),
    surfaceVariant = Color(0x80FFFFFF),
    onSurfaceVariant = Color(0xFF544943),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF2A2420),
    inverseOnSurface = Color(0xFFF6EFE8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DCD5),
    onErrorContainer = Color(0xFF6A1209),
    outline = Color(0x521B1712),
    outlineVariant = Color(0xA6FFFFFF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE9DED6),
    surfaceContainerLowest = Color(0xF2FFFFFF),
    surfaceContainerLow = Color(0xE6FFFFFF),
    surfaceContainer = Color(0xE6FFFBF8),
    surfaceContainerHigh = Color(0xF0FFFBF8),
    surfaceContainerHighest = Color(0xF5FFFBF8),
)

internal val GlassDark = darkColorScheme(
    primary = Color(0xFFE8704A),
    onPrimary = Color(0xFF2A0E06),
    primaryContainer = Color(0xFF4A2418),
    onPrimaryContainer = Color(0xFFF8D0C0),
    inversePrimary = Color(0xFFC1391F),
    secondary = Color(0xFFD2B57A),
    onSecondary = Color(0xFF2E2408),
    secondaryContainer = Color(0x40D2B57A),
    onSecondaryContainer = Color(0xFFF0E2BE),
    tertiary = Color(0xFFB9A7DA),
    onTertiary = Color(0xFF241C33),
    tertiaryContainer = Color(0x40B9A7DA),
    onTertiaryContainer = Color(0xFFEDE4FA),
    background = Color(0xFF14110E),
    onBackground = Color(0xFFF3EFE4),
    surface = Color(0x17FFFFFF),
    onSurface = Color(0xFFF3EFE4),
    surfaceVariant = Color(0x21FFFFFF),
    onSurfaceVariant = Color(0xFFCBC1B2),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF3EFE4),
    inverseOnSurface = Color(0xFF201A14),
    error = Color(0xFFF07A68),
    onError = Color(0xFF2E0A06),
    errorContainer = Color(0xFF4A1D15),
    onErrorContainer = Color(0xFFFFD9D2),
    outline = Color(0x52FFFFFF),
    outlineVariant = Color(0x29FFFFFF),
    surfaceBright = Color(0xFF3A332C),
    surfaceDim = Color(0xFF14110E),
    surfaceContainerLowest = Color(0xF00F0D0B),
    surfaceContainerLow = Color(0xEB1A1612),
    surfaceContainer = Color(0xEB221D18),
    surfaceContainerHigh = Color(0xF2282320),
    surfaceContainerHighest = Color(0xF52F2925),
)

internal val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)
