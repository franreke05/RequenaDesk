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
)

data class TaskLabelDraft(
    val name: String,
    val colorHex: String,
    val ownerAdminId: String,
)
