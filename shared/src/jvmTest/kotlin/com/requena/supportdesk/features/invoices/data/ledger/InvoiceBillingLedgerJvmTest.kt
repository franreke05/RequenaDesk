package com.requena.supportdesk.features.invoices.data.ledger

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoiceBillingLedgerJvmTest {
    @Test
    fun issuingMarksUnitsBilledAndPersistsAcrossInstances() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            val number = ledger.issueInvoice("2026-07-15", listOf("log-1", "log-2"))
            assertEquals(setOf("log-1", "log-2"), ledger.billedLogIds())

            // A fresh instance (simulating an app restart) must see the same durable state.
            val reopened = DesktopInvoiceBillingLedger(file)
            assertEquals(setOf("log-1", "log-2"), reopened.billedLogIds())
            assertTrue(reopened.isIssuedAndNotVoided(number))
        }
    }

    @Test
    fun numbersAreCorrelativePerYearAndContinueAfterARestart() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            assertEquals("FAC-2026-0001", ledger.issueInvoice("2026-07-15", emptyList()))
            assertEquals("FAC-2026-0002", ledger.issueInvoice("2026-07-15", listOf("log-1")))
            assertEquals("FAC-2026-0003", DesktopInvoiceBillingLedger(file).issueInvoice("2026-08-01", emptyList()))
            // A new year starts a new series.
            assertEquals("FAC-2027-0001", ledger.issueInvoice("2027-01-02", emptyList()))
        }
    }

    @Test
    fun voidedNumbersAreNeverReused() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            val first = ledger.issueInvoice("2026-07-15", emptyList())
            ledger.recordVoided(first, reason = "Error")
            assertEquals("FAC-2026-0002", ledger.issueInvoice("2026-07-15", emptyList()))
        }
    }

    @Test
    fun anIssueDateEarlierThanTheLastInvoiceOfTheSeriesIsRejected() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            ledger.issueInvoice("2026-07-15", emptyList())
            assertFailsWith<InvoiceNumberingException> { ledger.issueInvoice("2026-07-10", emptyList()) }
            assertEquals("FAC-2026-0002", ledger.issueInvoice("2026-07-15", emptyList()))
        }
    }

    @Test
    fun existingPdfsInTheFolderKeepTheSeriesGoingWhenTheLedgerIsMissing() = runBlocking {
        withTempLedgerFile { file ->
            Files.createFile(file.parent.resolve("FAC-2026-0007_Cliente.pdf"))
            // Old random numbers must not be mistaken for correlatives.
            Files.createFile(file.parent.resolve("FAC-2026-12345678_Cliente.pdf"))
            assertEquals("FAC-2026-0008", DesktopInvoiceBillingLedger(file).issueInvoice("2026-07-15", emptyList()))
        }
    }

    @Test
    fun billingTheSameUnitTwiceIsRejectedAndUsesNoNumber() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            ledger.issueInvoice("2026-07-15", listOf("log-1"))

            assertFailsWith<DuplicateBillingException> {
                ledger.issueInvoice("2026-07-15", listOf("log-1", "log-2"))
            }
            // The failed attempt must not have partially applied - log-2 stays unbilled.
            assertEquals(setOf("log-1"), ledger.billedLogIds())
            assertEquals("FAC-2026-0002", ledger.issueInvoice("2026-07-15", emptyList()))
        }
    }

    @Test
    fun voidingReleasesItsUnitsAndTheyCanBeBilledAgain() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            val number = ledger.issueInvoice("2026-07-15", listOf("log-1", "log-2"))

            val released = ledger.recordVoided(number, reason = "Error en el importe")

            assertEquals(setOf("log-1", "log-2"), released.toSet())
            assertTrue(ledger.billedLogIds().isEmpty())
            assertFalse(ledger.isIssuedAndNotVoided(number))
            assertEquals(setOf(number), ledger.voidedInvoiceNumbers())

            // Once voided, the same units are billable again under a new invoice.
            ledger.issueInvoice("2026-07-16", listOf("log-1", "log-2"))
            assertEquals(setOf("log-1", "log-2"), ledger.billedLogIds())
        }
    }

    @Test
    fun voidingTwiceIsIdempotentAndDoesNotDoubleReleaseOrThrow() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            val number = ledger.issueInvoice("2026-07-15", listOf("log-1"))

            val firstVoid = ledger.recordVoided(number, reason = null)
            val secondVoid = ledger.recordVoided(number, reason = null)

            assertEquals(listOf("log-1"), firstVoid)
            assertTrue(secondVoid.isEmpty())
        }
    }

    @Test
    fun voidingAnInvoiceThatWasNeverIssuedReleasesNothing() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            assertTrue(ledger.recordVoided("FAC-UNKNOWN", reason = null).isEmpty())
        }
    }

    private fun withTempLedgerFile(block: suspend (Path) -> Unit) = runBlocking {
        val directory = Files.createTempDirectory("invoice-ledger-test")
        val file = directory.resolve("ledger.jsonl")
        try {
            block(file)
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
