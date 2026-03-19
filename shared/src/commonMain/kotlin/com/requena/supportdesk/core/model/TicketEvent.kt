package com.requena.supportdesk.core.model

data class TicketEvent(
    val id: String,
    val ticketId: String,
    val type: String,
    val description: String,
    val actorName: String,
    val createdAt: String,
)
