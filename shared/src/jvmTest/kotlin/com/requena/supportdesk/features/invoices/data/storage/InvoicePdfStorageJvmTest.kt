package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoicePdfStorageJvmTest {
    @Test
    fun createsAndListsAMultiTaskInvoiceWithoutAnyServer() = runBlocking {
        val directory = Files.createTempDirectory("invoice-storage-test")

        try {
            val storage = DesktopInvoicePdfStorage(directory)
            val savedInvoice = storage.saveInvoice(testInvoice())
            val pdf = directory.resolve(savedInvoice.fileName)

            assertTrue(Files.isRegularFile(pdf))
            assertTrue(Files.readAllBytes(pdf).decodeToString().startsWith("%PDF"))
            assertEquals(savedInvoice, storage.listSavedInvoices().single())

            val pdfText = PDDocument.load(pdf.toFile()).use(PDFTextStripper()::getText)
            assertContains(pdfText, "Cliente de prueba")
            assertContains(pdfText, "Tarea uno")
            assertContains(pdfText, "Tarea dos")
            assertContains(pdfText, "Horas: 2 h")
            assertContains(pdfText, "TOTAL: \$127.05")
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    private fun testInvoice() = CreateInvoiceInput(
        clientId = "client-test",
        clientName = "Cliente de prueba",
        issuedAt = "2026-07-15",
        dueAt = "2026-07-30",
        notes = "Factura local de prueba sin servidor.",
        taxPercent = 21.0,
        items = listOf(
            CreateInvoiceItemInput(
                description = "Tarea uno",
                quantity = 1.1,
                unitPrice = 30.0,
                sortOrder = 0,
            ),
            CreateInvoiceItemInput(
                description = "Tarea dos",
                quantity = 1.0,
                unitPrice = 45.0,
                sortOrder = 1,
            ),
        ),
    )
}
