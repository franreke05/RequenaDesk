package com.requena.supportdesk.core.model

data class TaskBoardPlacement(
    val taskId: String,
    val boardDate: String,
    val positionX: Double,
    val positionY: Double,
    val createdAt: String,
    val updatedAt: String,
)

data class TaskBoardSnapshot(
    val today: String,
    val timeZone: String,
    val refreshAfterSeconds: Long,
    val placements: List<TaskBoardPlacement>,
)
