package com.requena.supportdesk.core.model

data class TicketMessage(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
    val attachments: List<Attachment> = emptyList(),
)
