package com.requena.supportdesk.core.model

data class InternalComment(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)
