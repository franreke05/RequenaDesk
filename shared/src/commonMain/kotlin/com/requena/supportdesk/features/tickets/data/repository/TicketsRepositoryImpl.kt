package com.requena.supportdesk.features.tickets.data.repository

import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tickets.data.datasource.TicketsDataSource
import com.requena.supportdesk.features.tickets.data.dto.CreateTicketRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketCloseAcceptanceRequestDto
import com.requena.supportdesk.features.tickets.data.dto.TicketSatisfactionRequestDto
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
            .map(TicketsMapper::fromDto)
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

    override suspend fun changeStatus(ticketId: String, status: TicketStatus): AppResult<Ticket> = runCatching {
        dataSource.changeStatus(ticketId, UpdateTicketStatusRequestDto(status.name))
        dataSource.getTicket(ticketId)?.let(TicketsMapper::fromDto)
            ?: error("Ticket no encontrado.")
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cambiar el estado.", cause = it) },
    )

    override suspend fun changePriority(ticketId: String, priority: TicketPriority): AppResult<Ticket> = runCatching {
        dataSource.changePriority(ticketId, UpdateTicketPriorityRequestDto(priority.name))
        dataSource.getTicket(ticketId)?.let(TicketsMapper::fromDto)
            ?: error("Ticket no encontrado.")
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo cambiar la prioridad.", cause = it) },
    )

    override suspend fun acceptClose(ticketId: String, resolutionSummary: String?): AppResult<Ticket> = runCatching {
        dataSource.acceptClose(ticketId, TicketCloseAcceptanceRequestDto(resolutionSummary)).let(TicketsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo aceptar el cierre.", cause = it) },
    )

    override suspend fun rateTicket(ticketId: String, rating: Int): AppResult<Ticket> = runCatching {
        dataSource.rateTicket(ticketId, TicketSatisfactionRequestDto(rating)).let(TicketsMapper::fromDto)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo guardar la satisfaccion.", cause = it) },
    )

    override suspend fun deleteTicket(ticketId: String): AppResult<Unit> = runCatching {
        dataSource.deleteTicket(ticketId)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(message = it.message ?: "No se pudo eliminar el ticket.", cause = it) },
    )
}
