package com.requena.supportdesk.desktop.app

import androidx.compose.runtime.Composable
import com.requena.supportdesk.app.SupportDeskApp
import com.requena.supportdesk.app.admin.AdminWorkspaceApp

@Composable
fun DesktopSupportDeskApp() {
    SupportDeskApp {
        AdminWorkspaceApp()
    }
}
