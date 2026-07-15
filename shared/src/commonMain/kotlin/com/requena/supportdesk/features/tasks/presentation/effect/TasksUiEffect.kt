package com.requena.supportdesk.features.tasks.presentation.effect

sealed interface TasksUiEffect {
    data class ShowMessage(val message: String) : TasksUiEffect
}
