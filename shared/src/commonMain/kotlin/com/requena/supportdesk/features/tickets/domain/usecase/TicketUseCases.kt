package com.requena.supportdesk.features.tickets.domain.usecase

import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters
import com.requena.supportdesk.features.tickets.domain.repository.TicketsRepository

class GetTicketsUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(filters: TicketFilters = TicketFilters()) = repository.getTickets(filters)
}

class GetTicketUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(id: String) = repository.getTicket(id)
}

class CreateTicketUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(input: CreateTicketInput) = repository.createTicket(input)
}

class ReplyTicketUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(ticketId: String, message: String) = repository.replyTicket(ticketId, message)
}

class ChangeTicketStatusUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(ticketId: String, status: TicketStatus) = repository.changeStatus(ticketId, status)
}

class ChangeTicketPriorityUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(ticketId: String, priority: TicketPriority) = repository.changePriority(ticketId, priority)
}

class AcceptTicketCloseUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(ticketId: String, resolutionSummary: String? = null) =
        repository.acceptClose(ticketId, resolutionSummary)
}

class RateTicketUseCase(
    private val repository: TicketsRepository,
) {
    suspend operator fun invoke(ticketId: String, rating: Int) = repository.rateTicket(ticketId, rating)
}
