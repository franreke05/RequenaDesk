package com.requena.supportdesk.core.model

data class DashboardSnapshot(
    val monthLabel: String,
    val totalMinutes: Int,
    val billableMinutes: Int,
    val selectedClientId: String? = null,
    val selectedClientMinutes: Int = 0,
    val selectedClientBillableMinutes: Int = 0,
    val dailyMinutes: Map<String, Int> = emptyMap(),
)
