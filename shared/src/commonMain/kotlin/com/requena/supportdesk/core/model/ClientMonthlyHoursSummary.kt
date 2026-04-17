package com.requena.supportdesk.core.model

data class ClientMonthlyHoursSummary(
    val monthLabel: String,
    val totalMinutes: Int,
    val billableMinutes: Int,
    val entriesCount: Int,
)
