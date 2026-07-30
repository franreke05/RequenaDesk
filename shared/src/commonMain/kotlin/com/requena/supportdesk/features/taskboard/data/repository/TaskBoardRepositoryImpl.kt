package com.requena.supportdesk.features.taskboard.data.repository

import com.requena.supportdesk.core.model.TaskBoardPlacement
import com.requena.supportdesk.core.model.TaskBoardSnapshot
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.taskboard.data.datasource.TaskBoardDataSource
import com.requena.supportdesk.features.taskboard.data.dto.TaskBoardPlacementDto
import com.requena.supportdesk.features.taskboard.data.dto.UpdateTaskBoardPlacementRequestDto
import com.requena.supportdesk.features.taskboard.domain.repository.TaskBoardRepository

class TaskBoardRepositoryImpl(
    private val dataSource: TaskBoardDataSource,
) : TaskBoardRepository {
    override suspend fun getBoard(): AppResult<TaskBoardSnapshot> = runCatching {
        val dto = dataSource.getBoard()
        TaskBoardSnapshot(
            today = dto.today,
            timeZone = dto.timeZone,
            refreshAfterSeconds = dto.refreshAfterSeconds,
            placements = dto.placements.map(::mapPlacement),
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = {
            AppResult.Error(
                message = it.message ?: "No se pudo cargar el tablero.",
                cause = it,
            )
        },
    )

    override suspend fun upsertPlacement(
        taskId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): AppResult<TaskBoardPlacement> = runCatching {
        dataSource.upsertPlacement(
            taskId = taskId,
            request = UpdateTaskBoardPlacementRequestDto(
                boardDate = boardDate,
                positionX = positionX,
                positionY = positionY,
            ),
        ).let(::mapPlacement)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = {
            AppResult.Error(
                message = it.message ?: "No se pudo guardar la posición del pósit.",
                cause = it,
            )
        },
    )

    override suspend fun deletePlacement(taskId: String): AppResult<Unit> = runCatching {
        dataSource.deletePlacement(taskId)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = {
            AppResult.Error(
                message = it.message ?: "No se pudo retirar la tarea del tablero.",
                cause = it,
            )
        },
    )

    private fun mapPlacement(dto: TaskBoardPlacementDto) = TaskBoardPlacement(
        taskId = dto.taskId,
        boardDate = dto.boardDate,
        positionX = dto.positionX.coerceIn(0.0, 1.0),
        positionY = dto.positionY.coerceIn(0.0, 1.0),
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
    )
}
