package com.requena.supportdesk.core.model

data class TaskLog(
    val id: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val workDate: String,
    val note: String,
    val billable: Boolean,
    val createdAt: String,
)
