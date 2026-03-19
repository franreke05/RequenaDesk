package com.requena.supportdesk.app

import androidx.compose.runtime.Composable
import com.requena.supportdesk.designsystem.theme.SupportDeskDesignTheme

@Composable
fun SupportDeskTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    SupportDeskDesignTheme(
        useDarkTheme = useDarkTheme,
        content = content,
    )
}
