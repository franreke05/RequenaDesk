package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.data.entity.AttachmentEntity
import com.requena.supportdesk.server.data.entity.ClientEntity
import com.requena.supportdesk.server.data.entity.DashboardEntity
import com.requena.supportdesk.server.data.entity.DeviceEntity
import com.requena.supportdesk.server.data.entity.TicketEntity

class InMemorySupportDeskDataSource {
    fun tickets(): List<TicketEntity> = listOf(
        TicketEntity(
            id = "ticket-1",
            clientId = "client-1",
            ticketNumber = "SD-1001",
            subject = "Desktop app no arranca",
            description = "Error critico al iniciar la ultima build enviada.",
            category = "BUG",
            affectedApp = "Northwind Desk",
            platform = "DESKTOP",
            appVersion = "1.8.2 (418)",
            stepsToReproduce = "Launch the desktop application after installing build 418.",
            clientReference = "NW-REL-418",
            status = "IN_PROGRESS",
            priority = "HIGH",
            waitingOn = "ADMIN",
            requesterId = "user-client",
            requesterName = "Ana Northwind",
            requesterEmail = "ana@northwind.dev",
            assigneeId = "user-admin",
            assigneeName = "Administrador",
            assigneeEmail = "admin@orykai.local",
            createdAt = "2026-07-14T08:00:00Z",
            updatedAt = "2026-07-14T09:30:00Z",
        ),
        TicketEntity(
            id = "ticket-2",
            clientId = "client-2",
            ticketNumber = "SD-1002",
            subject = "Anadir campo de referencia",
            description = "Necesitamos un campo extra para enlazar pedidos con tickets.",
            category = "CHANGE_REQUEST",
            affectedApp = "Forge Flow",
            platform = "WEB",
            appVersion = "2.4.0",
            stepsToReproduce = null,
            clientReference = "PF-Q2-REQ-12",
            status = "PENDING_CLIENT",
            priority = "MEDIUM",
            waitingOn = "CLIENT",
            resolutionSummary = "Pending client confirmation about the exact reference format.",
            requesterId = "user-client-2",
            requesterName = "Contacto Forge Flow",
            requesterEmail = "support@forgeflow.dev",
            assigneeId = "user-admin",
            assigneeName = "Administrador",
            assigneeEmail = "admin@orykai.local",
            createdAt = "2026-07-13T11:00:00Z",
            updatedAt = "2026-07-14T07:45:00Z",
        ),
    )

    fun ticket(id: String): TicketEntity? = tickets().firstOrNull { it.id == id }

    fun clients(): List<ClientEntity> = listOf(
        ClientEntity(
            id = "client-1",
            ownerAdminId = "user-admin",
            companyName = "Northwind Studio",
            productName = "Northwind Desk",
            email = "ana@northwind.dev",
            accountStatus = "ACTIVE",
            serviceTier = "PRIORITY",
            preferredContactChannel = "TICKET",
            activeTicketCount = 2,
        ),
        ClientEntity(
            id = "client-2",
            ownerAdminId = "user-admin-2",
            companyName = "Pixel Forge",
            productName = "Forge Flow",
            email = "david@pixelforge.dev",
            accountStatus = "ACTIVE",
            serviceTier = "STANDARD",
            preferredContactChannel = "EMAIL",
            activeTicketCount = 1,
        ),
    )

    fun dashboard(): DashboardEntity = DashboardEntity(
        openTickets = 3,
        pendingClientTickets = 1,
        resolvedToday = 1,
        activeClients = 2,
    )

    fun attachment(id: String): AttachmentEntity? = AttachmentEntity(
        id = id,
        fileName = "placeholder.txt",
        contentType = "text/plain",
    )

    fun registerDevice(): DeviceEntity = DeviceEntity(
        id = "device-1",
        userId = "user-admin",
        platform = "ANDROID",
    )
}
