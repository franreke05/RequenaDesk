package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.features.invoices.domain.usecase.GenerateInvoiceUseCase
import com.requena.supportdesk.features.invoices.presentation.effect.InvoicesUiEffect
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InvoicesViewModel(
    private val generateInvoiceUseCase: GenerateInvoiceUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(InvoicesUiState())
    val state: StateFlow<InvoicesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<InvoicesUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<InvoicesUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: InvoicesUiEvent) {
        when (event) {
            is InvoicesUiEvent.GenerateInvoice -> generateInvoice(event)
        }
    }

    private fun generateInvoice(event: InvoicesUiEvent.GenerateInvoice) {
        launch {
            _state.update { it.copy(isGenerating = true, errorMessage = null) }
            runCatching { generateInvoiceUseCase(event.input) }
                .onSuccess { url ->
                    _state.update { it.copy(isGenerating = false) }
                    _effects.emit(InvoicesUiEffect.OpenGeneratedInvoice(url))
                }
                .onFailure { error ->
                    val message = error.message ?: "No se pudo generar la factura."
                    _state.update { it.copy(isGenerating = false, errorMessage = message) }
                    _effects.emit(InvoicesUiEffect.ShowMessage(message))
                }
        }
    }
}
