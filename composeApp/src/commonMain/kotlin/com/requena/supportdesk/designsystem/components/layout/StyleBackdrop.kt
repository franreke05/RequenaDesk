package com.requena.supportdesk.designsystem.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

/**
 * Paints the style's backdrop (gradient + soft light spots) behind the whole app. The light
 * spots are radial gradients rather than blur, so they look the same on every platform and
 * content on top always stays crisp.
 */
@Composable
fun StyleBackdrop(
    fallback: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val surfaceStyle = SupportDeskThemeTokens.surfaceStyle
    val colors = surfaceStyle.backdropColors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(fallback)
            .let { if (colors.size >= 2) it.background(Brush.linearGradient(colors)) else it },
    ) {
        val spot = (if (maxWidth < maxHeight) maxWidth else maxHeight) * 1.1f
        surfaceStyle.backdropGlows.getOrNull(0)?.let { glow ->
            GlowSpot(glow, Modifier.size(spot).offset(x = -spot * 0.3f, y = -spot * 0.3f))
        }
        surfaceStyle.backdropGlows.getOrNull(1)?.let { glow ->
            GlowSpot(glow, Modifier.size(spot).offset(x = maxWidth - spot * 0.65f, y = maxHeight - spot * 0.7f))
        }
        content()
    }
}

@Composable
private fun GlowSpot(color: Color, modifier: Modifier) {
    Box(modifier.background(Brush.radialGradient(listOf(color, Color.Transparent)), CircleShape))
}
