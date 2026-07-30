package com.requena.supportdesk.features.tasks.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskLabelDto(
    val id: String,
    val ownerAdminId: String,
    val name: String,
    val colorHex: String,
    val tasksCount: Int = 0,
)

@Serializable
data class TaskDto(
    val id: String,
    val ownerAdminId: String,
    val createdByUserId: String = "",
    val createdByUserName: String = "",
    val title: String,
    val description: String = "",
    val clientId: String? = null,
    val clientName: String? = null,
    val labelId: String,
    val labelName: String,
    val labelColorHex: String,
    val dueDate: String? = null,
    val completed: Boolean,
    val loggedMinutes: Int,
    val loggedSeconds: Int = loggedMinutes * 60,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TaskLogDto(
    val id: String,
    val ownerAdminId: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val authorName: String,
    val minutes: Int,
    val seconds: Int = minutes * 60,
    val workDate: String? = null,
    val note: String = "",
    val billable: Boolean,
    val createdAt: String,
    val sessionId: String? = null,
    val isHistoricMigrated: Boolean = false,
)

@Serializable
data class CreateTaskRequestDto(
    val title: String,
    val description: String,
    val clientId: String? = null,
    val labelId: String,
    val dueDate: String? = null,
)

@Serializable
data class UpdateTaskRequestDto(
    val title: String? = null,
    val description: String? = null,
    val clientId: String? = null,
    val labelId: String? = null,
    val dueDate: String? = null,
    val completed: Boolean? = null,
)

@Serializable
data class CreateTaskLabelRequestDto(
    val name: String,
    val colorHex: String,
    val ownerAdminId: String,
)

@Serializable
data class UpdateTaskLabelRequestDto(
    val name: String? = null,
    val colorHex: String? = null,
)

@Serializable
data class WorkSegmentDto(
    val id: String,
    val sessionId: String,
    val startedAt: String,
    val endedAt: String? = null,
)

@Serializable
data class WorkSessionDto(
    val id: String,
    val ownerAdminId: String,
    val taskId: String,
    val clientId: String? = null,
    val authorId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val status: String,
    val billable: Boolean,
    val timeSource: String,
    val businessTimezone: String,
    val lastCheckpointAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val segments: List<WorkSegmentDto> = emptyList(),
    val closedSegmentsSeconds: Long = 0,
    val openSegmentStartedAt: String? = null,
)

@Serializable
data class StartWorkSessionRequestDto(
    val taskId: String,
)
