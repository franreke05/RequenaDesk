package com.requena.supportdesk.features.dashboard.presentation.event

sealed interface DashboardUiEvent {
    object Refresh : DashboardUiEvent
}
