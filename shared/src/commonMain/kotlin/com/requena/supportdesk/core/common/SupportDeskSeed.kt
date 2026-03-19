package com.requena.supportdesk.core.common

import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.model.InternalComment
import com.requena.supportdesk.core.model.NotificationDevice
import com.requena.supportdesk.core.model.PreferredContactChannel
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketEvent
import com.requena.supportdesk.core.model.TicketMessage
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn

object SupportDeskSeed {
    val adminUser = User(
        id = "user-admin",
        name = "Fran Requena",
        email = SUPPORT_DESK_ADMIN_EMAIL,
        role = UserRole.ADMIN,
    )

    val clientUser = User(
        id = "user-client",
        name = "Cliente Demo",
        email = SUPPORT_DESK_CLIENT_EMAIL,
        role = UserRole.CLIENT,
        clientId = "client-1",
    )

    val clients = listOf(
        Client(
            id = "client-1",
            companyName = "Northwind Studio",
            productName = "Northwind Desk",
            contactName = "Ana Torres",
            email = "ana@northwind.dev",
            accountStatus = ClientAccountStatus.ACTIVE,
            serviceTier = ClientServiceTier.PRIORITY,
            preferredContactChannel = PreferredContactChannel.TICKET,
            activeTicketCount = 2,
        ),
        Client(
            id = "client-2",
            companyName = "Pixel Forge",
            productName = "Forge Flow",
            contactName = "David Vega",
            email = "david@pixelforge.dev",
            accountStatus = ClientAccountStatus.ACTIVE,
            serviceTier = ClientServiceTier.STANDARD,
            preferredContactChannel = PreferredContactChannel.EMAIL,
            activeTicketCount = 1,
        ),
    )

    fun tickets(): List<Ticket> {
        val firstAttachment = Attachment(
            id = "attachment-1",
            fileName = "error-log.txt",
            contentType = "text/plain",
            sizeBytes = 2048,
            uploadedBy = clientUser.name,
            uploadedAt = "2026-03-19T08:30:00Z",
        )

        val firstTicketMessages = listOf(
            TicketMessage(
                id = "message-1",
                ticketId = "ticket-1",
                authorId = clientUser.id,
                authorName = clientUser.name,
                body = "La aplicacion de escritorio no arranca despues de actualizar.",
                createdAt = "2026-03-19T08:30:00Z",
                attachments = listOf(firstAttachment),
            ),
            TicketMessage(
                id = "message-2",
                ticketId = "ticket-1",
                authorId = adminUser.id,
                authorName = adminUser.name,
                body = "He revisado el problema y estoy preparando un parche.",
                createdAt = "2026-03-19T09:10:00Z",
            ),
        )

        return listOf(
            Ticket(
                id = "ticket-1",
                clientId = "client-1",
                ticketNumber = "SD-1001",
                subject = "Desktop app no arranca",
                description = "Error critico al iniciar la ultima build enviada.",
                category = TicketCategory.BUG,
                affectedApp = "Northwind Desk",
                platform = SupportPlatform.DESKTOP,
                appVersion = "1.8.2 (418)",
                stepsToReproduce = "Abrir la app desde el acceso directo despues de instalar la ultima build.",
                clientReference = "NW-REL-418",
                status = TicketStatus.IN_PROGRESS,
                priority = TicketPriority.HIGH,
                waitingOn = WaitingOn.ADMIN,
                requester = clientUser,
                assignee = adminUser,
                createdAt = "2026-03-19T08:30:00Z",
                updatedAt = "2026-03-19T09:10:00Z",
                attachments = listOf(firstAttachment),
                messages = firstTicketMessages,
                internalComments = listOf(
                    InternalComment(
                        id = "comment-1",
                        ticketId = "ticket-1",
                        authorId = adminUser.id,
                        authorName = adminUser.name,
                        body = "Posible incompatibilidad con el nuevo empaquetado MSI.",
                        createdAt = "2026-03-19T09:20:00Z",
                    ),
                ),
                events = listOf(
                    TicketEvent(
                        id = "event-1",
                        ticketId = "ticket-1",
                        type = "CREATED",
                        description = "Ticket creado por cliente",
                        actorName = clientUser.name,
                        createdAt = "2026-03-19T08:30:00Z",
                    ),
                    TicketEvent(
                        id = "event-2",
                        ticketId = "ticket-1",
                        type = "STATUS_CHANGED",
                        description = "Estado cambiado a IN_PROGRESS",
                        actorName = adminUser.name,
                        createdAt = "2026-03-19T09:10:00Z",
                    ),
                ),
            ),
            Ticket(
                id = "ticket-2",
                clientId = "client-2",
                ticketNumber = "SD-1002",
                subject = "Anadir campo de referencia en formulario",
                description = "Necesitamos un campo extra para enlazar pedidos con tickets.",
                category = TicketCategory.CHANGE_REQUEST,
                affectedApp = "Forge Flow",
                platform = SupportPlatform.WEB,
                appVersion = "2.4.0",
                stepsToReproduce = "Abrir el formulario de alta de ticket y revisar los campos visibles.",
                clientReference = "PF-Q2-REQ-12",
                status = TicketStatus.PENDING_CLIENT,
                priority = TicketPriority.MEDIUM,
                waitingOn = WaitingOn.CLIENT,
                requester = User(
                    id = "user-client-2",
                    name = "David Vega",
                    email = "david@pixelforge.dev",
                    role = UserRole.CLIENT,
                    clientId = "client-2",
                ),
                assignee = adminUser,
                createdAt = "2026-03-18T16:00:00Z",
                updatedAt = "2026-03-19T07:50:00Z",
                resolutionSummary = "Pending client confirmation about the exact reference format.",
                messages = listOf(
                    TicketMessage(
                        id = "message-3",
                        ticketId = "ticket-2",
                        authorId = "user-client-2",
                        authorName = "David Vega",
                        body = "Adjunto mockup y comportamiento esperado.",
                        createdAt = "2026-03-18T16:05:00Z",
                    ),
                ),
                events = listOf(
                    TicketEvent(
                        id = "event-3",
                        ticketId = "ticket-2",
                        type = "PRIORITY_SET",
                        description = "Prioridad asignada a MEDIUM",
                        actorName = adminUser.name,
                        createdAt = "2026-03-18T16:10:00Z",
                    ),
                ),
            ),
        )
    }

    fun dashboardSummary(): DashboardSummary = DashboardSummary(
        openTickets = 3,
        inProgressTickets = 1,
        pendingClientTickets = 1,
        resolvedToday = 1,
        activeClients = clients.size,
    )

    fun defaultDevice(userId: String = adminUser.id): NotificationDevice = NotificationDevice(
        id = "device-1",
        userId = userId,
        platform = "ANDROID",
        token = "placeholder-device-token",
        lastSeenAt = "2026-03-19T10:00:00Z",
    )
}
