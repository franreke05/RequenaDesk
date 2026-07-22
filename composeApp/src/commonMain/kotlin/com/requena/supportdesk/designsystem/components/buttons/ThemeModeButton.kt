package com.requena.supportdesk.designsystem.components.buttons

import androidx.compose.runtime.Composable
import com.requena.supportdesk.app.LocalSupportDeskThemeController

@Composable
fun ThemeModeButton(fullWidth: Boolean = false) {
    val controller = LocalSupportDeskThemeController.current
    SecondaryButton(
        text = if (controller.isDarkMode) "Light mode" else "Dark mode",
        onClick = controller.toggleDarkMode,
        fullWidth = fullWidth,
    )
}
