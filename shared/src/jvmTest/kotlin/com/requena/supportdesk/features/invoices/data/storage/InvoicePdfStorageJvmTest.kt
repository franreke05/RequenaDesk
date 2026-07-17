package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoicePdfStorageJvmTest {
    @Test
    fun createsAndListsAMixedTaskAndActivityInvoiceWithoutAnyServer() = runBlocking {
        val directory = Files.createTempDirectory("invoice-storage-test")

        try {
            val storage = DesktopInvoicePdfStorage(
                invoicesDirectory = directory,
                issuerProfile = InvoiceIssuerProfile(name = "OryKai software"),
            )
            val savedInvoice = storage.saveInvoice(testInvoice())
            val pdf = directory.resolve(savedInvoice.fileName)

            assertTrue(Files.isRegularFile(pdf))
            assertTrue(Files.readAllBytes(pdf).decodeToString().startsWith("%PDF"))
            assertEquals(savedInvoice, storage.listSavedInvoices().single())

            val pdfText = PDDocument.load(pdf.toFile()).use(PDFTextStripper()::getText)
            assertContains(pdfText, "OryKai software")
            assertContains(pdfText, "Cliente de prueba")
            assertContains(pdfText, "Fecha de vencimiento")
            assertContains(pdfText, "Conceptos")
            assertContains(pdfText, "Tarea uno")
            assertContains(pdfText, "Actividad adicional")
            assertContains(pdfText, "horas")
            assertContains(pdfText, "unidades")
            assertContains(pdfText, "154,28 EUR")
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun deletesOnlyTheSelectedLocalInvoicePdf() = runBlocking {
        val directory = Files.createTempDirectory("invoice-storage-delete-test")

        try {
            val storage = DesktopInvoicePdfStorage(
                invoicesDirectory = directory,
                issuerProfile = InvoiceIssuerProfile(name = "OryKai software"),
            )
            val savedInvoice = storage.saveInvoice(testInvoice())

            storage.deleteSavedInvoice(savedInvoice.fileName)

            assertFalse(Files.exists(directory.resolve(savedInvoice.fileName)))
            assertTrue(storage.listSavedInvoices().isEmpty())
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
        reference = "AGL001",
        items = listOf(
            CreateInvoiceItemInput(
                description = "Tarea uno",
                detail = "Horas de soporte y configuración.",
                quantity = 2.0,
                unitPrice = 30.0,
                sortOrder = 0,
            ),
            CreateInvoiceItemInput(
                description = "Actividad adicional",
                detail = "Servicio facturado por unidades.",
                quantity = 1.5,
                unitPrice = 45.0,
                sortOrder = 1,
                kind = InvoiceItemKind.ACTIVITY,
            ),
        ),
    )
}
