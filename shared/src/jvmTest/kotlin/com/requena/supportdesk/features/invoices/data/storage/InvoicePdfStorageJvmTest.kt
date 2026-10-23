package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoicePdfStorageJvmTest {
    @Test
    fun createsAProfessionalInvoiceWithAllLegalDataWithoutAnyServer() = withTempDirectory { directory ->
        val storage = DesktopInvoicePdfStorage(invoicesDirectory = directory)
        val savedInvoice = storage.saveInvoice(testInvoice(), "FAC-2026-0001")
        val pdf = directory.resolve(savedInvoice.fileName)

        assertTrue(Files.readAllBytes(pdf).decodeToString().startsWith("%PDF"))
        assertEquals(savedInvoice, storage.listSavedInvoices().single())

        val pdfText = PDDocument.load(pdf.toFile()).use(PDFTextStripper()::getText)
        // Title, number and dates.
        assertContains(pdfText, "FACTURA")
        assertContains(pdfText, "FAC-2026-0001")
        assertContains(pdfText, "Fecha de emisión")
        assertContains(pdfText, "Fecha de vencimiento")
        // Issuer.
        assertContains(pdfText, "Ana Pérez Gómez")
        assertContains(pdfText, "NIF: 12345678Z")
        assertContains(pdfText, "Calle Mayor 1")
        // Recipient.
        assertContains(pdfText, "Empresa Ejemplo SL")
        assertContains(pdfText, "NIF/CIF: B00000000")
        assertContains(pdfText, "Avenida Ejemplo 2")
        // Lines and totals: base 127,50 + IVA 26,78 - IRPF 19,13 = 135,15.
        assertContains(pdfText, "Tarea uno")
        assertContains(pdfText, "Actividad adicional")
        assertContains(pdfText, "Base imponible")
        assertContains(pdfText, "127,50 EUR")
        assertContains(pdfText, "Retención IRPF (15,00%)")
        assertContains(pdfText, "-19,13 EUR")
        assertContains(pdfText, "TOTAL A PAGAR")
        assertContains(pdfText, "135,15 EUR")
        // Payment.
        assertContains(pdfText, "Transferencia bancaria")
        assertContains(pdfText, "ES91 2100 0418 4502 0005 1332")
    }

    @Test
    fun refusesAnInvoiceWithoutTheIssuerTaxData() = withTempDirectory { directory ->
        val storage = DesktopInvoicePdfStorage(invoicesDirectory = directory)
        val error = assertFailsWith<IllegalArgumentException> {
            storage.saveInvoice(testInvoice().copy(issuer = InvoiceIssuer(fullName = "Ana")), "FAC-2026-0001")
        }
        assertContains(error.message.orEmpty(), "NIF")
        assertTrue(storage.listSavedInvoices().isEmpty())
    }

    @Test
    fun zeroVatRequiresTheLegalReasonAndPrintsIt() = withTempDirectory { directory ->
        val storage = DesktopInvoicePdfStorage(invoicesDirectory = directory)
        assertFailsWith<IllegalArgumentException> {
            storage.saveInvoice(testInvoice().copy(taxPercent = 0.0), "FAC-2026-0001")
        }
        val saved = storage.saveInvoice(
            testInvoice().copy(taxPercent = 0.0, taxExemptionNote = "Operación exenta de IVA"),
            "FAC-2026-0002",
        )
        val pdfText = PDDocument.load(directory.resolve(saved.fileName).toFile()).use(PDFTextStripper()::getText)
        assertContains(pdfText, "Operación exenta de IVA")
        assertFalse(pdfText.contains("Retención IRPF (0"))
    }

    private fun withTempDirectory(block: suspend (Path) -> Unit) = runBlocking {
        val directory = Files.createTempDirectory("invoice-storage-test")
        try {
            block(directory)
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    // Fictitious data only.
    private fun testInvoice() = CreateInvoiceInput(
        clientId = "client-test",
        clientName = "Empresa Ejemplo SL",
        issuedAt = "2026-07-15",
        dueAt = "2026-07-30",
        notes = "Factura local de prueba sin servidor.",
        taxPercent = 21.0,
        withholdingPercent = 15.0,
        reference = "AGL001",
        issuer = InvoiceIssuer(
            fullName = "Ana Pérez Gómez",
            tradeName = "OryKai software",
            taxId = "12345678Z",
            addressLine = "Calle Mayor 1",
            postalCode = "04001",
            city = "Almería",
            iban = "ES9121000418450200051332",
        ),
        recipient = InvoiceRecipient(
            legalName = "Empresa Ejemplo SL",
            taxId = "B00000000",
            addressLine = "Avenida Ejemplo 2",
            postalCode = "28001",
            city = "Madrid",
        ),
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
