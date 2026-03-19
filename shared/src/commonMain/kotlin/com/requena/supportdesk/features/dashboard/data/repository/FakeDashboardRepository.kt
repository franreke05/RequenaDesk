package com.requena.supportdesk.features.dashboard.data.repository

import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.dashboard.domain.repository.DashboardRepository

class FakeDashboardRepository : DashboardRepository {
    override suspend fun getSummary(): AppResult<DashboardSummary> = AppResult.Success(SupportDeskSeed.dashboardSummary())
}
