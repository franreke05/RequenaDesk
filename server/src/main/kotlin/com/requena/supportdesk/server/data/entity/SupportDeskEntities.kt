package com.requena.supportdesk.server.data.entity

data class TicketEntity(
    val id: String,
    val ticketNumber: String,
    val subject: String,
    val description: String,
    val category: String,
    val affectedApp: String,
    val platform: String,
    val appVersion: String? = null,
    val clientReference: String? = null,
    val status: String,
    val priority: String,
    val waitingOn: String,
    val resolutionSummary: String? = null,
)

data class ClientEntity(
    val id: String,
    val ownerAdminId: String,
    val companyName: String,
    val productName: String,
    val email: String,
    val accountStatus: String,
    val serviceTier: String,
    val preferredContactChannel: String,
    val activeTicketCount: Int,
)

data class DashboardEntity(
    val openTickets: Int,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
)

data class AttachmentEntity(
    val id: String,
    val fileName: String,
    val contentType: String,
)

data class DeviceEntity(
    val id: String,
    val userId: String,
    val platform: String,
)
