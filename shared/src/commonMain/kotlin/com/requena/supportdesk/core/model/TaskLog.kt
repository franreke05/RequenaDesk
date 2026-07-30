package com.requena.supportdesk.core.model

data class TaskLog(
    val id: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val seconds: Int = minutes * 60,
    // Null only for the migrated "historic time without a daily breakdown" bucket:
    // a real, immutable duration with no invented date, excluded from the calendar.
    val workDate: String? = null,
    val note: String,
    val billable: Boolean,
    val createdAt: String,
    val sessionId: String? = null,
    val isHistoricMigrated: Boolean = false,
)
