package com.requena.supportdesk.features.programs.presentation.effect

sealed interface ProgramsUiEffect {
    data class ShowMessage(val message: String) : ProgramsUiEffect
}
