package com.requena.supportdesk.core.model

data class ClientNote(
    val id: String,
    val clientId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAt: String,
)
