package com.requena.supportdesk.features.invoices.data.storage

import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceInput
import com.requena.supportdesk.features.invoices.domain.model.CreateInvoiceItemInput
import com.requena.supportdesk.features.invoices.domain.model.InvoiceItemKind
import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoicePdfFile
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient
import com.requena.supportdesk.features.invoices.domain.model.InvoiceTotals
import com.requena.supportdesk.features.invoices.domain.model.invoiceLegalProblem
import com.requena.supportdesk.features.invoices.domain.model.normalizeTaxId
import com.requena.supportdesk.features.invoices.domain.model.calculateInvoiceLineAmounts
import com.requena.supportdesk.features.invoices.domain.model.calculateInvoiceTotals
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.awt.Color
import java.awt.Desktop
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

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

    override suspend fun saveInvoice(input: CreateInvoiceInput, invoiceNumber: String): InvoicePdfFile {
        val operationId = UUID.randomUUID().toString().take(OPERATION_ID_LENGTH)
        logger.info("invoice_pdf.create.start operationId=$operationId itemCount=${input.items.size}")

        return try {
            validate(input)
            val directory = ensureInvoicesDirectory()
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
                "Este equipo no puede abrir archivos PDF automáticamente."
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
        require(input.clientName.isNotBlank()) { "El cliente no tiene un nombre válido." }
        invoiceLegalProblem(input)?.let { problem -> throw IllegalArgumentException(problem) }
        require(input.issuedAt.isValidIsoDate()) { "La fecha de emisión debe usar el formato YYYY-MM-DD." }
        input.dueAt?.takeIf(String::isNotBlank)?.let { dueAt ->
            require(dueAt.isValidIsoDate()) { "La fecha de vencimiento debe usar el formato YYYY-MM-DD." }
            require(!LocalDate.parse(dueAt).isBefore(LocalDate.parse(input.issuedAt))) {
                "La fecha de vencimiento no puede ser anterior a la emisión."
            }
        }
        require(input.items.isNotEmpty()) { "Añade al menos una tarea o actividad." }
        require(input.taxPercent.isFinite() && input.taxPercent in MIN_TAX_PERCENT..MAX_TAX_PERCENT) {
            "El IVA debe estar entre 0 y 100."
        }
        input.items.forEach { item ->
            require(item.description.isNotBlank()) { "Todos los conceptos deben tener una descripción." }
            require(item.quantity.isFinite() && item.quantity > 0.0) { "Las cantidades deben ser mayores que cero." }
            require(item.unitPrice.isFinite() && item.unitPrice >= 0.0) { "El precio unitario no es válido." }
            if (item.kind == InvoiceItemKind.TASK_HOURS) {
                val quarterHours = item.quantity * 4.0
                require(quarterHours == kotlin.math.round(quarterHours)) {
                    "Las tareas deben facturarse en incrementos de un cuarto de hora."
                }
            }
        }
    }

    private fun writeInvoicePdf(input: CreateInvoiceInput, invoiceNumber: String, target: Path) {
        val items = input.items.sortedBy(CreateInvoiceItemInput::sortOrder)
        val totals = calculateInvoiceTotals(items, input.taxPercent, input.withholdingPercent)

        PDDocument().use { document ->
            fun startPage(): PDPageContentStream {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                return PDPageContentStream(document, page)
            }

            var content = startPage()
            var cursorY = PAGE_HEIGHT - PDF_TOP_MARGIN
            fun ensureSpace(needed: Float) {
                if (cursorY - needed < PDF_BOTTOM_MARGIN) {
                    content.close()
                    content = startPage()
                    cursorY = PAGE_HEIGHT - PDF_TOP_MARGIN
                }
            }
            try {
                cursorY = drawInvoiceHeader(content = content, input = input, invoiceNumber = invoiceNumber)
                drawTableHeader(content, cursorY)
                cursorY -= TABLE_HEADER_HEIGHT

                items.forEach { item ->
                    val row = invoiceTableRow(item, input.taxPercent)
                    if (cursorY - row.height < PDF_BOTTOM_MARGIN) {
                        ensureSpace(row.height)
                        drawTableHeader(content, cursorY)
                        cursorY -= TABLE_HEADER_HEIGHT
                    }
                    drawTableRow(content, top = cursorY, row = row)
                    cursorY -= row.height
                }

                val totalLines = totalLines(input, totals)
                ensureSpace(TOTALS_LINE_HEIGHT * totalLines.size + TOTALS_TOP_GAP)
                cursorY = drawTotals(content, top = cursorY - TOTALS_TOP_GAP, lines = totalLines)

                input.taxExemptionNote?.takeIf { input.taxPercent == 0.0 && it.isNotBlank() }?.let { note ->
                    val lines = wrapText(note, PDType1Font.HELVETICA, NOTES_FONT_SIZE, CONTENT_WIDTH)
                    ensureSpace(NOTES_GAP + NOTES_TITLE_LEADING + lines.size * NOTES_LEADING)
                    cursorY = drawParagraph(content, cursorY - NOTES_GAP, "MENCIÓN LEGAL DEL IVA", lines)
                }

                val paymentLines = paymentLines(input)
                if (paymentLines.isNotEmpty()) {
                    ensureSpace(NOTES_GAP + NOTES_TITLE_LEADING + paymentLines.size * NOTES_LEADING)
                    cursorY = drawParagraph(content, cursorY - NOTES_GAP, "FORMA DE PAGO", paymentLines)
                }

                input.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    val lines = wrapText(notes, PDType1Font.HELVETICA, NOTES_FONT_SIZE, CONTENT_WIDTH)
                    ensureSpace(NOTES_GAP + NOTES_TITLE_LEADING + lines.size * NOTES_LEADING)
                    cursorY = drawParagraph(content, cursorY - NOTES_GAP, "NOTAS", lines)
                }
            } finally {
                content.close()
            }
            document.save(target.toFile())
        }
    }

    /**
     * Issuer on the left, invoice title and dates on the right, then the full-width client
     * block. Returns the y where the concepts table starts.
     */
    private fun drawInvoiceHeader(
        content: PDPageContentStream,
        input: CreateInvoiceInput,
        invoiceNumber: String,
    ): Float {
        val top = PAGE_HEIGHT - PDF_TOP_MARGIN

        var issuerY = top
        input.issuer.visibleLines().forEachIndexed { index, line ->
            drawText(
                content = content,
                text = line,
                x = PDF_LEFT_MARGIN,
                y = issuerY,
                font = if (index == 0) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA,
                fontSize = if (index == 0) ISSUER_NAME_FONT_SIZE else ISSUER_FONT_SIZE,
                color = if (index == 0) TEXT_COLOR else DETAIL_COLOR,
            )
            issuerY -= if (index == 0) ISSUER_NAME_LEADING else ISSUER_LEADING
        }

        val rightEdge = PDF_LEFT_MARGIN + CONTENT_WIDTH
        drawTextRightAligned(content, "FACTURA", rightEdge, top - TITLE_FONT_SIZE + ISSUER_NAME_FONT_SIZE, PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE, ACCENT_COLOR)
        var metaY = top - TITLE_FONT_SIZE - TITLE_GAP
        buildList {
            add("N.º de factura" to invoiceNumber)
            add("Fecha de emisión" to input.issuedAt.toPdfDate())
            input.operationDate?.takeIf(String::isNotBlank)?.let { add("Fecha de operación" to it.toPdfDateOrSelf()) }
            input.dueAt?.takeIf(String::isNotBlank)?.let { add("Fecha de vencimiento" to it.toPdfDate()) }
            input.reference?.takeIf(String::isNotBlank)?.let { add("Referencia" to it) }
        }.forEach { (label, value) ->
            drawTextRightAligned(content, value, rightEdge, metaY, PDType1Font.HELVETICA_BOLD, VALUE_FONT_SIZE, TEXT_COLOR)
            val valueWidth = textWidth(value, PDType1Font.HELVETICA_BOLD, VALUE_FONT_SIZE)
            drawTextRightAligned(content, "$label:", rightEdge - valueWidth - META_LABEL_GAP, metaY, PDType1Font.HELVETICA, VALUE_FONT_SIZE, DETAIL_COLOR)
            metaY -= META_LEADING
        }

        val clientTop = minOf(issuerY, metaY) - HEADER_TO_CLIENT_GAP
        val clientBottom = drawClientBox(content, top = clientTop, recipient = input.recipient)
        return clientBottom - HEADER_TO_TABLE_GAP
    }

    private fun drawClientBox(content: PDPageContentStream, top: Float, recipient: InvoiceRecipient): Float {
        val lines = recipient.visibleLines()
        val height = CLIENT_LABEL_HEIGHT + CELL_PADDING + lines.size * CLIENT_LEADING + CELL_PADDING / 2
        val bottom = top - height
        fillRectangle(content, x = PDF_LEFT_MARGIN, y = top - CLIENT_LABEL_HEIGHT, width = CONTENT_WIDTH, height = CLIENT_LABEL_HEIGHT, color = HEADER_FILL_COLOR)
        drawBorder(content, x = PDF_LEFT_MARGIN, y = bottom, width = CONTENT_WIDTH, height = height)
        drawHorizontalLine(content, PDF_LEFT_MARGIN, top - CLIENT_LABEL_HEIGHT, PDF_LEFT_MARGIN + CONTENT_WIDTH)
        drawText(content, "CLIENTE", PDF_LEFT_MARGIN + CELL_PADDING, top - CLIENT_LABEL_TEXT_OFFSET, PDType1Font.HELVETICA_BOLD, LABEL_FONT_SIZE, TEXT_COLOR)
        var y = top - CLIENT_LABEL_HEIGHT - CELL_PADDING - CLIENT_NAME_FONT_SIZE + 2f
        lines.forEachIndexed { index, line ->
            drawText(
                content = content,
                text = line,
                x = PDF_LEFT_MARGIN + CELL_PADDING,
                y = y,
                font = if (index == 0) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA,
                fontSize = if (index == 0) CLIENT_NAME_FONT_SIZE else VALUE_FONT_SIZE,
                color = TEXT_COLOR,
            )
            y -= CLIENT_LEADING
        }
        return bottom
    }

    private fun drawTableHeader(content: PDPageContentStream, top: Float) {
        fillRectangle(
            content = content,
            x = PDF_LEFT_MARGIN,
            y = top - TABLE_HEADER_HEIGHT,
            width = CONTENT_WIDTH,
            height = TABLE_HEADER_HEIGHT,
            color = HEADER_FILL_COLOR,
        )
        drawBorder(content, PDF_LEFT_MARGIN, top - TABLE_HEADER_HEIGHT, CONTENT_WIDTH, TABLE_HEADER_HEIGHT)
        invoiceColumns.forEach { column ->
            drawText(
                content = content,
                text = column.label,
                x = if (column.alignRight) column.x + column.width - CELL_PADDING - textWidth(column.label, PDType1Font.HELVETICA_BOLD, TABLE_HEADER_FONT_SIZE) else column.x + CELL_PADDING,
                y = top - TABLE_HEADER_TEXT_OFFSET,
                font = PDType1Font.HELVETICA_BOLD,
                fontSize = TABLE_HEADER_FONT_SIZE,
                color = TEXT_COLOR,
            )
        }
    }

    private fun invoiceTableRow(item: CreateInvoiceItemInput, taxPercent: Double): InvoiceTableRow {
        val primaryLines = wrapText(item.description, PDType1Font.HELVETICA_BOLD, ROW_TITLE_FONT_SIZE, CONCEPT_TEXT_WIDTH)
        val detailLines = item.detail
            ?.takeIf(String::isNotBlank)
            ?.let { detail -> wrapText(detail, PDType1Font.HELVETICA, ROW_DETAIL_FONT_SIZE, CONCEPT_TEXT_WIDTH) }
            .orEmpty()
        val calculated = calculateInvoiceLineAmounts(item, taxPercent)
        val contentHeight = CELL_PADDING + primaryLines.size * ROW_TITLE_LEADING +
            detailLines.size * ROW_DETAIL_LEADING + CELL_PADDING
        return InvoiceTableRow(
            primaryLines = primaryLines,
            detailLines = detailLines,
            quantity = formatQuantity(item.quantity),
            quantityLabel = if (item.kind == InvoiceItemKind.TASK_HOURS) "horas" else "unidades",
            unitPrice = currency(item.unitPrice),
            taxPercent = "${decimal(taxPercent)}%",
            // Line amount is the taxable base; IVA and IRPF are shown once in the totals block.
            total = currency(calculated.subtotal),
            height = maxOf(MINIMUM_ROW_HEIGHT, contentHeight),
        )
    }

    private fun drawTableRow(content: PDPageContentStream, top: Float, row: InvoiceTableRow) {
        val bottom = top - row.height
        drawBorder(content, PDF_LEFT_MARGIN, bottom, CONTENT_WIDTH, row.height)
        invoiceColumns.drop(1).forEach { column ->
            drawVerticalLine(content, column.x, bottom, top)
        }

        var textY = top - CELL_PADDING - ROW_TITLE_FONT_SIZE
        row.primaryLines.forEach { line ->
            drawText(content, line, PDF_LEFT_MARGIN + CELL_PADDING, textY, PDType1Font.HELVETICA_BOLD, ROW_TITLE_FONT_SIZE, TEXT_COLOR)
            textY -= ROW_TITLE_LEADING
        }
        row.detailLines.forEach { line ->
            drawText(content, line, PDF_LEFT_MARGIN + CELL_PADDING, textY, PDType1Font.HELVETICA, ROW_DETAIL_FONT_SIZE, DETAIL_COLOR)
            textY -= ROW_DETAIL_LEADING
        }

        val valueY = top - CELL_PADDING - ROW_TITLE_FONT_SIZE
        drawRightAlignedText(content, row.quantity, invoiceColumns[1], valueY, PDType1Font.HELVETICA, ROW_TITLE_FONT_SIZE, TEXT_COLOR)
        drawRightAlignedText(content, row.quantityLabel, invoiceColumns[1], valueY - QUANTITY_LABEL_LEADING, PDType1Font.HELVETICA, QUANTITY_LABEL_FONT_SIZE, DETAIL_COLOR)
        drawRightAlignedText(content, row.unitPrice, invoiceColumns[2], valueY, PDType1Font.HELVETICA, ROW_TITLE_FONT_SIZE, TEXT_COLOR)
        drawRightAlignedText(content, row.taxPercent, invoiceColumns[3], valueY, PDType1Font.HELVETICA, ROW_TITLE_FONT_SIZE, TEXT_COLOR)
        drawRightAlignedText(content, row.total, invoiceColumns[4], valueY, PDType1Font.HELVETICA_BOLD, ROW_TITLE_FONT_SIZE, TEXT_COLOR)
    }

    private fun totalLines(input: CreateInvoiceInput, totals: InvoiceTotals): List<Pair<String, String>> = buildList {
        add("Base imponible" to currency(totals.subtotal))
        add("IVA (${decimal(input.taxPercent)}%)" to currency(totals.tax))
        if (input.withholdingPercent > 0.0) {
            add("Retención IRPF (${decimal(input.withholdingPercent)}%)" to "-${currency(totals.withholding)}")
        }
        add("TOTAL A PAGAR" to currency(totals.total))
    }

    /** The last line is the highlighted total. Returns the bottom of the block. */
    private fun drawTotals(content: PDPageContentStream, top: Float, lines: List<Pair<String, String>>): Float {
        val x = PDF_LEFT_MARGIN + CONTENT_WIDTH - TOTALS_BLOCK_WIDTH
        val height = TOTALS_LINE_HEIGHT * lines.size
        val bottom = top - height
        fillRectangle(content, x, bottom, TOTALS_BLOCK_WIDTH, TOTALS_LINE_HEIGHT, HEADER_FILL_COLOR)
        drawBorder(content, x, bottom, TOTALS_BLOCK_WIDTH, height)
        lines.forEachIndexed { index, (label, value) ->
            val lineTop = top - TOTALS_LINE_HEIGHT * index
            if (index > 0) drawHorizontalLine(content, x, lineTop, x + TOTALS_BLOCK_WIDTH)
            drawTotalLine(content, label, value, lineTop - TOTALS_LINE_TEXT_OFFSET, bold = index == lines.lastIndex)
        }
        return bottom
    }

    private fun paymentLines(input: CreateInvoiceInput): List<String> = buildList {
        input.issuer.paymentMethod.takeIf(String::isNotBlank)?.let(::add)
        input.issuer.iban.takeIf(String::isNotBlank)?.let { add("IBAN: ${formatIban(it)}") }
        input.dueAt?.takeIf(String::isNotBlank)?.let { add("Vencimiento: ${it.toPdfDate()}") }
    }

    private fun drawTotalLine(
        content: PDPageContentStream,
        label: String,
        value: String,
        y: Float,
        bold: Boolean = false,
    ) {
        val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
        drawText(content, label, PDF_LEFT_MARGIN + CONTENT_WIDTH - TOTALS_BLOCK_WIDTH + CELL_PADDING, y, font, TOTALS_FONT_SIZE, TEXT_COLOR)
        drawText(
            content = content,
            text = value,
            x = PDF_LEFT_MARGIN + CONTENT_WIDTH - CELL_PADDING - textWidth(value, font, TOTALS_FONT_SIZE),
            y = y,
            font = font,
            fontSize = TOTALS_FONT_SIZE,
            color = TEXT_COLOR,
        )
    }

    /** Small titled paragraph below the totals. Returns the y just under the last line. */
    private fun drawParagraph(content: PDPageContentStream, top: Float, title: String, lines: List<String>): Float {
        drawText(content, title, PDF_LEFT_MARGIN, top, PDType1Font.HELVETICA_BOLD, LABEL_FONT_SIZE, TEXT_COLOR)
        var y = top - NOTES_TITLE_LEADING
        lines.forEach { line ->
            drawText(content, line, PDF_LEFT_MARGIN, y, PDType1Font.HELVETICA, NOTES_FONT_SIZE, DETAIL_COLOR)
            y -= NOTES_LEADING
        }
        return y + NOTES_LEADING
    }

    private fun drawTextRightAligned(
        content: PDPageContentStream,
        text: String,
        rightX: Float,
        y: Float,
        font: PDFont,
        fontSize: Float,
        color: Color,
    ) = drawText(content, text, rightX - textWidth(text, font, fontSize), y, font, fontSize, color)

    private fun drawRightAlignedText(
        content: PDPageContentStream,
        text: String,
        column: InvoiceColumn,
        y: Float,
        font: PDFont,
        fontSize: Float,
        color: Color,
    ) {
        drawText(
            content = content,
            text = text,
            x = column.x + column.width - CELL_PADDING - textWidth(text, font, fontSize),
            y = y,
            font = font,
            fontSize = fontSize,
            color = color,
        )
    }

    private fun drawText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        font: PDFont,
        fontSize: Float,
        color: Color,
    ) {
        content.beginText()
        content.setNonStrokingColor(color)
        content.setFont(font, fontSize)
        content.newLineAtOffset(x, y)
        content.showText(pdfSafeText(text))
        content.endText()
    }

    private fun drawBorder(content: PDPageContentStream, x: Float, y: Float, width: Float, height: Float) {
        content.setStrokingColor(BORDER_COLOR)
        content.setLineWidth(BORDER_WIDTH)
        content.addRect(x, y, width, height)
        content.stroke()
    }

    private fun drawHorizontalLine(content: PDPageContentStream, xStart: Float, y: Float, xEnd: Float) {
        content.setStrokingColor(BORDER_COLOR)
        content.setLineWidth(BORDER_WIDTH)
        content.moveTo(xStart, y)
        content.lineTo(xEnd, y)
        content.stroke()
    }

    private fun drawVerticalLine(content: PDPageContentStream, x: Float, yStart: Float, yEnd: Float) {
        content.setStrokingColor(BORDER_COLOR)
        content.setLineWidth(BORDER_WIDTH)
        content.moveTo(x, yStart)
        content.lineTo(x, yEnd)
        content.stroke()
    }

    private fun fillRectangle(
        content: PDPageContentStream,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) {
        content.setNonStrokingColor(color)
        content.addRect(x, y, width, height)
        content.fill()
    }

    private fun wrapText(value: String, font: PDFont, fontSize: Float, maximumWidth: Float): List<String> {
        val normalized = pdfSafeText(value).replace(Regex("[\\r\\n]+"), " ").trim()
        if (normalized.isEmpty()) return emptyList()
        val words = normalized.split(Regex("\\s+"))

        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textWidth(candidate, font, fontSize) <= maximumWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = splitLongWord(word, font, fontSize, maximumWidth, lines)
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    private fun splitLongWord(
        word: String,
        font: PDFont,
        fontSize: Float,
        maximumWidth: Float,
        lines: MutableList<String>,
    ): String {
        if (textWidth(word, font, fontSize) <= maximumWidth) return word
        var pending = ""
        word.forEach { character ->
            val candidate = pending + character
            if (textWidth(candidate, font, fontSize) <= maximumWidth) {
                pending = candidate
            } else {
                lines += pending
                pending = character.toString()
            }
        }
        return pending
    }

    private fun textWidth(text: String, font: PDFont, fontSize: Float): Float =
        font.getStringWidth(pdfSafeText(text)) / 1_000f * fontSize

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

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private data class InvoiceColumn(
        val label: String,
        val x: Float,
        val width: Float,
        val alignRight: Boolean,
    )

    private data class InvoiceTableRow(
        val primaryLines: List<String>,
        val detailLines: List<String>,
        val quantity: String,
        val quantityLabel: String,
        val unitPrice: String,
        val taxPercent: String,
        val total: String,
        val height: Float,
    )

    private companion object {
        val logger: Logger = Logger.getLogger(DesktopInvoicePdfStorage::class.java.name)
        val invoiceColumns = listOf(
            InvoiceColumn("Conceptos", PDF_LEFT_MARGIN, CONCEPT_COLUMN_WIDTH, alignRight = false),
            InvoiceColumn("Cant.", PDF_LEFT_MARGIN + CONCEPT_COLUMN_WIDTH, QUANTITY_COLUMN_WIDTH, alignRight = true),
            InvoiceColumn("Precio uni.", PDF_LEFT_MARGIN + CONCEPT_COLUMN_WIDTH + QUANTITY_COLUMN_WIDTH, UNIT_PRICE_COLUMN_WIDTH, alignRight = true),
            InvoiceColumn("IVA", PDF_LEFT_MARGIN + CONCEPT_COLUMN_WIDTH + QUANTITY_COLUMN_WIDTH + UNIT_PRICE_COLUMN_WIDTH, TAX_COLUMN_WIDTH, alignRight = true),
            InvoiceColumn("Importe", PDF_LEFT_MARGIN + CONCEPT_COLUMN_WIDTH + QUANTITY_COLUMN_WIDTH + UNIT_PRICE_COLUMN_WIDTH + TAX_COLUMN_WIDTH, TOTAL_COLUMN_WIDTH, alignRight = true),
        )
        const val PDF_EXTENSION = ".pdf"
        const val PAGE_HEIGHT = 841.89f
        const val PDF_LEFT_MARGIN = 56f
        const val PDF_TOP_MARGIN = 64f
        const val PDF_BOTTOM_MARGIN = 56f
        const val CONTENT_WIDTH = 483f
        const val BORDER_WIDTH = 0.8f
        const val CELL_PADDING = 10f
        const val ISSUER_NAME_FONT_SIZE = 13f
        const val ISSUER_FONT_SIZE = 10f
        const val ISSUER_NAME_LEADING = 19f
        const val ISSUER_LEADING = 15f
        const val TITLE_FONT_SIZE = 24f
        const val TITLE_GAP = 14f
        const val META_LEADING = 15f
        const val META_LABEL_GAP = 4f
        const val HEADER_TO_CLIENT_GAP = 22f
        const val CLIENT_LABEL_HEIGHT = 22f
        const val CLIENT_LABEL_TEXT_OFFSET = 15f
        const val CLIENT_NAME_FONT_SIZE = 11f
        const val CLIENT_LEADING = 14f
        const val HEADER_TO_TABLE_GAP = 18f
        const val LABEL_FONT_SIZE = 8.5f
        const val VALUE_FONT_SIZE = 10f
        const val TABLE_HEADER_HEIGHT = 28f
        const val TABLE_HEADER_FONT_SIZE = 8.5f
        const val TABLE_HEADER_TEXT_OFFSET = 18f
        const val CONCEPT_COLUMN_WIDTH = 245f
        const val QUANTITY_COLUMN_WIDTH = 48f
        const val UNIT_PRICE_COLUMN_WIDTH = 72f
        const val TAX_COLUMN_WIDTH = 43f
        const val TOTAL_COLUMN_WIDTH = 75f
        const val CONCEPT_TEXT_WIDTH = CONCEPT_COLUMN_WIDTH - CELL_PADDING * 2
        const val ROW_TITLE_FONT_SIZE = 9.5f
        const val ROW_DETAIL_FONT_SIZE = 8.5f
        const val ROW_TITLE_LEADING = 13f
        const val ROW_DETAIL_LEADING = 11f
        const val QUANTITY_LABEL_FONT_SIZE = 7.5f
        const val QUANTITY_LABEL_LEADING = 11f
        const val MINIMUM_ROW_HEIGHT = 54f
        const val TOTALS_BLOCK_WIDTH = 205f
        const val TOTALS_TOP_GAP = 12f
        const val TOTALS_LINE_HEIGHT = 22f
        const val TOTALS_LINE_TEXT_OFFSET = 14f
        const val TOTALS_FONT_SIZE = 9.5f
        const val NOTES_GAP = 24f
        const val NOTES_TITLE_LEADING = 15f
        const val NOTES_FONT_SIZE = 8.5f
        const val NOTES_LEADING = 11f
        const val MAX_CLIENT_FILE_NAME_LENGTH = 48
        const val OPERATION_ID_LENGTH = 8
        const val MIN_TAX_PERCENT = 0.0
        const val MAX_TAX_PERCENT = 100.0
        val TEXT_COLOR = Color(57, 59, 64)
        val DETAIL_COLOR = Color(116, 116, 116)
        val BORDER_COLOR = Color(140, 149, 171)
        val HEADER_FILL_COLOR = Color(244, 245, 248)
        val ACCENT_COLOR = Color(32, 44, 74)
    }
}


