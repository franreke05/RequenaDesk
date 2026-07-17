package com.requena.supportdesk.features.invoices.presentation.viewmodel

import com.requena.supportdesk.features.invoices.data.storage.InvoicePdfStorage
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
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
        val viewModel = InvoicesViewModel(storage)

        try {
            val effect = async { withTimeout(TEST_TIMEOUT_MILLIS) { viewModel.effects.first() } }
            viewModel.onEvent(InvoicesUiEvent.GenerateInvoice(input))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { state ->
                    !state.isGenerating && state.savedInvoices == listOf(storage.savedInvoice)
                }
            }

            assertEquals(input, storage.savedInput)
            assertEquals(listOf(storage.savedInvoice), completedState.savedInvoices)
            assertEquals(null, completedState.errorMessage)
            assertTrue(storage.listCalls >= 1)
            assertTrue(assertIs<InvoicesUiEffect.ShowMessage>(effect.await()).message.contains("guardada"))
        } finally {
            viewModel.clear()
        }
    }

    @Test
    fun deleteEventRemovesTheLocalInvoiceAndRefreshesTheLibrary() = runBlocking {
        val storage = RecordingInvoicePdfStorage().apply { savedInput = testInvoice() }
        val viewModel = InvoicesViewModel(storage)

        try {
            viewModel.onEvent(InvoicesUiEvent.DeleteSavedInvoice(storage.savedInvoice.fileName))

            val completedState = withTimeout(TEST_TIMEOUT_MILLIS) {
                viewModel.state.first { state ->
                    storage.deletedFileName == storage.savedInvoice.fileName &&
                        state.deletingInvoiceFileName == null &&
                        state.savedInvoices.isEmpty()
                }
            }

            assertEquals(storage.savedInvoice.fileName, storage.deletedFileName)
            assertTrue(completedState.savedInvoices.isEmpty())
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
        var deletedFileName: String? = null

        override suspend fun listSavedInvoices(): List<InvoicePdfFile> {
            listCalls += 1
            return if (savedInput == null || deletedFileName != null) emptyList() else listOf(savedInvoice)
        }

        override suspend fun saveInvoice(input: CreateInvoiceInput): InvoicePdfFile {
            savedInput = input
            return savedInvoice
        }

        override suspend fun openSavedInvoice(fileName: String) = Unit

        override suspend fun deleteSavedInvoice(fileName: String) {
            check(fileName == savedInvoice.fileName)
            deletedFileName = fileName
        }
    }

    private fun testInvoice() = CreateInvoiceInput(
        clientId = "client-test",
        clientName = "Cliente de prueba",
        issuedAt = "2026-07-15",
        dueAt = null,
        notes = null,
        taxPercent = 21.0,
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
