package com.requena.supportdesk.desktop.theme

import androidx.compose.runtime.Composable
import com.requena.supportdesk.app.SupportDeskApp

@Composable
fun SupportDeskDesktopTheme(content: @Composable () -> Unit) {
    SupportDeskApp(content = content)
}
