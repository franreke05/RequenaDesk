package com.requena.supportdesk.designsystem.components.buttons

import androidx.compose.runtime.Composable
import com.requena.supportdesk.app.LocalSupportDeskThemeController

@Composable
fun ThemeModeButton() {
    val controller = LocalSupportDeskThemeController.current
    SecondaryButton(
        text = if (controller.isDarkMode) "Tema claro" else "Tema oscuro",
        onClick = controller.toggleDarkMode,
    )
}
