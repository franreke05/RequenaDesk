package com.requena.supportdesk.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SupportDeskSemanticColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    // Content colors for full-strength (non-container) fills - e.g. a Snackbar, where the
    // container tint used for badges/cards is too low-contrast to read as a floating shape.
    val onSuccess: Color,
    val onDanger: Color,
)

internal val LocalSupportDeskSemanticColors = staticCompositionLocalOf {
    SupportDeskSemanticColors(
        success = Color(0xFF3F6B4A),
        successContainer = Color(0xFFDCE8DD),
        warning = Color(0xFF8A6115),
        warningContainer = Color(0xFFFCF2D8),
        danger = Color(0xFFA5291A),
        dangerContainer = Color(0xFFF5DAD3),
        info = Color(0xFF6B5636),
        infoContainer = Color(0xFFEAE0C8),
        onSuccess = Color(0xFFF8F4EA),
        onDanger = Color(0xFFF8F4EA),
    )
}
