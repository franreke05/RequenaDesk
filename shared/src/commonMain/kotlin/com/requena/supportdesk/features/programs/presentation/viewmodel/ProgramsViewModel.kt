package com.requena.supportdesk.features.programs.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.programs.domain.usecase.ApproveProgramRequestUseCase
import com.requena.supportdesk.features.programs.domain.usecase.GetAdminProgramRequestsUseCase
import com.requena.supportdesk.features.programs.domain.usecase.GetClientProgramBillingPreviewUseCase
import com.requena.supportdesk.features.programs.domain.usecase.GetClientProgramsUseCase
import com.requena.supportdesk.features.programs.domain.usecase.RejectProgramRequestUseCase
import com.requena.supportdesk.features.programs.domain.usecase.RequestProgramsUseCase
import com.requena.supportdesk.features.programs.presentation.effect.ProgramsUiEffect
import com.requena.supportdesk.features.programs.presentation.event.ProgramsUiEvent
import com.requena.supportdesk.features.programs.presentation.state.ProgramsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProgramsViewModel(
    private val getClientProgramsUseCase: GetClientProgramsUseCase,
    private val requestProgramsUseCase: RequestProgramsUseCase,
    private val getAdminProgramRequestsUseCase: GetAdminProgramRequestsUseCase,
    private val approveProgramRequestUseCase: ApproveProgramRequestUseCase,
    private val rejectProgramRequestUseCase: RejectProgramRequestUseCase,
    private val getBillingPreviewUseCase: GetClientProgramBillingPreviewUseCase,
) : BaseViewModel() {
    private val _state = MutableStateFlow(ProgramsUiState())
    val state: StateFlow<ProgramsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProgramsUiEffect>(extraBufferCapacity = 2)
    val effects: SharedFlow<ProgramsUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: ProgramsUiEvent) {
        when (event) {
            ProgramsUiEvent.RefreshClientPrograms -> refreshClientPrograms()
            is ProgramsUiEvent.ToggleProgramSelection -> toggleProgram(event.productKey)
            is ProgramsUiEvent.CustomerNoteChanged -> _state.update { it.copy(customerNote = event.note) }
            ProgramsUiEvent.SubmitProgramSelection -> submitSelection()
            ProgramsUiEvent.ClearProgramSelection -> _state.update { it.copy(selectedProgramKeys = emptySet(), customerNote = "") }
            ProgramsUiEvent.RefreshAdminRequests -> refreshAdminRequests()
            is ProgramsUiEvent.ApproveRequest -> decide(event.requestId, event.adminNote, approved = true)
            is ProgramsUiEvent.RejectRequest -> decide(event.requestId, event.adminNote, approved = false)
            is ProgramsUiEvent.LoadBillingPreview -> loadBillingPreview(event.clientId, event.period)
        }
    }

    private fun refreshClientPrograms() = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = getClientProgramsUseCase()) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, overview = result.data) }
        }
    }

    private fun toggleProgram(productKey: String) {
        val current = state.value
        val selectable = current.overview.catalog.any { program ->
            program.key == productKey && program.isAvailable && program.isRequestable &&
                current.overview.subscriptions.none { it.productKey == productKey && it.status.name == "ACTIVE" } &&
                current.overview.requests.none { it.productKey == productKey && it.status.name == "REQUESTED" }
        }
        if (!selectable) return
        _state.update {
            it.copy(
                selectedProgramKeys = if (productKey in it.selectedProgramKeys) {
                    it.selectedProgramKeys - productKey
                } else {
                    it.selectedProgramKeys + productKey
                },
            )
        }
    }

    private fun submitSelection() {
        val snapshot = state.value
        if (snapshot.isSubmitting || snapshot.selectedProgramKeys.isEmpty()) return
        launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = requestProgramsUseCase(snapshot.selectedProgramKeys, snapshot.customerNote.trim())) {
                is AppResult.Error -> fail(result.message, submitting = true)
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, selectedProgramKeys = emptySet(), customerNote = "") }
                    _effects.emit(ProgramsUiEffect.ShowMessage("Solicitud enviada para revisión del administrador"))
                    refreshClientPrograms()
                }
            }
        }
    }

    private fun refreshAdminRequests() = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = getAdminProgramRequestsUseCase()) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, adminRequests = result.data) }
        }
    }

    private fun decide(requestId: String, note: String?, approved: Boolean) {
        if (state.value.isSubmitting) return
        launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = if (approved) {
                approveProgramRequestUseCase(requestId, 0L, note?.trim()?.ifBlank { null })
            } else {
                rejectProgramRequestUseCase(requestId, note?.trim()?.ifBlank { null })
            }
            when (result) {
                is AppResult.Error -> fail(result.message, submitting = true)
                is AppResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.emit(
                        ProgramsUiEffect.ShowMessage(
                            if (approved) "Programa autorizado y activado gratis durante la beta" else "Solicitud rechazada",
                        ),
                    )
                    refreshAdminRequests()
                }
            }
        }
    }

    private fun loadBillingPreview(clientId: String, period: String) = launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = getBillingPreviewUseCase(clientId, period)) {
            is AppResult.Error -> fail(result.message)
            is AppResult.Success -> _state.update { it.copy(isLoading = false, billingPreview = result.data) }
        }
    }

    private suspend fun fail(message: String, submitting: Boolean = false) {
        _state.update { it.copy(isLoading = false, isSubmitting = if (submitting) false else it.isSubmitting, errorMessage = message) }
        _effects.emit(ProgramsUiEffect.ShowMessage(message))
    }
}
