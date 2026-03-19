package com.requena.supportdesk.features.tickets.data.dto

data class TicketDto(
    val id: String,
    val clientId: String,
    val ticketNumber: String,
    val subject: String,
    val description: String,
    val category: String,
    val affectedApp: String,
    val platform: String,
    val appVersion: String? = null,
    val stepsToReproduce: String? = null,
    val clientReference: String? = null,
    val status: String,
    val priority: String,
    val waitingOn: String = "ADMIN",
    val resolutionSummary: String? = null,
)
