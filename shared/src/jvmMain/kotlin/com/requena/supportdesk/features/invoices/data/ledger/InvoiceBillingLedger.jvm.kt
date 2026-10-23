package com.requena.supportdesk.features.invoices.data.ledger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun createInvoiceBillingLedger(): InvoiceBillingLedger = DesktopInvoiceBillingLedger()

@Serializable
private data class LedgerLine(
    val type: String,
    val invoiceNumber: String,
    val logIds: List<String> = emptyList(),
    val reason: String? = null,
    /** Issue date printed on the invoice (YYYY-MM-DD); null on entries written before numbering was correlative. */
    val issuedAt: String? = null,
    val timestamp: String,
)

internal class DesktopInvoiceBillingLedger(
    private val ledgerFile: Path = defaultLedgerFile(),
) : InvoiceBillingLedger {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun billedLogIds(): Set<String> = mutex.withLock { billedLogIdsLocked() }

    override suspend fun issueInvoice(issuedAt: String, logIds: List<String>): String = mutex.withLock {
        val issueDate = runCatching { LocalDate.parse(issuedAt) }.getOrElse {
            throw InvoiceNumberingException("La fecha de emisión debe usar el formato YYYY-MM-DD.")
        }
        val entries = readEntries()

        val alreadyBilled = billedLogIdsLocked(entries)
        val duplicates = logIds.filter { it in alreadyBilled }
        if (duplicates.isNotEmpty()) {
            throw DuplicateBillingException(
                "Alguna de las unidades de tiempo seleccionadas ya esta facturada (ids: ${duplicates.joinToString()}).",
            )
        }

        val year = issueDate.year
        val seriesEntries = entries.filter { it.type == TYPE_ISSUED && correlativeOf(it.invoiceNumber, year) != null }
        val lastDated = seriesEntries.filter { it.issuedAt != null }.maxByOrNull { correlativeOf(it.invoiceNumber, year)!! }
        lastDated?.issuedAt?.let(LocalDate::parse)?.takeIf(issueDate::isBefore)?.let { lastDate ->
            throw InvoiceNumberingException(
                "La fecha de emisión no puede ser anterior a la de la última factura " +
                    "(${lastDated.invoiceNumber}, ${lastDate.format(DISPLAY_DATE)}). La numeración debe seguir el orden de fechas.",
            )
        }

        val lastNumber = (seriesEntries.mapNotNull { correlativeOf(it.invoiceNumber, year) } + existingPdfNumbers(year))
            .maxOrNull() ?: 0
        val invoiceNumber = formatInvoiceNumber(year, lastNumber + 1)
        appendLine(
            LedgerLine(
                type = TYPE_ISSUED,
                invoiceNumber = invoiceNumber,
                logIds = logIds,
                issuedAt = issuedAt,
                timestamp = Instant.now().toString(),
            ),
        )
        invoiceNumber
    }

    /** PDFs already in the folder also count, so the series never restarts if the ledger is lost. */
    private fun existingPdfNumbers(year: Int): List<Int> {
        val directory = ledgerFile.toAbsolutePath().parent ?: return emptyList()
        if (!Files.isDirectory(directory)) return emptyList()
        return Files.list(directory).use { paths ->
            paths.iterator().asSequence()
                .mapNotNull { correlativeOf(it.fileName.toString().substringBefore("_").removeSuffix(".pdf"), year) }
                .toList()
        }
    }

    override suspend fun recordVoided(invoiceNumber: String, reason: String?): List<String> = mutex.withLock {
        val entries = readEntries()
        val alreadyVoided = entries.any { it.type == TYPE_VOIDED && it.invoiceNumber == invoiceNumber }
        if (alreadyVoided) return@withLock emptyList()
        val issued = entries.lastOrNull { it.type == TYPE_ISSUED && it.invoiceNumber == invoiceNumber }
            ?: return@withLock emptyList()
        appendLine(
            LedgerLine(
                type = TYPE_VOIDED,
                invoiceNumber = invoiceNumber,
                reason = reason,
                timestamp = Instant.now().toString(),
            ),
        )
        issued.logIds
    }

    override suspend fun isIssuedAndNotVoided(invoiceNumber: String): Boolean = mutex.withLock {
        val entries = readEntries()
        val issued = entries.any { it.type == TYPE_ISSUED && it.invoiceNumber == invoiceNumber }
        val voided = entries.any { it.type == TYPE_VOIDED && it.invoiceNumber == invoiceNumber }
        issued && !voided
    }

    override suspend fun voidedInvoiceNumbers(): Set<String> = mutex.withLock {
        readEntries().filter { it.type == TYPE_VOIDED }.mapTo(mutableSetOf()) { it.invoiceNumber }
    }

    private fun billedLogIdsLocked(entries: List<LedgerLine> = readEntries()): Set<String> {
        val voidedInvoiceNumbers = entries.filter { it.type == TYPE_VOIDED }.mapTo(mutableSetOf()) { it.invoiceNumber }
        return entries.asSequence()
            .filter { it.type == TYPE_ISSUED && it.invoiceNumber !in voidedInvoiceNumbers }
            .flatMap { it.logIds }
            .toSet()
    }

    private fun readEntries(): List<LedgerLine> {
        if (!Files.exists(ledgerFile)) return emptyList()
        return Files.readAllLines(ledgerFile)
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> runCatching { json.decodeFromString<LedgerLine>(line) }.getOrNull() }
            .toList()
    }

    private fun appendLine(entry: LedgerLine) {
        Files.createDirectories(ledgerFile.parent)
        Files.write(
            ledgerFile,
            listOf(json.encodeToString(LedgerLine.serializer(), entry)),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    private companion object {
        const val TYPE_ISSUED = "ISSUED"
        const val TYPE_VOIDED = "VOIDED"
        val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
    }
}

/** Correlative part of a `FAC-<year>-<0001>` number, or null for other years or legacy random numbers. */
internal fun correlativeOf(invoiceNumber: String, year: Int): Int? =
    CORRELATIVE_NUMBER.matchEntire(invoiceNumber)
        ?.takeIf { it.groupValues[1].toInt() == year }
        ?.groupValues?.get(2)?.toInt()

internal fun formatInvoiceNumber(year: Int, correlative: Int): String =
    "FAC-$year-${correlative.toString().padStart(CORRELATIVE_DIGITS, '0')}"

// 4-6 digits: the old random numbers had 8 characters, so they never match.
private val CORRELATIVE_NUMBER = Regex("^FAC-(\\d{4})-(\\d{4,6})$")
private const val CORRELATIVE_DIGITS = 4

private fun defaultLedgerFile(): Path {
    val homeDirectory = System.getenv("USERPROFILE")
        ?.takeIf { it.isNotBlank() }
        ?.let(Paths::get)
        ?: Paths.get(System.getProperty("user.home"))
    return homeDirectory.resolve("Desktop").resolve("Facturas OryKai").resolve(".facturacion-ledger.jsonl")
}
