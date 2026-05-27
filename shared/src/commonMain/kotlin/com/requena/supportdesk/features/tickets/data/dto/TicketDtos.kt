package com.requena.supportdesk.features.tickets.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TicketDto(
    val id: String,
    val clientId: String = "",
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
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterEmail: String = "",
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val clientAcceptedCloseAt: String? = null,
    val adminAcceptedCloseAt: String? = null,
    val archivedAt: String? = null,
    val satisfactionRating: Int? = null,
)

@Serializable
data class CreateTicketRequestDto(
    val clientId: String,
    val subject: String,
    val description: String,
    val category: String,
    val affectedApp: String,
    val platform: String,
    val appVersion: String? = null,
    val stepsToReproduce: String? = null,
    val clientReference: String? = null,
    val priority: String,
)

@Serializable
data class UpdateTicketStatusRequestDto(
    val status: String,
)

@Serializable
data class UpdateTicketPriorityRequestDto(
    val priority: String,
)

@Serializable
data class TicketCloseAcceptanceRequestDto(
    val resolutionSummary: String? = null,
)

@Serializable
data class TicketSatisfactionRequestDto(
    val rating: Int,
)
