package com.requena.supportdesk.features.dashboard.domain.usecase

import com.requena.supportdesk.features.dashboard.domain.repository.DashboardRepository

class GetDashboardSummaryUseCase(
    private val repository: DashboardRepository,
) {
    suspend operator fun invoke() = repository.getSummary()
}
