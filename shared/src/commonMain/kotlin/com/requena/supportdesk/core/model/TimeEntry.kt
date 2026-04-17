package com.requena.supportdesk.core.model

data class TimeEntry(
    val id: String,
    val clientId: String,
    val ticketId: String,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val workDate: String,
    val note: String,
    val billable: Boolean,
    val createdAt: String,
)
