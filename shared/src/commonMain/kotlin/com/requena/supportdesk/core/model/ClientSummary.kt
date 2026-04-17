package com.requena.supportdesk.core.model

data class ClientSummary(
    val id: String,
    val companyName: String,
    val productName: String,
    val contactName: String,
    val email: String,
    val accountStatus: ClientAccountStatus,
    val serviceTier: ClientServiceTier,
    val openTasksCount: Int = 0,
    val monthlyLoggedMinutes: Int = 0,
)
