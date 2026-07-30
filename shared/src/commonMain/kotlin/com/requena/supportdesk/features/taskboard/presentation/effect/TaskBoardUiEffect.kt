package com.requena.supportdesk.features.taskboard.presentation.effect

sealed interface TaskBoardUiEffect {
    data class ShowMessage(val message: String) : TaskBoardUiEffect
}
