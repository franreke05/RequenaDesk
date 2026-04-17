package com.requena.supportdesk.features.tasks.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskLabelDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val tasksCount: Int = 0,
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String = "",
    val clientId: String? = null,
    val clientName: String? = null,
    val labelId: String,
    val labelName: String,
    val labelColorHex: String,
    val completed: Boolean,
    val loggedMinutes: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TaskLogDto(
    val id: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val workDate: String,
    val note: String = "",
    val billable: Boolean,
    val createdAt: String,
)

@Serializable
data class CreateTaskRequestDto(
    val title: String,
    val description: String,
    val clientId: String? = null,
    val labelId: String,
)

@Serializable
data class UpdateTaskRequestDto(
    val title: String? = null,
    val description: String? = null,
    val clientId: String? = null,
    val labelId: String? = null,
    val completed: Boolean? = null,
)

@Serializable
data class CreateTaskLabelRequestDto(
    val name: String,
    val colorHex: String,
)

@Serializable
data class UpdateTaskLabelRequestDto(
    val name: String? = null,
    val colorHex: String? = null,
)

@Serializable
data class CreateTimeLogRequestDto(
    val taskId: String,
    val authorId: String,
    val workDate: String,
    val minutes: Int,
    val note: String,
    val billable: Boolean,
)
