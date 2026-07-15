package com.requena.supportdesk.server.domain.model

data class ServerSession(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val clientId: String? = null,
    val accessToken: String,
    val refreshToken: String,
)

data class ServerAuthIdentity(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val clientId: String? = null,
)

data class ServerTicketSnapshot(
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
    val waitingOn: String,
    val resolutionSummary: String? = null,
    val requesterId: String,
    val requesterName: String,
    val requesterEmail: String,
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val assigneeEmail: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val messages: List<ServerTicketMessageSnapshot> = emptyList(),
    val internalComments: List<ServerInternalCommentSnapshot> = emptyList(),
    val events: List<ServerTicketEventSnapshot> = emptyList(),
    val attachments: List<ServerTicketAttachmentSnapshot> = emptyList(),
)

data class ServerTicketMessageSnapshot(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)

data class ServerInternalCommentSnapshot(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)

data class ServerTicketEventSnapshot(
    val id: String,
    val ticketId: String,
    val type: String,
    val description: String,
    val actorName: String,
    val createdAt: String,
)

data class ServerTicketAttachmentSnapshot(
    val id: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: String,
)

data class ServerClientSnapshot(
    val id: String,
    val ownerAdminId: String,
    val companyName: String,
    val productName: String,
    val contactName: String = "",
    val email: String,
    val accountStatus: String,
    val serviceTier: String,
    val preferredContactChannel: String,
    val activeTicketCount: Int,
    val openTasksCount: Int = 0,
    val monthlyLoggedMinutes: Int = 0,
)

data class ServerDashboardSnapshot(
    val openTickets: Int,
    val pendingClientTickets: Int,
    val resolvedToday: Int,
    val activeClients: Int,
    val monthLabel: String = "",
    val totalMinutes: Int = 0,
    val billableMinutes: Int = 0,
    val selectedClientId: String? = null,
    val selectedClientMinutes: Int = 0,
    val selectedClientBillableMinutes: Int = 0,
    val dailyMinutes: List<ServerDailyMinutesSnapshot> = emptyList(),
    val availableTasks: List<ServerTaskSnapshot> = emptyList(),
)

data class ServerTaskLabelSnapshot(
    val id: String,
    val ownerAdminId: String,
    val name: String,
    val colorHex: String,
    val tasksCount: Int,
)

data class ServerTaskSnapshot(
    val id: String,
    val ownerAdminId: String,
    val title: String,
    val description: String,
    val clientId: String? = null,
    val clientName: String? = null,
    val labelId: String,
    val labelName: String,
    val labelColorHex: String,
    val dueDate: String? = null,
    val completed: Boolean,
    val loggedMinutes: Int,
    val loggedSeconds: Int = loggedMinutes * 60,
    val createdAt: String,
    val updatedAt: String,
)

data class ServerTimeLogSnapshot(
    val id: String,
    val ownerAdminId: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val seconds: Int = minutes * 60,
    val workDate: String,
    val note: String,
    val billable: Boolean,
    val createdAt: String,
)

data class ServerDailyMinutesSnapshot(
    val workDate: String,
    val minutes: Int,
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

data class ServerTicketMessageCreated(
    val ticketId: String,
    val messageId: String,
)

data class ServerTicketFieldUpdate(
    val ticketId: String,
    val value: String,
)

data class ServerAttachmentCreated(
    val ticketId: String,
    val attachmentId: String,
)

data class ServerInvoiceItemSnapshot(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val sortOrder: Int,
)

data class ServerInvoiceSnapshot(
    val invoiceNumber: String,
    val clientId: String,
    val clientName: String,
    val issuedAt: String,
    val dueAt: String?,
    val notes: String?,
    val taxPercent: Double,
    val items: List<ServerInvoiceItemSnapshot>,
)
