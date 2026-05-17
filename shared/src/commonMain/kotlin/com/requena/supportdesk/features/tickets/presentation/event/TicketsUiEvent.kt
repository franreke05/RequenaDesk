package com.requena.supportdesk.features.tickets.presentation.event

import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn

import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput

sealed interface TicketsUiEvent {
    object Load : TicketsUiEvent
    data class SearchChanged(val query: String) : TicketsUiEvent
    data class StatusFilterChanged(val status: TicketStatus?) : TicketsUiEvent
    data class PriorityFilterChanged(val priority: TicketPriority?) : TicketsUiEvent
    data class CategoryFilterChanged(val category: TicketCategory?) : TicketsUiEvent
    data class PlatformFilterChanged(val platform: SupportPlatform?) : TicketsUiEvent
    data class WaitingOnFilterChanged(val waitingOn: WaitingOn?) : TicketsUiEvent
    data class SelectTicket(val ticketId: String) : TicketsUiEvent
    data class CreateTicket(val input: CreateTicketInput) : TicketsUiEvent
    object CreateSampleTicket : TicketsUiEvent
    data class ReplyToSelected(val message: String) : TicketsUiEvent
    data class ChangeSelectedStatus(val status: TicketStatus) : TicketsUiEvent
    data class ChangeSelectedPriority(val priority: TicketPriority) : TicketsUiEvent
    data class AcceptSelectedClose(val resolutionSummary: String? = null) : TicketsUiEvent
    data class RateSelected(val rating: Int) : TicketsUiEvent
    data class AddInternalNote(
        val body: String,
        val authorId: String,
        val authorName: String,
    ) : TicketsUiEvent
    data class AddTimeEntry(
        val minutes: Int,
        val note: String,
        val billable: Boolean,
        val authorId: String,
        val authorName: String,
    ) : TicketsUiEvent
}
