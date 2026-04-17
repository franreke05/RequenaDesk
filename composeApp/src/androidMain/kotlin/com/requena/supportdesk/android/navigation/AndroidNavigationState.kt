package com.requena.supportdesk.android.navigation

import com.requena.supportdesk.core.navigation.AppDestination

data class AndroidNavigationState(
    val destination: AppDestination = AppDestination.Tickets,
)
