package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.result.AppResult
import com.requena.supportdesk.features.invoices.domain.usecase.CreateInvoiceUseCase
import com.requena.supportdesk.features.invoices.domain.usecase.GetInvoicePdfUrlUseCase
import com.requena.supportdesk.features.invoices.domain.usecase.GetInvoiceUseCase
import com.requena.supportdesk.features.invoices.domain.usecase.GetInvoicesUseCase
import com.requena.supportdesk.features.invoices.domain.usecase.UpdateInvoiceStatusUseCase
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
    private val getInvoicesUseCase: GetInvoicesUseCase,
    private val getInvoiceUseCase: GetInvoiceUseCase,
    private val createInvoiceUseCase: CreateInvoiceUseCase,
    private val updateInvoiceStatusUseCase: UpdateInvoiceStatusUseCase,
    private val getInvoicePdfUrlUseCase: GetInvoicePdfUrlUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(InvoicesUiState())
    val state: StateFlow<InvoicesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<InvoicesUiEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<InvoicesUiEffect> = _effects.asSharedFlow()

    init {
        onEvent(InvoicesUiEvent.Load)
    }

    fun onEvent(event: InvoicesUiEvent) {
        when (event) {
            InvoicesUiEvent.Load -> loadInvoices()
            is InvoicesUiEvent.SelectInvoice -> selectInvoice(event.invoiceId)
            is InvoicesUiEvent.CreateInvoice -> createInvoice(event)
            is InvoicesUiEvent.UpdateStatus -> updateStatus(event)
            is InvoicesUiEvent.DownloadPdf -> downloadPdf(event.invoiceId)
        }
    }

    private fun loadInvoices() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getInvoicesUseCase()) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(InvoicesUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> _state.update { it.copy(isLoading = false, invoices = result.data) }
            }
        }
    }

    private fun selectInvoice(invoiceId: String) {
        launch {
            when (val result = getInvoiceUseCase(invoiceId)) {
                is AppResult.Error -> {
                    _state.update {
                        it.copy(selectedInvoice = it.invoices.firstOrNull { inv -> inv.id == invoiceId })
                    }
                    _effects.emit(InvoicesUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> _state.update { it.copy(selectedInvoice = result.data) }
            }
        }
    }

    private fun createInvoice(event: InvoicesUiEvent.CreateInvoice) {
        launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = createInvoiceUseCase(event.input)) {
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                    _effects.emit(InvoicesUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    val updated = listOf(result.data) + _state.value.invoices
                    val message = "Factura ${result.data.invoiceNumber} creada."
                    _state.update {
                        it.copy(
                            isLoading = false,
                            invoices = updated,
                            selectedInvoice = result.data,
                            statusMessage = message,
                        )
                    }
                    _effects.emit(InvoicesUiEffect.ShowMessage(message))
                }
            }
        }
    }

    private fun updateStatus(event: InvoicesUiEvent.UpdateStatus) {
        launch {
            when (val result = updateInvoiceStatusUseCase(event.invoiceId, event.status)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(InvoicesUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> {
                    val updated = _state.value.invoices.map {
                        if (it.id == event.invoiceId) result.data else it
                    }
                    val message = "Estado actualizado a ${result.data.status.name}."
                    _state.update {
                        it.copy(
                            invoices = updated,
                            selectedInvoice = result.data,
                            statusMessage = message,
                        )
                    }
                    _effects.emit(InvoicesUiEffect.ShowMessage(message))
                }
            }
        }
    }

    private fun downloadPdf(invoiceId: String) {
        launch {
            when (val result = getInvoicePdfUrlUseCase(invoiceId)) {
                is AppResult.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                    _effects.emit(InvoicesUiEffect.ShowMessage(result.message))
                }
                is AppResult.Success -> _effects.emit(InvoicesUiEffect.OpenPdfUrl(result.data))
            }
        }
    }
}
