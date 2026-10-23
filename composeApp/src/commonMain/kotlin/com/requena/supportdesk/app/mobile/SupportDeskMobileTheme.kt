package com.requena.supportdesk.app.mobile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

// Larger corner radii than the desktop shapes: mobile touch targets favor a rounder,
// thumb-friendly silhouette. Minimalista stays noticeably squarer.
private val MobileShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val MobileMinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

// Relies on being nested inside SupportDeskDesignTheme (see SupportDeskApp / SupportDeskPlatformApp.ios.kt),
// which provides the ambient colorScheme, typography and style read below.
@Composable
fun SupportDeskMobileTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val style = SupportDeskThemeTokens.style
    val translucent = SupportDeskThemeTokens.surfaceStyle.translucentSurfaces

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = if (style == SupportDeskStyle.MINIMAL) MobileMinimalShapes else MobileShapes,
    ) {
        Surface(
            // Glass already paints its backdrop at the root; keep it visible.
            color = if (translucent) Color.Transparent else colorScheme.background,
            contentColor = colorScheme.onBackground,
            content = content,
        )
    }
}
