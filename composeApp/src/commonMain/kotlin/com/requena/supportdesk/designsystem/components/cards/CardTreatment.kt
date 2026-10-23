package com.requena.supportdesk.designsystem.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.EmphasisTreatment

/**
 * Card border for the current style: a hairline by default; for the one emphasized card per
 * screen, an ink outline (Artesanal), an accent outline (Minimalista) or a luminous edge (Glass).
 */
@Composable
fun supportDeskCardBorder(emphasized: Boolean): BorderStroke {
    val colors = MaterialTheme.colorScheme
    val surfaceStyle = SupportDeskThemeTokens.surfaceStyle
    if (!emphasized) return BorderStroke(surfaceStyle.cardBorderWidth, colors.outlineVariant)
    return when (surfaceStyle.emphasis) {
        EmphasisTreatment.HARD_OFFSET -> BorderStroke(2.dp, colors.onSurface)
        EmphasisTreatment.OUTLINE -> BorderStroke(1.5.dp, colors.primary)
        EmphasisTreatment.GLOW -> BorderStroke(1.5.dp, colors.primary.copy(alpha = 0.55f))
    }
}

/** Shadow/glow drawn behind an emphasized card, matching [supportDeskCardBorder]. */
@Composable
fun Modifier.supportDeskCardEmphasis(emphasized: Boolean): Modifier {
    if (!emphasized) return this
    val primary = MaterialTheme.colorScheme.primary
    return when (SupportDeskThemeTokens.surfaceStyle.emphasis) {
        EmphasisTreatment.HARD_OFFSET -> hardOffsetShadow(primary)
        EmphasisTreatment.OUTLINE -> this
        EmphasisTreatment.GLOW -> softGlow(primary)
    }
}

// Glass halo: a few inflated rounded rects with fading alpha. A real elevation shadow would
// show through the translucent card and muddy it, so the glow is drawn only around the edge.
private fun Modifier.softGlow(color: Color, cornerRadius: Dp = 16.dp): Modifier = drawBehind {
    val radius = cornerRadius.toPx()
    // Keep the card interior untouched: only the ring outside the card gets the halo.
    val cardOutline = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius, radius)))
    }
    clipPath(cardOutline, clipOp = ClipOp.Difference) {
        listOf(12.dp to 0.05f, 8.dp to 0.07f, 4.dp to 0.10f).forEach { (spread, alpha) ->
            val s = spread.toPx()
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(-s, -s),
                size = Size(size.width + 2 * s, size.height + 2 * s),
                cornerRadius = CornerRadius(radius + s, radius + s),
            )
        }
    }
}

// Artesanal "printed panel": hard, unblurred offset shadow in the accent color, borrowed from
// the portfolio's .comic-ink-outline utility. Used sparingly, like the source.
private fun Modifier.hardOffsetShadow(
    color: Color,
    offset: Dp = 4.dp,
    cornerRadius: Dp = 8.dp,
): Modifier = drawBehind {
    val offsetPx = offset.toPx()
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetPx, offsetPx),
        size = size,
        cornerRadius = CornerRadius(radiusPx, radiusPx),
    )
}
