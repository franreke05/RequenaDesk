package com.requena.supportdesk.android.app

import androidx.compose.runtime.Composable
import com.requena.supportdesk.app.SupportDeskApp
import com.requena.supportdesk.app.mobile.MobileWorkspaceApp

@Composable
fun AndroidSupportDeskApp() {
    SupportDeskApp {
        MobileWorkspaceApp()
    }
}
