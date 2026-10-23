package com.requena.supportdesk.designsystem.components.buttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.requena.supportdesk.app.LocalSupportDeskThemeController
import com.requena.supportdesk.designsystem.components.feedback.AppearanceDialog

/** Opens the appearance picker (style + light/dark/system). Replaces the old dark-mode toggle. */
@Composable
fun AppearanceButton(fullWidth: Boolean = false) {
    val controller = LocalSupportDeskThemeController.current
    var open by remember { mutableStateOf(false) }
    SecondaryButton(
        text = "Apariencia",
        onClick = { open = true },
        fullWidth = fullWidth,
        icon = Lucide.Palette,
    )
    if (open) {
        AppearanceDialog(
            selectedStyle = controller.style,
            selectedMode = controller.mode,
            previewDark = controller.isDarkMode,
            onStyleSelected = controller.setStyle,
            onModeSelected = controller.setMode,
            onDismiss = { open = false },
        )
    }
}
