package com.requena.supportdesk.features.tickets.presentation.effect

sealed interface TicketsUiEffect {
    data class ShowMessage(val message: String) : TicketsUiEffect
    data class TicketSelected(val ticketId: String) : TicketsUiEffect
}
