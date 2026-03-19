package com.requena.supportdesk.features.dashboard.domain.repository

import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.result.AppResult

interface DashboardRepository {
    suspend fun getSummary(): AppResult<DashboardSummary>
}
