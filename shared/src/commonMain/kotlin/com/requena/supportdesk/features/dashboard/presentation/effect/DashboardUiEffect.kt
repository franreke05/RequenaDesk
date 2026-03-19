package com.requena.supportdesk.features.dashboard.presentation.effect

sealed interface DashboardUiEffect {
    data class ShowMessage(val message: String) : DashboardUiEffect
}
