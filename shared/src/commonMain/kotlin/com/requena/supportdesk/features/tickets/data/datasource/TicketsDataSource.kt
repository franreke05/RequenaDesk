package com.requena.supportdesk.features.tickets.data.datasource

import com.requena.supportdesk.features.tickets.data.dto.TicketDto

interface TicketsDataSource {
    suspend fun getTickets(): List<TicketDto>
    suspend fun getTicket(id: String): TicketDto?
}
