package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.features.invoices.data.ledger.DuplicateBillingException
import com.requena.supportdesk.features.invoices.data.ledger.InvoiceBillingLedger
import com.requena.supportdesk.features.invoices.data.profile.InvoiceProfileStore
import com.requena.supportdesk.features.invoices.data.storage.InvoicePdfStorage
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.presentation.effect.InvoicesUiEffect
import com.requena.supportdesk.features.invoices.presentation.event.InvoicesUiEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvoicesViewModelJvmTest {
    @Test
    fun generateEventSavesLocallyRefreshesTheLibraryAndEmitsOneSuccessEffect() = runBlocking {
        val input = testInvoice()
        val storage = RecordingInvoicePdfStorage()
        val ledger = RecordingInvoiceBillingLedger()
        val viewModel = InvoicesViewModel(storage, ledger, RecordingInvoiceProfileStore())

        try {
            val effect = async { withTimeout(TEST_TIMEOUT_MILLIS) { viewModel.effects.first() } }
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(input, selectedLogIds = listOf("log-1", "log-2")))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { state ->
                    !state.isGenerating && state.savedInvoices == listOf(storage.savedInvoice)
                }
            }

            assertEquals(input, storage.savedInput)
            assertEquals(listOf(storage.savedInvoice), completedState.savedInvoices)
            assertEquals(null, completedState.errorMessage)
            assertEquals(setOf("log-1", "log-2"), completedState.billedLogIds)
            assertTrue(storage.listCalls >= 1)
            assertTrue(assertIs<InvoicesUiEffect.ShowMessage>(effect.await()).message.contains("guardada"))
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun generateEventRejectsUnitsAlreadyBilledAndDoesNotWriteAPdf() = runBlocking {
        val storage = RecordingInvoicePdfStorage()
        val ledger = RecordingInvoiceBillingLedger().apply { billed.add("log-1") }
        val viewModel = InvoicesViewModel(storage, ledger, RecordingInvoiceProfileStore())

        try {
            val effect = async { withTimeout(TEST_TIMEOUT_MILLIS) { viewModel.effects.first() } }
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(testInvoice(), selectedLogIds = listOf("log-1")))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { state -> !state.isGenerating && state.errorMessage != null }
            }

            assertEquals(null, storage.savedInput)
            assertTrue(completedState.errorMessage!!.contains("ya esta facturada", ignoreCase = true))
            assertTrue(assertIs<InvoicesUiEffect.ShowMessage>(effect.await()).message.contains("facturada", ignoreCase = true))
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun voidEventReleasesBilledUnitsBackToPending() = runBlocking {
        val storage = RecordingInvoicePdfStorage()
        val ledger = RecordingInvoiceBillingLedger().apply {
            billed.addAll(listOf("log-1", "log-2"))
            issuedInvoiceNumbers["FAC-2026-TEST"] = listOf("log-1", "log-2")
        }
        val viewModel = InvoicesViewModel(storage, ledger, RecordingInvoiceProfileStore())

        try {
            viewModel.onEvent(InvoicesUiEvent.VoidInvoice(fileName = "FAC-2026-TEST_Cliente.pdf", reason = "Importe incorrecto"))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { state -> state.billedLogIds.isEmpty() && state.voidingInvoiceFileName == null }
            }

            assertTrue(completedState.billedLogIds.isEmpty())
            assertEquals(setOf("FAC-2026-TEST"), completedState.voidedInvoiceNumbers)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun missingFiscalDataBlocksTheInvoiceBeforeANumberIsTaken() = runBlocking {
        val storage = RecordingInvoicePdfStorage()
        val ledger = RecordingInvoiceBillingLedger()
        val viewModel = InvoicesViewModel(storage, ledger, RecordingInvoiceProfileStore())

        try {
            val incomplete = testInvoice().copy(recipient = InvoiceRecipient(legalName = "Cliente de prueba"))
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(incomplete))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { it.errorMessage != null }
            }

            assertTrue(completedState.errorMessage!!.contains("NIF/CIF del cliente"))
            assertEquals(0, ledger.issuedCount)
            assertEquals(null, storage.savedInput)
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun generatingRemembersIssuerAndClientFiscalData() = runBlocking {
        val profileStore = RecordingInvoiceProfileStore()
        val viewModel = InvoicesViewModel(RecordingInvoicePdfStorage(), RecordingInvoiceBillingLedger(), profileStore)

        try {
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(testInvoice()))
            withTimeout(TEST_TIMEOUT_MILLIS) { viewModel.state.first { !it.isGenerating && it.savedInvoices.isNotEmpty() } }

            assertEquals(testInvoice().issuer, profileStore.issuer)
            assertEquals(testInvoice().recipient, profileStore.recipients["client-test"])
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun aPdfFailureVoidsTheNumberSoTheSeriesStaysAuditable() = runBlocking {
        val storage = RecordingInvoicePdfStorage().apply { failOnSave = true }
        val ledger = RecordingInvoiceBillingLedger()
        val viewModel = InvoicesViewModel(storage, ledger, RecordingInvoiceProfileStore())

        try {
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(testInvoice(), selectedLogIds = listOf("log-1")))
            withTimeout(TEST_TIMEOUT_MILLIS) { viewModel.state.first { !it.isGenerating && it.errorMessage != null } }

            assertEquals(1, ledger.issuedCount)
            assertTrue(ledger.billed.isEmpty())
        } finally {
            viewModel.clear()
        }
    }

    private class RecordingInvoicePdfStorage : InvoicePdfStorage {
        val savedInvoice = InvoicePdfFile(
            fileName = "FAC-2026-TEST_Cliente.pdf",
            sizeBytes = 1_024,
            lastModifiedEpochMillis = 1L,
        )

        @Volatile
        var savedInput: CreateInvoiceInput? = null

        @Volatile
        var listCalls: Int = 0

        @Volatile
        var failOnSave: Boolean = false

        override suspend fun listSavedInvoices(): List<InvoicePdfFile> {
            listCalls += 1
            return if (savedInput == null) emptyList() else listOf(savedInvoice)
        }

        override suspend fun saveInvoice(input: CreateInvoiceInput, invoiceNumber: String): InvoicePdfFile {
            check(!failOnSave) { "Disco lleno" }
            savedInput = input
            return savedInvoice
        }

        override suspend fun openSavedInvoice(fileName: String) = Unit
    }

    private class RecordingInvoiceBillingLedger : InvoiceBillingLedger {
        val billed = mutableSetOf<String>()
        val issuedInvoiceNumbers = mutableMapOf<String, List<String>>()
        private val voided = mutableSetOf<String>()

        @Volatile
        var issuedCount: Int = 0

        override suspend fun billedLogIds(): Set<String> = billed.toSet()

        override suspend fun issueInvoice(issuedAt: String, logIds: List<String>): String {
            val duplicates = logIds.filter { it in billed }
            if (duplicates.isNotEmpty()) {
                throw DuplicateBillingException("Alguna de las unidades de tiempo seleccionadas ya esta facturada.")
            }
            issuedCount += 1
            val invoiceNumber = if (issuedCount == 1) "FAC-2026-TEST" else "FAC-2026-TEST$issuedCount"
            billed.addAll(logIds)
            issuedInvoiceNumbers[invoiceNumber] = logIds
            return invoiceNumber
        }

        override suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String> {
            if (invoiceNumber in voided) return emptyList()
            val logIds = issuedInvoiceNumbers[invoiceNumber] ?: return emptyList()
            voided += invoiceNumber
            billed.removeAll(logIds.toSet())
            return logIds
        }

        override suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean =
            invoiceNumber in issuedInvoiceNumbers && invoiceNumber !in voided

        override suspend fun voidedInvoiceNumbers(): Set<String> = voided.toSet()
    }

    private class RecordingInvoiceProfileStore : InvoiceProfileStore {
        @Volatile
        var issuer: InvoiceIssuer? = null
        val recipients = java.util.concurrent.ConcurrentHashMap<String, InvoiceRecipient>()

        override suspend fun loadIssuer(): InvoiceIssuer = issuer ?: InvoiceIssuer()
        override suspend fun saveIssuer(issuer: InvoiceIssuer) { this.issuer = issuer }
        override suspend fun loadRecipient(clientId: String): InvoiceRecipient? = recipients[clientId]
        override suspend fun saveRecipient(clientId: String, recipient: InvoiceRecipient) { recipients[clientId] = recipient }
    }

    // Fictitious data only.
    private fun testInvoice() = CreateInvoiceInput(
        clientId = "client-test",
        clientName = "Cliente de prueba",
        issuedAt = "2026-07-15",
        dueAt = null,
        notes = null,
        taxPercent = 21.0,
        issuer = InvoiceIssuer(
            fullName = "Ana Pérez Gómez",
            taxId = "12345678Z",
            addressLine = "Calle Mayor 1",
            postalCode = "04001",
            city = "Almería",
        ),
        recipient = InvoiceRecipient(
            legalName = "Cliente de prueba",
            taxId = "B00000000",
            addressLine = "Avenida Ejemplo 2",
            city = "Madrid",
        ),
        items = listOf(
            CreateInvoiceItemInput("Tarea uno", quantity = 2.0, unitPrice = 30.0, sortOrder = 0),
            CreateInvoiceItemInput(
                description = "Actividad",
                quantity = 1.5,
                unitPrice = 45.0,
                sortOrder = 1,
                kind = InvoiceItemKind.ACTIVITY,
            ),
        ),
    )

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 2_000L
    }
}
