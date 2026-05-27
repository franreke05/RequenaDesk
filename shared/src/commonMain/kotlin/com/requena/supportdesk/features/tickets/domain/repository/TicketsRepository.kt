package com.requena.supportdesk.features.tickets.domain.repository

import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.domain.model.TicketFilters

interface TicketsRepository {
    suspend fun getTickets(filters: TicketFilters = TicketFilters()): AppResult<List<Ticket>>
    suspend fun getTicket(id: String): AppResult<Ticket>
    suspend fun createTicket(input: CreateTicketInput): AppResult<Ticket>
    suspend fun changeStatus(ticketId: String, status: TicketStatus): AppResult<Ticket>
    suspend fun changePriority(ticketId: String, priority: TicketPriority): AppResult<Ticket>
    suspend fun acceptClose(ticketId: String, resolutionSummary: String? = null): AppResult<Ticket>
    suspend fun rateTicket(ticketId: String, rating: Int): AppResult<Ticket>
    suspend fun deleteTicket(ticketId: String): AppResult<Unit>
}
