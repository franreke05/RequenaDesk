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
            ticketNumber = "SD-1001",
            subject = "Desktop app no arranca",
            description = "Error critico al iniciar la ultima build enviada.",
            category = "BUG",
            affectedApp = "Northwind Desk",
            platform = "DESKTOP",
            appVersion = "1.8.2 (418)",
            clientReference = "NW-REL-418",
            status = "IN_PROGRESS",
            priority = "HIGH",
            waitingOn = "ADMIN",
        ),
        TicketEntity(
            id = "ticket-2",
            ticketNumber = "SD-1002",
            subject = "Anadir campo de referencia",
            description = "Necesitamos un campo extra para enlazar pedidos con tickets.",
            category = "CHANGE_REQUEST",
            affectedApp = "Forge Flow",
            platform = "WEB",
            appVersion = "2.4.0",
            clientReference = "PF-Q2-REQ-12",
            status = "PENDING_CLIENT",
            priority = "MEDIUM",
            waitingOn = "CLIENT",
            resolutionSummary = "Pending client confirmation about the exact reference format.",
        ),
    )

    fun ticket(id: String): TicketEntity? = tickets().firstOrNull { it.id == id }

    fun clients(): List<ClientEntity> = listOf(
        ClientEntity(
            id = "client-1",
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
