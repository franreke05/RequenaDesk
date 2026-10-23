package com.requena.supportdesk.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.designsystem.components.layout.StyleBackdrop
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSpacing
import com.requena.supportdesk.designsystem.tokens.LocalSupportDeskSurfaceStyle
import com.requena.supportdesk.designsystem.tokens.SupportDeskElevations
import com.requena.supportdesk.designsystem.tokens.SupportDeskSemanticColors
import com.requena.supportdesk.designsystem.tokens.SupportDeskSpacing
import com.requena.supportdesk.designsystem.tokens.SupportDeskSurfaceStyle

internal val LocalSupportDeskStyle = staticCompositionLocalOf { SupportDeskStyle.Default }

object SupportDeskThemeTokens {
    val style: SupportDeskStyle
        @Composable get() = LocalSupportDeskStyle.current

    val spacing: SupportDeskSpacing
        @Composable get() = LocalSupportDeskSpacing.current

    val semanticColors: SupportDeskSemanticColors
        @Composable get() = LocalSupportDeskSemanticColors.current

    val elevations: SupportDeskElevations
        @Composable get() = LocalSupportDeskElevations.current

    val surfaceStyle: SupportDeskSurfaceStyle
        @Composable get() = LocalSupportDeskSurfaceStyle.current

    val typography: Typography
        @Composable get() = MaterialTheme.typography

    /**
     * Fill for full-page containers. Transparent in styles that paint their own backdrop
     * (Glass), so the gradient behind the app stays visible; `background` otherwise.
     */
    val pageBackground: Color
        @Composable get() = if (LocalSupportDeskSurfaceStyle.current.backdropColors.isEmpty()) {
            MaterialTheme.colorScheme.background
        } else {
            Color.Transparent
        }
}

/**
 * Root theme. Palettes, shapes, typography and surface treatment come from the selected
 * [style] (see SupportDeskStylePalettes.kt); ARTESANAL is the original editorial look.
 */
@Composable
fun SupportDeskDesignTheme(
    useDarkTheme: Boolean,
    style: SupportDeskStyle = SupportDeskStyle.Default,
    content: @Composable () -> Unit,
) {
    val tokens = remember(style, useDarkTheme) { supportDeskStyleTokens(style, useDarkTheme) }
    val colorScheme = tokens.colorScheme
    CompositionLocalProvider(
        LocalSupportDeskSpacing provides SupportDeskSpacing(),
        LocalSupportDeskSemanticColors provides tokens.semanticColors,
        LocalSupportDeskElevations provides SupportDeskElevations(),
        LocalSupportDeskSurfaceStyle provides tokens.surfaceStyle,
        LocalSupportDeskStyle provides style,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = tokens.typography,
            shapes = tokens.shapes,
        ) {
            if (tokens.surfaceStyle.backdropColors.isEmpty()) {
                Surface(color = colorScheme.background, content = content)
            } else {
                StyleBackdrop(fallback = colorScheme.background) {
                    Surface(
                        color = Color.Transparent,
                        contentColor = colorScheme.onBackground,
                        content = content,
                    )
                }
            }
        }
    }
}
