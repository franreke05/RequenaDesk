package com.requena.supportdesk.features.dashboard.data.mapper

import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.features.dashboard.data.dto.DashboardSummaryDto

object DashboardMapper {
    fun fromDto(dto: DashboardSummaryDto): DashboardSummary = DashboardSummary(
        openTickets = dto.openTickets,
        inProgressTickets = dto.inProgressTickets,
        pendingClientTickets = dto.pendingClientTickets,
        resolvedToday = dto.resolvedToday,
        activeClients = dto.activeClients,
    )
}
