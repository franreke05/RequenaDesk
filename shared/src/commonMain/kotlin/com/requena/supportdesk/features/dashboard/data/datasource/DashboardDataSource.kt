package com.requena.supportdesk.features.dashboard.data.datasource

import com.requena.supportdesk.features.dashboard.data.dto.DashboardSummaryDto

interface DashboardDataSource {
    suspend fun getSummary(): DashboardSummaryDto
}
