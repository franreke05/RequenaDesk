package com.requena.supportdesk.app

import androidx.compose.runtime.Composable
import com.requena.supportdesk.core.appearance.SupportDeskStyle
import com.requena.supportdesk.designsystem.theme.SupportDeskDesignTheme

@Composable
fun SupportDeskTheme(
    useDarkTheme: Boolean,
    style: SupportDeskStyle = SupportDeskStyle.Default,
    content: @Composable () -> Unit,
) {
    SupportDeskDesignTheme(
        useDarkTheme = useDarkTheme,
        style = style,
        content = content,
    )
}
