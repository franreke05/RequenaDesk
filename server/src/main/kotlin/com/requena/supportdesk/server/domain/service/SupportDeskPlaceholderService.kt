package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository

class SupportDeskPlaceholderService(
    private val repository: SupportDeskRepository,
) {
    fun login(email: String = ""): ServerSession = ServerSession(
        role = "ADMIN",
        accessToken = if (email.isBlank()) "placeholder-access-token" else "placeholder-access-token-${email.substringBefore('@')}",
        refreshToken = "placeholder-refresh-token",
    )

    fun refresh(): ServerSession = ServerSession(
        role = "ADMIN",
        accessToken = "placeholder-access-token-refreshed",
        refreshToken = "placeholder-refresh-token-refreshed",
    )

    fun logoutMessage(): String = "Logout placeholder completed."

    fun tickets() = repository.getTickets()

    fun ticket(id: String) = repository.getTicket(id)

    fun createdTicket(request: CreateTicketRequest = CreateTicketRequest()): ServerTicketSnapshot = ServerTicketSnapshot(
        id = "ticket-created",
        ticketNumber = "SD-1099",
        subject = request.subject.ifBlank { "Nuevo ticket placeholder" },
        description = request.description.ifBlank { "Placeholder body for the ticket creation flow." },
        category = request.category,
        affectedApp = request.affectedApp.ifBlank { "Assigned product" },
        platform = request.platform,
        appVersion = request.appVersion,
        clientReference = request.clientReference,
        status = "OPEN",
        priority = request.priority,
        waitingOn = "ADMIN",
        resolutionSummary = null,
    )

    fun createdMessage(ticketId: String): Pair<String, String> = ticketId to "Placeholder reply stored."

    fun updatedStatus(ticketId: String, request: UpdateTicketStatusRequest = UpdateTicketStatusRequest()): Pair<String, String> =
        ticketId to request.status

    fun updatedPriority(ticketId: String, request: UpdateTicketPriorityRequest = UpdateTicketPriorityRequest()): Pair<String, String> =
        ticketId to request.priority

    fun uploadedAttachment(ticketId: String): Pair<String, String> = ticketId to "attachment-1"

    fun attachment(id: String) = repository.getAttachment(id)

    fun clients() = repository.getClients()

    fun createdClient(request: CreateClientRequest = CreateClientRequest()): ServerClientSnapshot = ServerClientSnapshot(
        id = "client-created",
        companyName = request.companyName.ifBlank { "New client placeholder" },
        productName = request.productName.ifBlank { "Assigned product" },
        email = request.email.ifBlank { "client@example.com" },
        accountStatus = request.accountStatus,
        serviceTier = request.serviceTier,
        preferredContactChannel = request.preferredContactChannel,
        activeTicketCount = 0,
    )

    fun dashboard() = repository.getDashboard()

    fun registerDevice(request: RegisterDeviceRequest = RegisterDeviceRequest()): ServerDeviceRegistration =
        repository.registerDevice().copy(
            userId = request.userId,
            platform = request.platform,
        )
}
