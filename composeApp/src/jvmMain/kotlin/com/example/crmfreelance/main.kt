package com.example.crmfreelance

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.requena.supportdesk.app.SupportDeskApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OryKai software",
    ) {
        SupportDeskApp()
    }
}
