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
import java.util.Locale
import java.util.UUID

actual fun createInvoiceBillingLedger(): InvoiceBillingLedger = DesktopInvoiceBillingLedger()

@Serializable
private data class LedgerLine(
    val type: String,
    val invoiceNumber: String,
    val logIds: List<String> = emptyList(),
    val reason: String? = null,
    val timestamp: String,
)

internal class DesktopInvoiceBillingLedger(
    private val ledgerFile: Path = defaultLedgerFile(),
) : InvoiceBillingLedger {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override fun nextInvoiceNumber(issuedAt: String): String {
        val year = runCatching { LocalDate.parse(issuedAt).year }.getOrElse { LocalDate.now().year }
        val uniquePart = UUID.randomUUID().toString().take(INVOICE_ID_LENGTH).uppercase(Locale.ROOT)
        return "FAC-$year-$uniquePart"
    }

    override suspend fun billedLogIds(): Set<String> = mutex.withLock { billedLogIdsLocked() }

    override suspend fun recordIssued(invoiceNumber: String, logIds: List<String>) {
        if (logIds.isEmpty()) return
        mutex.withLock {
            val alreadyBilled = billedLogIdsLocked()
            val duplicates = logIds.filter { it in alreadyBilled }
            if (duplicates.isNotEmpty()) {
                throw DuplicateBillingException(
                    "Alguna de las unidades de tiempo seleccionadas ya esta facturada (ids: ${duplicates.joinToString()}).",
                )
            }
            appendLine(LedgerLine(type = TYPE_ISSUED, invoiceNumber = invoiceNumber, logIds = logIds, timestamp = Instant.now().toString()))
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

    private fun billedLogIdsLocked(): Set<String> {
        val entries = readEntries()
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
        const val INVOICE_ID_LENGTH = 8
    }
}

private fun defaultLedgerFile(): Path {
    val homeDirectory = System.getenv("USERPROFILE")
        ?.takeIf { it.isNotBlank() }
        ?.let(Paths::get)
        ?: Paths.get(System.getProperty("user.home"))
    return homeDirectory.resolve("Desktop").resolve("Facturas OryKai").resolve(".facturacion-ledger.jsonl")
}
