package com.requena.supportdesk.app.mobile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.designsystem.tokens.SupportDeskTypography

// Larger corner radii than SupportDeskDesignTheme's SupportDeskShapes: mobile touch
// targets favor a rounder, thumb-friendly silhouette. Color and typography still
// come from the shared token source below.
private val MobileShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

// Relies on being nested inside SupportDeskDesignTheme (see SupportDeskApp / SupportDeskPlatformApp.ios.kt),
// which is what provides the ambient colorScheme read below.
@Composable
fun SupportDeskMobileTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SupportDeskTypography,
        shapes = MobileShapes,
    ) {
        Surface(
            color = colorScheme.background,
            content = content,
        )
    }
}
