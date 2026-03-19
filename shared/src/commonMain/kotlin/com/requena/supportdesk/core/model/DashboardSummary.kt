package com.requena.supportdesk.core.model

data class DashboardSummary(
    val openTickets: Int,
    val inProgressTickets: Int,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
)
