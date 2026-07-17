package com.requena.supportdesk.features.programs.presentation.event

sealed interface ProgramsUiEvent {
    data object RefreshClientPrograms : ProgramsUiEvent
    data class ToggleProgramSelection(val productKey: String) : ProgramsUiEvent
    data class CustomerNoteChanged(val note: String) : ProgramsUiEvent
    data object SubmitProgramSelection : ProgramsUiEvent
    data object ClearProgramSelection : ProgramsUiEvent
    data object RefreshAdminRequests : ProgramsUiEvent
    /**
     * The beta catalogue is free. Pricing is deliberately not exposed to the
     * administration UI; the view model always sends zero to the API.
     */
    data class ApproveRequest(
        val requestId: String,
        val adminNote: String? = null,
    ) : ProgramsUiEvent
    data class RejectRequest(val requestId: String, val adminNote: String? = null) : ProgramsUiEvent
    data class LoadBillingPreview(val clientId: String, val period: String) : ProgramsUiEvent
}
