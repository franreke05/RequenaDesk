package com.requena.supportdesk.features.tasks.domain.model

data class TaskDraft(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
    val dueDate: String? = null,
)

data class TaskUpdateInput(
    val title: String,
    val description: String,
    val clientId: String?,
    val categoryId: String,
    val dueDate: String? = null,
    val completed: Boolean,
    val status: String? = null,
)

data class TaskLabelDraft(
    val name: String,
    val colorHex: String,
    val ownerAdminId: String,
)

data class TaskTimeLogDraft(
    val taskId: String,
    val authorId: String,
    val workDate: String,
    val minutes: Int,
    val seconds: Int = minutes * 60,
    val note: String,
    val billable: Boolean,
)
