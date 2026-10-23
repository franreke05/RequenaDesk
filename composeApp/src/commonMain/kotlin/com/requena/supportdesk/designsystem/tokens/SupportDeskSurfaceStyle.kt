package com.requena.supportdesk.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How the one "emphasized" card per screen stands out in each style. */
enum class EmphasisTreatment {
    /** Artesanal: ink outline + hard, unblurred offset shadow in the accent color. */
    HARD_OFFSET,

    /** Minimalista: accent outline, no shadow. */
    OUTLINE,

    /** Glass: luminous border + soft colored glow. */
    GLOW,
}

/**
 * Surface treatment that changes per visual style. Components read this instead of
 * hardcoding borders/shadows so the same screen renders correctly in every style.
 */
@Immutable
data class SupportDeskSurfaceStyle(
    val cardBorderWidth: Dp = 1.dp,
    val emphasis: EmphasisTreatment = EmphasisTreatment.HARD_OFFSET,
    /** Gradient stops painted behind the whole app; empty = plain background color. */
    val backdropColors: List<Color> = emptyList(),
    /** Two soft blurred light spots behind the content (Glass). */
    val backdropGlows: List<Color> = emptyList(),
    /** Whether `surface` colors are translucent and need a backdrop to read well. */
    val translucentSurfaces: Boolean = false,
)

internal val LocalSupportDeskSurfaceStyle = staticCompositionLocalOf { SupportDeskSurfaceStyle() }
