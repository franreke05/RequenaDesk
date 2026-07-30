package com.requena.supportdesk.features.taskboard.presentation.event

sealed interface TaskBoardUiEvent {
    data object Load : TaskBoardUiEvent
    data object Refresh : TaskBoardUiEvent
    data object SelectToday : TaskBoardUiEvent
    data class SelectDate(val date: String) : TaskBoardUiEvent
    data class PlaceTask(
        val taskId: String,
        val boardDate: String,
        val positionX: Double,
        val positionY: Double,
    ) : TaskBoardUiEvent
    data class MoveTask(
        val taskId: String,
        val positionX: Double,
        val positionY: Double,
    ) : TaskBoardUiEvent
    data class RemoveTask(val taskId: String) : TaskBoardUiEvent
}
