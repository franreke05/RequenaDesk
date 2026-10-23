package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.features.invoices.data.ledger.DuplicateBillingException
import com.requena.supportdesk.features.invoices.data.ledger.InvoiceBillingLedger
import com.requena.supportdesk.features.invoices.data.ledger.InvoiceNumberingException
import com.requena.supportdesk.features.invoices.data.profile.InvoiceProfileStore
import com.requena.supportdesk.features.invoices.data.storage.InvoicePdfStorage
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.invoiceLegalProblem
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
    private val invoiceBillingLedger: InvoiceBillingLedger,
    private val invoiceProfileStore: InvoiceProfileStore,
) : BaseViewModel() {

    private val _state = MutableStateFlow(InvoicesUiState())
    val state: StateFlow<InvoicesUiState> = _state.asStateFlow()

    private val _effects = Channel<InvoicesUiEffect>(Channel.BUFFERED)
    val effects: Flow<InvoicesUiEffect> = _effects.receiveAsFlow()

    init {
        refreshSavedInvoices()
        refreshLedgerState()
        loadIssuer()
    }

    fun onEvent(event: InvoicesUiEvent) {
        when (event) {
            InvoicesUiEvent.RefreshSavedInvoices -> {
                refreshSavedInvoices()
                refreshLedgerState()
            }
            is InvoicesUiEvent.OpenSavedInvoice -> openSavedInvoice(event.fileName)
            is InvoicesUiEvent.LoadRecipient -> loadRecipient(event.clientId)
            is InvoicesUiEvent.GenerateInvoice -> generateInvoice(event)
            is InvoicesUiEvent.VoidInvoice -> voidInvoice(event.fileName, event.reason)
        }
    }

    private fun generateInvoice(event: InvoicesUiEvent.GenerateInvoice) {
        if (state.value.isGenerating) return
        val input = event.input
        // Checked before a number is taken so a wrong form never leaves a gap in the series.
        invoiceLegalProblem(input)?.let { problem ->
            _state.update { it.copy(errorMessage = problem) }
            _effects.trySend(InvoicesUiEffect.ShowMessage(problem))
            return
        }
        launch {
            _state.update { it.copy(isGenerating = true, errorMessage = null) }
            rememberFiscalData(input)
            try {
                // Ledger first: the number and the billed time are reserved atomically.
                val invoiceNumber = invoiceBillingLedger.issueInvoice(input.issuedAt, event.selectedLogIds)
                val savedInvoice = try {
                    invoicePdfStorage.saveInvoice(input, invoiceNumber)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    // Keep the series auditable: the number is recorded as voided, never reused.
                    runCatching { invoiceBillingLedger.recordVoided(invoiceNumber, "No se pudo generar el PDF") }
                    throw error
                }
                val savedInvoices = invoicePdfStorage.listSavedInvoices()
                val billedLogIds = invoiceBillingLedger.billedLogIds()
                _state.update {
                    it.copy(isGenerating = false, savedInvoices = savedInvoices, billedLogIds = billedLogIds)
                }
                _effects.send(
                    InvoicesUiEffect.ShowMessage(
                        "${savedInvoice.fileName} guardada en Escritorio/Facturas OryKai.",
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: DuplicateBillingException) {
                val message = error.message ?: "Alguna de las unidades seleccionadas ya esta facturada."
                _state.update { it.copy(isGenerating = false, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            } catch (error: InvoiceNumberingException) {
                val message = error.message ?: "No se pudo asignar el número de factura."
                _state.update { it.copy(isGenerating = false, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            } catch (error: Throwable) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                val message = detail?.let { "No se pudo crear el PDF local: $it" }
                    ?: "No se pudo crear el PDF local. Revisa los logs invoice_pdf.*."
                _state.update { it.copy(isGenerating = false, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }

    /** Saving the data is a convenience; a failure here must never block the invoice. */
    private suspend fun rememberFiscalData(input: CreateInvoiceInput) {
        runCatching { invoiceProfileStore.saveIssuer(input.issuer) }
        runCatching { invoiceProfileStore.saveRecipient(input.clientId, input.recipient) }
        _state.update {
            it.copy(
                issuer = input.issuer,
                recipientsByClientId = it.recipientsByClientId + (input.clientId to input.recipient),
            )
        }
    }

    private fun loadIssuer() {
        launch {
            val issuer = runCatching { invoiceProfileStore.loadIssuer() }.getOrDefault(InvoiceIssuer())
            _state.update { it.copy(issuer = issuer) }
        }
    }

    private fun loadRecipient(clientId: String) {
        if (clientId.isBlank() || clientId in state.value.recipientsByClientId) return
        launch {
            runCatching { invoiceProfileStore.loadRecipient(clientId) }.getOrNull()?.let { recipient ->
                _state.update { it.copy(recipientsByClientId = it.recipientsByClientId + (clientId to recipient)) }
            }
        }
    }

    private fun voidInvoice(fileName: String, reason: String?) {
        if (state.value.voidingInvoiceFileName != null) return
        val invoiceNumber = fileName.substringBefore("_")
        launch {
            _state.update { it.copy(voidingInvoiceFileName = fileName, errorMessage = null) }
            try {
                val releasedLogIds = invoiceBillingLedger.recordVoided(invoiceNumber, reason)
                val billedLogIds = invoiceBillingLedger.billedLogIds()
                val voidedInvoiceNumbers = invoiceBillingLedger.voidedInvoiceNumbers()
                _state.update {
                    it.copy(
                        voidingInvoiceFileName = null,
                        billedLogIds = billedLogIds,
                        voidedInvoiceNumbers = voidedInvoiceNumbers,
                    )
                }
                val message = if (releasedLogIds.isEmpty()) {
                    "Factura anulada."
                } else {
                    "Factura anulada. ${releasedLogIds.size} unidad(es) de tiempo vuelven a estar pendientes."
                }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: "No se pudo anular la factura."
                _state.update { it.copy(voidingInvoiceFileName = null, errorMessage = message) }
                _effects.send(InvoicesUiEffect.ShowMessage(message))
            }
        }
    }

    private fun refreshLedgerState() {
        launch {
            runCatching {
                invoiceBillingLedger.billedLogIds() to invoiceBillingLedger.voidedInvoiceNumbers()
            }.onSuccess { (billedLogIds, voidedInvoiceNumbers) ->
                _state.update { it.copy(billedLogIds = billedLogIds, voidedInvoiceNumbers = voidedInvoiceNumbers) }
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
}
