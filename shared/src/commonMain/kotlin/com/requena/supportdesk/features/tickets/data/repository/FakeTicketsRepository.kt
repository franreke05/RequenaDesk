package com.requena.supportdesk.features.tickets.data.repository

import com.requena.supportdesk.core.common.SupportDeskSeed
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketEvent
import com.requena.supportdesk.core.model.TicketMessage
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.core.utils.matchesQuery
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters
import com.requena.supportdesk.features.tickets.domain.repository.TicketsRepository

class FakeTicketsRepository : TicketsRepository {
    private var tickets = SupportDeskSeed.tickets().toMutableList()

    override suspend fun getTickets(filters: TicketFilters): AppResult<List<Ticket>> {
        val filtered = tickets.filter { ticket ->
            val queryMatches = ticket.subject.matchesQuery(filters.query) ||
                ticket.description.matchesQuery(filters.query) ||
                ticket.ticketNumber.matchesQuery(filters.query) ||
                ticket.affectedApp.matchesQuery(filters.query) ||
                (ticket.clientReference ?: "").matchesQuery(filters.query) ||
                ticket.requester.name.matchesQuery(filters.query)
            val statusMatches = filters.status == null || ticket.status == filters.status
            val priorityMatches = filters.priority == null || ticket.priority == filters.priority
            val categoryMatches = filters.category == null || ticket.category == filters.category
            val platformMatches = filters.platform == null || ticket.platform == filters.platform
            val waitingOnMatches = filters.waitingOn == null || ticket.waitingOn == filters.waitingOn
            queryMatches && statusMatches && priorityMatches && categoryMatches && platformMatches && waitingOnMatches
        }
        return AppResult.Success(filtered)
    }

    override suspend fun getTicket(id: String): AppResult<Ticket> {
        return tickets.firstOrNull { it.id == id }?.let { AppResult.Success(it) }
            ?: AppResult.Error("Ticket $id no encontrado.")
    }

    override suspend fun createTicket(input: CreateTicketInput): AppResult<Ticket> {
        val newTicket = Ticket(
            id = "ticket-${tickets.size + 1}",
            clientId = input.clientId.ifBlank { SupportDeskSeed.clientUser.clientId ?: "client-1" },
            ticketNumber = "SD-${1000 + tickets.size + 1}",
            subject = input.subject.ifBlank { "Nuevo ticket de muestra" },
            description = input.description.ifBlank { "Placeholder para continuar el desarrollo del CRM." },
            category = input.category,
            affectedApp = input.affectedApp.ifBlank { "Northwind Desk" },
            platform = input.platform,
            appVersion = input.appVersion.ifBlank { null },
            stepsToReproduce = input.stepsToReproduce.ifBlank { null },
            clientReference = input.clientReference.ifBlank { null },
            status = TicketStatus.OPEN,
            priority = input.priority,
            waitingOn = WaitingOn.ADMIN,
            requester = SupportDeskSeed.clientUser,
            assignee = SupportDeskSeed.adminUser,
            createdAt = "2026-03-19T10:30:00Z",
            updatedAt = "2026-03-19T10:30:00Z",
            events = listOf(
                TicketEvent(
                    id = "event-create-${tickets.size + 1}",
                    ticketId = "ticket-${tickets.size + 1}",
                    type = "CREATED",
                    description = "Ticket creado desde placeholder del MVP.",
                    actorName = SupportDeskSeed.clientUser.name,
                    createdAt = "2026-03-19T10:30:00Z",
                ),
            ),
        )

        tickets = (listOf(newTicket) + tickets).toMutableList()
        return AppResult.Success(newTicket)
    }

    override suspend fun replyTicket(ticketId: String, message: String): AppResult<TicketMessage> {
        val ticket = tickets.firstOrNull { it.id == ticketId } ?: return AppResult.Error("Ticket $ticketId no encontrado.")
        val reply = TicketMessage(
            id = "message-${ticket.messages.size + 1}-${ticket.id}",
            ticketId = ticket.id,
            authorId = SupportDeskSeed.adminUser.id,
            authorName = SupportDeskSeed.adminUser.name,
            body = message.ifBlank { "Respuesta placeholder desde el skeleton del CRM." },
            createdAt = "2026-03-19T10:45:00Z",
        )
        updateTicket(ticketId) {
            it.copy(
                updatedAt = "2026-03-19T10:45:00Z",
                waitingOn = WaitingOn.CLIENT,
                messages = it.messages + reply,
            )
        }
        return AppResult.Success(reply)
    }

    override suspend fun changeStatus(ticketId: String, status: TicketStatus): AppResult<Ticket> {
        val updated = updateTicket(ticketId) {
            it.copy(
                status = status,
                waitingOn = if (status == TicketStatus.PENDING_CLIENT) WaitingOn.CLIENT else WaitingOn.ADMIN,
                resolutionSummary = if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
                    "Issue addressed in placeholder workflow."
                } else {
                    it.resolutionSummary
                },
                updatedAt = "2026-03-19T10:50:00Z",
                events = it.events + TicketEvent(
                    id = "event-status-${it.events.size + 1}",
                    ticketId = it.id,
                    type = "STATUS_CHANGED",
                    description = "Estado cambiado a $status",
                    actorName = SupportDeskSeed.adminUser.name,
                    createdAt = "2026-03-19T10:50:00Z",
                ),
            )
        }
        return updated?.let { AppResult.Success(it) } ?: AppResult.Error("Ticket $ticketId no encontrado.")
    }

    override suspend fun changePriority(ticketId: String, priority: TicketPriority): AppResult<Ticket> {
        val updated = updateTicket(ticketId) {
            it.copy(
                priority = priority,
                updatedAt = "2026-03-19T10:55:00Z",
                events = it.events + TicketEvent(
                    id = "event-priority-${it.events.size + 1}",
                    ticketId = it.id,
                    type = "PRIORITY_CHANGED",
                    description = "Prioridad cambiada a $priority",
                    actorName = SupportDeskSeed.adminUser.name,
                    createdAt = "2026-03-19T10:55:00Z",
                ),
            )
        }
        return updated?.let { AppResult.Success(it) } ?: AppResult.Error("Ticket $ticketId no encontrado.")
    }

    private fun updateTicket(ticketId: String, transform: (Ticket) -> Ticket): Ticket? {
        var updatedTicket: Ticket? = null
        tickets = tickets.map { current ->
            if (current.id == ticketId) {
                transform(current).also { updatedTicket = it }
            } else {
                current
            }
        }.toMutableList()
        return updatedTicket
    }
}
