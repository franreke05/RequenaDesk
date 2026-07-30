package com.requena.supportdesk.features.taskboard.domain.repository

import com.requena.supportdesk.core.model.TaskBoardPlacement
import com.requena.supportdesk.core.model.TaskBoardSnapshot
import com.requena.supportdesk.core.result.AppResult

interface TaskBoardRepository {
    suspend fun getBoard(): AppResult<TaskBoardSnapshot>
    suspend fun upsertPlacement(
        taskId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): AppResult<TaskBoardPlacement>
    suspend fun deletePlacement(taskId: String): AppResult<Unit>
}
