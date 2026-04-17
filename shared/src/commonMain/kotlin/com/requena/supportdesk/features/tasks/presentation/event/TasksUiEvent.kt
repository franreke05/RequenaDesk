package com.requena.supportdesk.features.tasks.presentation.event

sealed interface TasksUiEvent {
    object Load : TasksUiEvent
    data class SelectDay(val dayIsoDate: String) : TasksUiEvent
    data class SelectTask(val taskId: String?) : TasksUiEvent
    data class SelectCategory(val categoryId: String?) : TasksUiEvent
    data class SelectClientFilter(val clientId: String?) : TasksUiEvent
    data class SelectDashboardClient(val clientId: String?) : TasksUiEvent
    data class CreateCategory(val name: String, val colorHex: String) : TasksUiEvent
    data class UpdateLabel(val labelId: String, val name: String, val colorHex: String) : TasksUiEvent
    data class DeleteLabel(val labelId: String) : TasksUiEvent
    data class CreateTask(
        val title: String,
        val description: String,
        val clientId: String?,
        val categoryId: String,
    ) : TasksUiEvent
    data class UpdateTaskClient(val taskId: String, val clientId: String?) : TasksUiEvent
    data class UpdateTask(
        val taskId: String,
        val title: String,
        val description: String,
        val categoryId: String,
    ) : TasksUiEvent
    data class DeleteTask(val taskId: String) : TasksUiEvent
    data class ToggleTaskCompletion(val taskId: String) : TasksUiEvent
    data class StartTimer(val taskId: String) : TasksUiEvent
    object PauseTimer : TasksUiEvent
    object StopTimer : TasksUiEvent
}
