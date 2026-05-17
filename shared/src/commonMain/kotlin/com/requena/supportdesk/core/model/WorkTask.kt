package com.requena.supportdesk.core.model

data class WorkTask(
    val id: String,
    val title: String,
    val clientId: String? = null,
    val categoryId: String,
    val description: String = "",
    val dueDate: String? = null,
    val completed: Boolean = false,
    val status: WorkTaskStatus = if (completed) WorkTaskStatus.DONE else WorkTaskStatus.TODO,
    val loggedMinutes: Int = 0,
    val loggedSeconds: Int = loggedMinutes * 60,
    val createdAt: String,
    val updatedAt: String,
)
