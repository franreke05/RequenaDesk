package com.requena.supportdesk.features.tickets.data.dto

import kotlinx.serialization.Serializable

@Serializable
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
    val requesterId: String,
    val requesterName: String,
    val requesterEmail: String,
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val assigneeEmail: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val messages: List<TicketMessageDto> = emptyList(),
    val internalComments: List<InternalCommentDto> = emptyList(),
    val events: List<TicketEventDto> = emptyList(),
    val attachments: List<TicketAttachmentDto> = emptyList(),
)

@Serializable
data class TicketMessageDto(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)

@Serializable
data class InternalCommentDto(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)

@Serializable
data class TicketEventDto(
    val id: String,
    val ticketId: String,
    val type: String,
    val description: String,
    val actorName: String,
    val createdAt: String,
)

@Serializable
data class TicketAttachmentDto(
    val id: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: String,
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
data class CreateTicketMessageRequestDto(
    val authorId: String,
    val body: String,
)

@Serializable
data class UpdateTicketStatusRequestDto(
    val status: String,
)

@Serializable
data class UpdateTicketPriorityRequestDto(
    val priority: String,
)
