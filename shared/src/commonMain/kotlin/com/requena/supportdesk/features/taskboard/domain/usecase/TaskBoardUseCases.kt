package com.requena.supportdesk.features.taskboard.domain.usecase

import com.requena.supportdesk.features.taskboard.domain.repository.TaskBoardRepository

class GetTaskBoardUseCase(private val repository: TaskBoardRepository) {
    suspend operator fun invoke() = repository.getBoard()
}

class PlaceTaskOnBoardUseCase(private val repository: TaskBoardRepository) {
    suspend operator fun invoke(
        taskId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ) = repository.upsertPlacement(taskId, boardDate, positionX, positionY)
}

class RemoveTaskFromBoardUseCase(private val repository: TaskBoardRepository) {
    suspend operator fun invoke(taskId: String) = repository.deletePlacement(taskId)
}
