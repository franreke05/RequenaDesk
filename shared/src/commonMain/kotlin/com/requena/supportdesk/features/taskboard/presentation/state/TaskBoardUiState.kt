package com.requena.supportdesk.features.taskboard.presentation.state

import com.requena.supportdesk.core.model.TaskBoardPlacement

data class TaskBoardUiState(
    val today: String = "",
    val timeZone: String = "Europe/Madrid",
    val selectedDate: String = "",
    val placements: List<TaskBoardPlacement> = emptyList(),
    val savingTaskIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedDayPlacements: List<TaskBoardPlacement>
        get() = placements.filter { it.boardDate == selectedDate }

    val placedTaskIds: Set<String>
        get() = placements.mapTo(mutableSetOf()) { it.taskId }

    fun placementFor(taskId: String): TaskBoardPlacement? =
        placements.firstOrNull { it.taskId == taskId }

    fun countFor(date: String): Int = placements.count { it.boardDate == date }
}
