package com.requena.supportdesk.features.dashboard.data.repository

import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.dashboard.data.datasource.DashboardDataSource
import com.requena.supportdesk.features.dashboard.data.mapper.DashboardMapper
import com.requena.supportdesk.features.dashboard.domain.repository.DashboardRepository

class DashboardRepositoryImpl(
    private val dataSource: DashboardDataSource,
) : DashboardRepository {
    override suspend fun getSummary(): AppResult<DashboardSummary> = runCatching {
        DashboardMapper.fromDto(dataSource.getSummary())
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cargar el dashboard.", cause = it) },
    )
}
