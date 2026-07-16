package com.requena.supportdesk.features.tickets.data.repository

import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.network.AdminSessionContext
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tickets.data.datasource.TicketsDataSource
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketMessageRequestDto
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketPriorityRequestDto
import com.requena.supportdesk.features.tickets.data.dto.UpdateTicketStatusRequestDto
import com.requena.supportdesk.features.tickets.data.mapper.TicketsMapper
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters
import com.requena.supportdesk.features.tickets.domain.repository.TicketsRepository

class TicketsRepositoryImpl(
    private val dataSource: TicketsDataSource,
) : TicketsRepository {
    override suspend fun getTickets(filters: TicketFilters): AppResult<List<Ticket>> = runCatching {
        dataSource.getTickets()
            .mapNotNull { dto ->
                runCatching { TicketsMapper.fromDto(dto) }
                    .onFailure { println("[Tickets] Ignorando ticket ${dto.id} (${dto.ticketNumber}): ${it.message}") }
                    .getOrNull()
            }
            .filter { ticket ->
                (filters.query.isBlank() || listOf(ticket.subject, ticket.ticketNumber, ticket.affectedApp, ticket.clientReference.orEmpty())
                    .any { it.contains(filters.query, ignoreCase = true) }) &&
                    (filters.status == null || ticket.status == filters.status) &&
                    (filters.priority == null || ticket.priority == filters.priority) &&
                    (filters.category == null || ticket.category == filters.category) &&
                    (filters.platform == null || ticket.platform == filters.platform) &&
                    (filters.waitingOn == null || ticket.waitingOn == filters.waitingOn)
            }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudieron cargar los tickets.", cause = it) },
    )

    override suspend fun getTicket(id: String): AppResult<Ticket> = runCatching {
        dataSource.getTicket(id)?.let(TicketsMapper::fromDto)
            ?: error("Ticket no encontrado.")
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cargar el ticket.", cause = it) },
    )

    override suspend fun createTicket(input: CreateTicketInput): AppResult<Ticket> = runCatching {
        dataSource.createTicket(
            CreateTicketRequestDto(
                clientId = input.clientId,
                subject = input.subject,
                description = input.description,
                category = input.category.name,
                affectedApp = input.affectedApp,
                platform = input.platform.name,
                appVersion = input.appVersion.ifBlank { null },
                stepsToReproduce = input.stepsToReproduce.ifBlank { null },
                clientReference = input.clientReference.ifBlank { null },
                priority = input.priority.name,
            ),
        ).let(TicketsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo crear el ticket.", cause = it) },
    )

    override suspend fun replyTicket(ticketId: String, message: String): AppResult<Unit> = runCatching {
        val authorId = AdminSessionContext.currentUserId() ?: "unknown-user"
        dataSource.replyTicket(
            ticketId = ticketId,
            request = CreateTicketMessageRequestDto(
                authorId = authorId,
                body = message,
            ),
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo enviar la respuesta.", cause = it) },
    )

    override suspend fun changeStatus(ticketId: String, status: TicketStatus, currentSnapshot: Ticket): AppResult<Ticket> {
        val patch = runCatching { dataSource.changeStatus(ticketId, UpdateTicketStatusRequestDto(status.name)) }
        val patchFailure = patch.exceptionOrNull()
        if (patchFailure != null) {
            return AppResult.Error(message = patchFailure.message ?: "No se pudo cambiar el estado.", cause = patchFailure)
        }
        // The PATCH itself already succeeded server-side at this point. A failure of this follow-up
        // GET must not be reported as "status change failed" - fall back to a local approximation
        // of the server's derived fields instead of losing the confirmed update.
        val refreshed = runCatching { dataSource.getTicket(ticketId)?.let(TicketsMapper::fromDto) }.getOrNull()
        val ticket = refreshed ?: currentSnapshot.copy(
            status = status,
            waitingOn = if (status == TicketStatus.PENDING_CLIENT) WaitingOn.CLIENT else WaitingOn.ADMIN,
        )
        return AppResult.Success(ticket)
    }

    override suspend fun changePriority(ticketId: String, priority: TicketPriority, currentSnapshot: Ticket): AppResult<Ticket> {
        val patch = runCatching { dataSource.changePriority(ticketId, UpdateTicketPriorityRequestDto(priority.name)) }
        val patchFailure = patch.exceptionOrNull()
        if (patchFailure != null) {
            return AppResult.Error(message = patchFailure.message ?: "No se pudo cambiar la prioridad.", cause = patchFailure)
        }
        val refreshed = runCatching { dataSource.getTicket(ticketId)?.let(TicketsMapper::fromDto) }.getOrNull()
        val ticket = refreshed ?: currentSnapshot.copy(priority = priority)
        return AppResult.Success(ticket)
    }
}
