package com.requena.supportdesk.features.dashboard.data.dto

data class DashboardSummaryDto(
    val openTickets: Int,
    val inProgressTickets: Int,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
)
