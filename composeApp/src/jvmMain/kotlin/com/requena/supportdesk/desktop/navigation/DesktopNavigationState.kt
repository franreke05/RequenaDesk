package com.requena.supportdesk.desktop.navigation

import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.navigation.AppDestination

data class DesktopNavigationState(
    val role: UserRole? = null,
    val destination: AppDestination = AppDestination.Login,
)

fun desktopHomeFor(role: UserRole): AppDestination = when (role) {
    UserRole.ADMIN -> AppDestination.Dashboard
    UserRole.CLIENT -> AppDestination.Tickets
}
