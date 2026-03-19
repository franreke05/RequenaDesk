package com.requena.supportdesk.server.domain.model

data class ServerSession(
    val role: String,
    val accessToken: String,
    val refreshToken: String,
)

data class ServerTicketSnapshot(
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

data class ServerClientSnapshot(
    val id: String,
    val companyName: String,
    val productName: String,
    val email: String,
    val accountStatus: String,
    val serviceTier: String,
    val preferredContactChannel: String,
    val activeTicketCount: Int,
)

data class ServerDashboardSnapshot(
    val openTickets: Int,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
)

data class ServerAttachmentSnapshot(
    val id: String,
    val fileName: String,
    val contentType: String,
)

data class ServerDeviceRegistration(
    val id: String,
    val userId: String,
    val platform: String,
)
