package com.requena.supportdesk.features.taskboard.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskBoardPlacementDto(
    val taskId: String,
    val boardDate: String,
    val positionX: Double,
    val positionY: Double,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TaskBoardSnapshotDto(
    val today: String,
    val timeZone: String,
    val refreshAfterSeconds: Long = 300,
    val placements: List<TaskBoardPlacementDto>,
)

@Serializable
data class UpdateTaskBoardPlacementRequestDto(
    val boardDate: String,
    val positionX: Double,
    val positionY: Double,
)