private fun String.isValidIsoDate(): Boolean = runCatching { LocalDate.parse(this) }.isSuccess

private fun String.toPdfDate(): String = LocalDate.parse(this).format(PDF_DATE_FORMATTER)

private fun formatQuantity(value: Double): String = decimal(value)

private fun decimal(value: Double): String = String.format(Locale.ROOT, "%.2f", value).replace('.', ',')

private fun currency(value: Double): String = "${decimal(value)} EUR"

private fun pdfSafeText(value: String): String = buildString {
    value.replace(0x20AC.toChar().toString(), "EUR").forEach { character ->
        val supported = runCatching { PDType1Font.HELVETICA.encode(character.toString()) }.isSuccess
        append(if (supported) character else '?')
    }
}

private fun defaultInvoicesDirectory(): Path {
    val homeDirectory = System.getenv("USERPROFILE")
        ?.takeIf { it.isNotBlank() }
        ?.let(Paths::get)
        ?: Paths.get(System.getProperty("user.home"))
    return homeDirectory.resolve("Desktop").resolve(INVOICES_DIRECTORY_NAME)
}

private val PDF_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
private const val INVOICES_DIRECTORY_NAME = "Facturas OryKai"

private fun InvoiceIssuer.visibleLines(): List<String> = buildList {
    add(fullName)
    tradeName.takeIf(String::isNotBlank)?.let(::add)
    add("NIF: ${normalizeTaxId(taxId)}")
    add(addressLine)
    add(postalCodeCityLine(postalCode, city, province))
    country.takeIf { it.isNotBlank() && !it.equals(InvoiceIssuer.DEFAULT_COUNTRY, ignoreCase = true) }?.let(::add)
    listOf(email, phone.takeIf(String::isNotBlank)?.let { "Tel. $it" }.orEmpty())
        .filter(String::isNotBlank)
        .takeIf(List<String>::isNotEmpty)
        ?.let { add(it.joinToString("  ·  ")) }
}

private fun InvoiceRecipient.visibleLines(): List<String> = buildList {
    add(legalName)
    add("NIF/CIF: ${normalizeTaxId(taxId)}")
    add(addressLine)
    postalCodeCityLine(postalCode, city, province).takeIf(String::isNotBlank)?.let(::add)
    country.takeIf { it.isNotBlank() && !it.equals(InvoiceIssuer.DEFAULT_COUNTRY, ignoreCase = true) }?.let(::add)
    email.takeIf(String::isNotBlank)?.let(::add)
}

private fun postalCodeCityLine(postalCode: String, city: String, province: String): String {
    val base = listOf(postalCode, city).filter(String::isNotBlank).joinToString(" ")
    return if (province.isNotBlank() && !province.equals(city, ignoreCase = true)) "$base ($province)" else base
}

private fun formatIban(iban: String): String = normalizeTaxId(iban).chunked(4).joinToString(" ")

/** Operation date may be an ISO date or free text such as "Octubre 2026". */
private fun String.toPdfDateOrSelf(): String = runCatching { LocalDate.parse(this).format(PDF_DATE_FORMATTER) }.getOrDefault(this)
