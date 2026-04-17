package com.requena.supportdesk.app.admin

import com.requena.supportdesk.core.navigation.AppDestination

data class AdminNavigationState(
    val destination: AppDestination = AppDestination.Dashboard,
)

enum class AdminLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}
