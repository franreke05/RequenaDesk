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
)

internal val LocalSupportDeskSemanticColors = staticCompositionLocalOf {
    SupportDeskSemanticColors(
        success = Color(0xFF1E6B3C),
        successContainer = Color(0xFFE8F5EC),
        warning = Color(0xFF8A6115),
        warningContainer = Color(0xFFFCF2D8),
        danger = Color(0xFF9B2C2C),
        dangerContainer = Color(0xFFFCE8E8),
        info = Color(0xFF1D4ED8),
        infoContainer = Color(0xFFE8F0FF),
    )
}
