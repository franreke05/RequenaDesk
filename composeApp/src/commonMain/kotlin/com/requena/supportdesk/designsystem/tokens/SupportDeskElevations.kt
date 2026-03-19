package com.requena.supportdesk.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SupportDeskElevations(
    val subtle: Dp = 1.dp,
    val raised: Dp = 4.dp,
    val floating: Dp = 8.dp,
)

internal val LocalSupportDeskElevations = staticCompositionLocalOf { SupportDeskElevations() }
