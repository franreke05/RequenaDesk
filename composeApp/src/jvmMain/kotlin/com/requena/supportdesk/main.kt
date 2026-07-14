package com.requena.supportdesk

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(width = 1440.dp, height = 900.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "OryKai software",
        state = windowState,
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(1024, 720)
        }
        App()
    }
}
