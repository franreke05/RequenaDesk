package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.awt.Desktop
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.ceil

actual fun createInvoicePdfStorage(): InvoicePdfStorage = DesktopInvoicePdfStorage()

internal class DesktopInvoicePdfStorage(
    private val invoicesDirectory: Path = defaultInvoicesDirectory(),
) : InvoicePdfStorage {

    override suspend fun listSavedInvoices(): List<InvoicePdfFile> {
        logger.info("invoice_pdf.list.start directory=${invoicesDirectory.toAbsolutePath().normalize()}")
        return try {
            val directory = ensureInvoicesDirectory()
            Files.list(directory).use { paths ->
                paths.iterator().asSequence()
                    .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(PDF_EXTENSION, true) }
                    .map(::invoiceFile)
                    .sortedByDescending(InvoicePdfFile::lastModifiedEpochMillis)
                    .toList()
            }.also { invoices ->
                logger.info("invoice_pdf.list.success directory=$directory count=${invoices.size}")
            }
        } catch (error: Throwable) {
            logger.log(
                Level.SEVERE,
                "invoice_pdf.list.failure directory=${invoicesDirectory.toAbsolutePath().normalize()}",
                error,
            )
            throw error
        }
    }

    override suspend fun saveInvoice(input: CreateInvoiceInput): InvoicePdfFile {
        val operationId = UUID.randomUUID().toString().take(OPERATION_ID_LENGTH)
        logger.info("invoice_pdf.create.start operationId=$operationId itemCount=${input.items.size}")

        return try {
            validate(input)
            val directory = ensureInvoicesDirectory()
            val invoiceNumber = createInvoiceNumber(input.issuedAt)
            val target = availableTarget(directory, pdfFileName(invoiceNumber, input.clientName))
            logger.info("invoice_pdf.create.write operationId=$operationId target=$target")

            saveAtomically(directory, target) { temporary ->
                writeInvoicePdf(input, invoiceNumber, temporary)
            }

            invoiceFile(target).also { savedInvoice ->
                logger.info(
                    "invoice_pdf.create.success operationId=$operationId " +
                        "fileName=${savedInvoice.fileName} sizeBytes=${savedInvoice.sizeBytes}",
                )
            }
        } catch (error: Throwable) {
            logger.log(Level.SEVERE, "invoice_pdf.create.failure operationId=$operationId", error)
            throw error
        }
    }

    override suspend fun openSavedInvoice(fileName: String) {
        logger.info("invoice_pdf.open.start fileName=$fileName")
        try {
            val directory = ensureInvoicesDirectory()
            val invoice = directory.resolve(fileName).normalize()
            require(invoice.parent == directory && Files.isRegularFile(invoice)) {
                "La factura seleccionada ya no existe."
            }
            require(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                "Este equipo no puede abrir archivos PDF automaticamente."
            }
            Desktop.getDesktop().open(invoice.toFile())
            logger.info("invoice_pdf.open.success fileName=$fileName")
        } catch (error: Throwable) {
            logger.log(Level.SEVERE, "invoice_pdf.open.failure fileName=$fileName", error)
            throw error
        }
    }

    private fun validate(input: CreateInvoiceInput) {
        require(input.clientId.isNotBlank()) { "Selecciona un cliente." }
        require(input.clientName.isNotBlank()) { "El cliente no tiene un nombre valido." }
        require(input.issuedAt.isNotBlank()) { "Indica la fecha de emision." }
        require(runCatching { LocalDate.parse(input.issuedAt) }.isSuccess) {
            "La fecha de emision debe usar el formato YYYY-MM-DD."
        }
        input.dueAt?.takeIf(String::isNotBlank)?.let { dueAt ->
            require(runCatching { LocalDate.parse(dueAt) }.isSuccess) {
                "La fecha de vencimiento debe usar el formato YYYY-MM-DD."
            }
        }
        require(input.items.isNotEmpty()) { "Selecciona al menos una tarea." }
        require(input.taxPercent.isFinite() && input.taxPercent in MIN_TAX_PERCENT..MAX_TAX_PERCENT) {
            "El impuesto debe estar entre 0 y 100."
        }
        input.items.forEach { item ->
            require(item.description.isNotBlank()) { "Todas las tareas deben tener una descripcion." }
            require(item.quantity.isFinite() && item.quantity > 0.0) { "Las horas deben ser mayores que cero." }
            require(item.unitPrice.isFinite() && item.unitPrice >= 0.0) { "El precio por hora no es valido." }
        }
    }

    private fun writeInvoicePdf(input: CreateInvoiceInput, invoiceNumber: String, target: Path) {
        val items = input.items.sortedBy { it.sortOrder }
        val subtotal = items.sumOf { item -> ceil(item.quantity) * item.unitPrice }
        val taxAmount = subtotal * input.taxPercent / PERCENT_DIVISOR
        val total = subtotal + taxAmount

        val lines = buildList {
            add(PdfLine("FACTURA", PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE, TITLE_LEADING))
            add(PdfLine(invoiceNumber, PDType1Font.HELVETICA_BOLD, SUBTITLE_FONT_SIZE, SECTION_LEADING))
            add(PdfLine("", leading = PARAGRAPH_GAP))
            add(PdfLine("Cliente: ${input.clientName}", PDType1Font.HELVETICA_BOLD))
            add(PdfLine("Fecha de emision: ${input.issuedAt}"))
            input.dueAt?.takeIf(String::isNotBlank)?.let { dueAt -> add(PdfLine("Fecha de vencimiento: $dueAt")) }
            add(PdfLine("", leading = PARAGRAPH_GAP))
            add(PdfLine("TAREAS FACTURADAS", PDType1Font.HELVETICA_BOLD, SECTION_FONT_SIZE, SECTION_LEADING))
            items.forEachIndexed { index, item ->
                val billedHours = ceil(item.quantity).toLong()
                add(PdfLine("${index + 1}. ${item.description}", PDType1Font.HELVETICA_BOLD))
                add(
                    PdfLine(
                        "   Horas: $billedHours h | Precio/hora: ${currency(item.unitPrice)} | " +
                            "Importe: ${currency(billedHours * item.unitPrice)}",
                    ),
                )
            }
            add(PdfLine("", leading = PARAGRAPH_GAP))
            add(PdfLine("Subtotal: ${currency(subtotal)}", PDType1Font.HELVETICA_BOLD))
            add(PdfLine("Impuesto (${money(input.taxPercent)}%): ${currency(taxAmount)}"))
            add(PdfLine("TOTAL: ${currency(total)}", PDType1Font.HELVETICA_BOLD, TOTAL_FONT_SIZE, TOTAL_LEADING))
            input.notes?.takeIf(String::isNotBlank)?.let { notes ->
                add(PdfLine("", leading = PARAGRAPH_GAP))
                add(PdfLine("NOTAS", PDType1Font.HELVETICA_BOLD, SECTION_FONT_SIZE, SECTION_LEADING))
                wrapText(notes, PDF_CHARACTERS_PER_LINE).forEach { line -> add(PdfLine(line)) }
            }
        }

        PDDocument().use { document ->
            var cursorY = 0f
            var content: PDPageContentStream? = null

            fun startPage(): PDPageContentStream {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                cursorY = page.mediaBox.height - PDF_TOP_MARGIN
                return PDPageContentStream(document, page)
            }

            try {
                content = startPage()
                lines.forEach { line ->
                    val wrappedLines = wrapText(line.text, line.maxCharacters).ifEmpty { listOf("") }
                    wrappedLines.forEach { text ->
                        if (cursorY - line.leading < PDF_BOTTOM_MARGIN) {
                            content?.close()
                            content = startPage()
                        }
                        if (text.isNotEmpty()) {
                            content?.beginText()
                            content?.setFont(line.font, line.fontSize)
                            content?.newLineAtOffset(PDF_LEFT_MARGIN, cursorY)
                            content?.showText(pdfSafeText(text))
                            content?.endText()
                        }
                        cursorY -= line.leading
                    }
                }
            } finally {
                content?.close()
            }
            document.save(target.toFile())
        }
    }

    private fun ensureInvoicesDirectory(): Path = invoicesDirectory
        .toAbsolutePath()
        .normalize()
        .also { directory -> Files.createDirectories(directory) }

    private fun invoiceFile(path: Path): InvoicePdfFile = InvoicePdfFile(
        fileName = path.fileName.toString(),
        sizeBytes = Files.size(path),
        lastModifiedEpochMillis = Files.getLastModifiedTime(path).toMillis(),
    )

    private fun saveAtomically(directory: Path, target: Path, write: (Path) -> Unit) {
        val temporary = Files.createTempFile(directory, ".invoice-", ".tmp")
        try {
            write(temporary)
            moveAtomically(temporary, target)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun availableTarget(directory: Path, requestedFileName: String): Path {
        val baseName = requestedFileName.removeSuffix(PDF_EXTENSION)
        var suffix = 1
        var candidate = directory.resolve(requestedFileName)
        while (Files.exists(candidate)) {
            candidate = directory.resolve("$baseName ($suffix)$PDF_EXTENSION")
            suffix += 1
        }
        return candidate
    }

    private fun pdfFileName(invoiceNumber: String, clientName: String): String {
        val clientSlug = Normalizer.normalize(clientName, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.')
            .take(MAX_CLIENT_FILE_NAME_LENGTH)
            .ifBlank { "cliente" }
        return "${invoiceNumber}_$clientSlug$PDF_EXTENSION"
    }

    private fun createInvoiceNumber(issuedAt: String): String {
        val year = LocalDate.parse(issuedAt).year.toString()
        val uniquePart = UUID.randomUUID().toString().take(INVOICE_ID_LENGTH).uppercase(Locale.ROOT)
        return "FAC-$year-$uniquePart"
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private fun wrapText(value: String, maxCharacters: Int): List<String> {
        val normalized = value.replace(Regex("[\\r\\n]+"), " ").trim()
        if (normalized.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var current = StringBuilder()
        normalized.split(Regex("\\s+")).forEach { word ->
            if (word.length > maxCharacters) {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    current = StringBuilder()
                }
                lines += word.chunked(maxCharacters)
            } else if (current.isEmpty()) {
                current.append(word)
            } else if (current.length + word.length + 1 <= maxCharacters) {
                current.append(' ').append(word)
            } else {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun pdfSafeText(value: String): String = buildString {
        value.replace(0x20AC.toChar().toString(), "EUR").forEach { character ->
            val supported = runCatching { PDType1Font.HELVETICA.encode(character.toString()) }.isSuccess
            append(if (supported) character else '?')
        }
    }

    private fun money(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

    private fun currency(value: Double): String = "\$${money(value)}"

    private data class PdfLine(
        val text: String,
        val font: PDFont = PDType1Font.HELVETICA,
        val fontSize: Float = BODY_FONT_SIZE,
        val leading: Float = BODY_LEADING,
        val maxCharacters: Int = PDF_CHARACTERS_PER_LINE,
    )

    private companion object {
        val logger: Logger = Logger.getLogger(DesktopInvoicePdfStorage::class.java.name)
        const val PDF_EXTENSION = ".pdf"
        const val PDF_LEFT_MARGIN = 56f
        const val PDF_TOP_MARGIN = 64f
        const val PDF_BOTTOM_MARGIN = 56f
        const val TITLE_FONT_SIZE = 22f
        const val SUBTITLE_FONT_SIZE = 14f
        const val SECTION_FONT_SIZE = 12f
        const val TOTAL_FONT_SIZE = 14f
        const val BODY_FONT_SIZE = 10f
        const val TITLE_LEADING = 30f
        const val SECTION_LEADING = 20f
        const val TOTAL_LEADING = 22f
        const val BODY_LEADING = 15f
        const val PARAGRAPH_GAP = 10f
        const val PDF_CHARACTERS_PER_LINE = 88
        const val MAX_CLIENT_FILE_NAME_LENGTH = 48
        const val INVOICE_ID_LENGTH = 8
        const val OPERATION_ID_LENGTH = 8
        const val PERCENT_DIVISOR = 100.0
        const val MIN_TAX_PERCENT = 0.0
        const val MAX_TAX_PERCENT = 100.0
    }
}

private fun defaultInvoicesDirectory(): Path {
    val homeDirectory = System.getenv("USERPROFILE")
        ?.takeIf { it.isNotBlank() }
        ?.let(Paths::get)
        ?: Paths.get(System.getProperty("user.home"))
    return homeDirectory.resolve("Desktop").resolve(INVOICES_DIRECTORY_NAME)
}

private const val INVOICES_DIRECTORY_NAME = "Facturas OryKai"
