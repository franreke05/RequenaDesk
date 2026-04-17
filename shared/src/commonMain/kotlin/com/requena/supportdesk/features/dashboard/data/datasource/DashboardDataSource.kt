package com.requena.supportdesk.features.dashboard.data.datasource

import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.features.dashboard.data.dto.DashboardSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

interface DashboardDataSource {
    suspend fun getSummary(): DashboardSummaryDto
}

class RemoteDashboardDataSource(
    private val httpClient: HttpClient,
) : DashboardDataSource {
    override suspend fun getSummary(): DashboardSummaryDto =
        httpClient.get("${supportDeskBaseUrl()}/admin/dashboard").requireApiData()
}
