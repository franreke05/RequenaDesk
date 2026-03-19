package com.requena.supportdesk.features.dashboard.presentation.state

import com.requena.supportdesk.core.model.DashboardSummary

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
