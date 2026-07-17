package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.features.invoices.data.storage.InvoicePdfStorage
import com.requena.supportdesk.features.invoices.presentation.effect.InvoicesUiEffect
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import com.requena.supportdesk.features.invoices.presentation.state.InvoicesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException

class InvoicesViewModel(
    private val invoicePdfStorage: InvoicePdfStorage,
) : BaseViewModel() {

    private val _state = MutableStateFlow(InvoicesUiState())
    val state: StateFlow<InvoicesUiState> = _state.asStateFlow()

    private val _effects = Channel<InvoicesUiEffect>(Channel.BUFFERED)
    val effects: Flow<InvoicesUiEffect> = _effects.receiveAsFlow()

    init {
        refreshSavedInvoices()
    }

    fun onEvent(event: InvoicesUiEvent) {
        when (event) {
            InvoicesUiEvent.RefreshSavedInvoices -> refreshSavedInvoices()
            is InvoicesUiEvent.OpenSavedInvoice -> openSavedInvoice(event.fileName)
            is InvoicesUiEvent.DeleteSavedInvoice -> deleteSavedInvoice(event.fileName)
            is InvoicesUiEvent.GenerateInvoice -> generateInvoice(event)
        }
    }

    private fun generateInvoice(event: InvoicesUiEvent.GenerateInvoice) {
        if (state.value.isGenerating) return
        launch {
            _state.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val savedInvoice = invoicePdfStorage.saveInvoice(event.input)
                val savedInvoices = invoicePdfStorage.listSavedInvoices()
                _state.update { it.copy(isGenerating = false, savedInvoices = savedInvoices) }
                _effects.send(
                    InvoicesUiEffect.ShowMessage(
                        "${savedInvoice.fileName} guardada en Escritorio/Facturas OryKai.",
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                val message = detail?.let { "No se pudo crear el PDF local: $it" }
                    ?: "No se pudo crear el PDF local. Revisa los logs invoice_pdf.*."
                _state.update { it.copy(isGenerating = false, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }

    private fun refreshSavedInvoices() {
        if (state.value.isLoadingSavedInvoices) return
        launch {
            _state.update { it.copy(isLoadingSavedInvoices = true, errorMessage = null) }
            try {
                val savedInvoices = invoicePdfStorage.listSavedInvoices()
                _state.update { it.copy(isLoadingSavedInvoices = false, savedInvoices = savedInvoices) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: "No se pudieron cargar las facturas guardadas."
                _state.update { it.copy(isLoadingSavedInvoices = false, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }

    private fun openSavedInvoice(fileName: String) {
        launch {
            try {
                invoicePdfStorage.openSavedInvoice(fileName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: "No se pudo abrir la factura."
                _state.update { it.copy(errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }

    private fun deleteSavedInvoice(fileName: String) {
        if (state.value.deletingInvoiceFileName != null) return
        launch {
            _state.update { it.copy(deletingInvoiceFileName = fileName, errorMessage = null) }
            try {
                invoicePdfStorage.deleteSavedInvoice(fileName)
                val savedInvoices = invoicePdfStorage.listSavedInvoices()
                _state.update {
                    it.copy(
                        deletingInvoiceFileName = null,
                        savedInvoices = savedInvoices,
                    )
                }
                _effects.send(InvoicesUiEffect.ShowMessage("Factura borrada de la biblioteca local."))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                val message = detail?.let { "No se pudo borrar la factura local: $it" }
                    ?: "No se pudo borrar la factura local."
                _state.update { it.copy(deletingInvoiceFileName = null, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }
}
