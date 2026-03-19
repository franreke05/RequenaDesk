package com.requena.supportdesk.features.tickets.data.mapper

import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.features.tickets.data.dto.TicketDto

object TicketsMapper {
    fun fromDto(dto: TicketDto): Ticket {
        val requester = SupportDeskSeed.clients.firstOrNull { it.id == dto.clientId }?.let {
            SupportDeskSeed.clientUser.copy(clientId = it.id)
        } ?: SupportDeskSeed.clientUser

        return Ticket(
            id = dto.id,
            clientId = dto.clientId,
            ticketNumber = dto.ticketNumber,
            subject = dto.subject,
            description = dto.description,
            category = TicketCategory.valueOf(dto.category),
            affectedApp = dto.affectedApp,
            platform = SupportPlatform.valueOf(dto.platform),
            appVersion = dto.appVersion,
            stepsToReproduce = dto.stepsToReproduce,
            clientReference = dto.clientReference,
            status = TicketStatus.valueOf(dto.status),
            priority = TicketPriority.valueOf(dto.priority),
            waitingOn = WaitingOn.valueOf(dto.waitingOn),
            resolutionSummary = dto.resolutionSummary,
            requester = requester,
            assignee = SupportDeskSeed.adminUser,
            createdAt = "2026-03-19T00:00:00Z",
            updatedAt = "2026-03-19T00:00:00Z",
        )
    }
}
