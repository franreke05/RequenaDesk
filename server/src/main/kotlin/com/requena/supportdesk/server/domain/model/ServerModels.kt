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
    val name: String,
    val colorHex: String,
    val tasksCount: Int,
)

data class ServerTaskSnapshot(
    val id: String,
    val title: String,
    val description: String,
    val clientId: String? = null,
    val clientName: String? = null,
    val labelId: String,
    val labelName: String,
    val labelColorHex: String,
    val completed: Boolean,
    val loggedMinutes: Int,
    val createdAt: String,
    val updatedAt: String,
)

data class ServerTimeLogSnapshot(
    val id: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
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
