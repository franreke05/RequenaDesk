package com.requena.supportdesk.features.dashboard.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryDto(
    val openTickets: Int,
    val inProgressTickets: Int = 0,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
)
