package com.requena.supportdesk.features.tickets.presentation.state

import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn

data class TicketsUiState(
    val tickets: List<Ticket> = emptyList(),
    val allTickets: List<Ticket> = emptyList(),
    val selectedTicket: Ticket? = null,
    val searchQuery: String = "",
    val statusFilter: TicketStatus? = null,
    val priorityFilter: TicketPriority? = null,
    val categoryFilter: TicketCategory? = null,
    val platformFilter: SupportPlatform? = null,
    val waitingOnFilter: WaitingOn? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
