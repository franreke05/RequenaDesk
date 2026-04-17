package com.requena.supportdesk.features.tasks.domain.model

data class TaskDraft(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
)

data class TaskUpdateInput(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
    val completed: Boolean,
)

data class TaskLabelDraft(
    val name: String,
    val colorHex: String,
)

data class TaskTimeLogDraft(
    val taskId: String,
    val authorId: String,
    val workDate: String,
    val minutes: Int,
    val note: String,
    val billable: Boolean,
)
