package com.requena.supportdesk.features.invoices.data.ledger

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
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
            ledger.recordIssued("FAC-2026-A", listOf("log-1", "log-2"))
            assertEquals(setOf("log-1", "log-2"), ledger.billedLogIds())

            // A fresh instance (simulating an app restart) must see the same durable state.
            val reopened = DesktopInvoiceBillingLedger(file)
            assertEquals(setOf("log-1", "log-2"), reopened.billedLogIds())
            assertTrue(reopened.isIssuedAndNotVoided("FAC-2026-A"))
        }
    }

    @Test
    fun recordingTheSameUnitTwiceIsRejectedEvenUnderADifferentInvoiceNumber() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            ledger.recordIssued("FAC-2026-A", listOf("log-1"))

            assertFailsWith<DuplicateBillingException> {
                ledger.recordIssued("FAC-2026-B", listOf("log-1", "log-2"))
            }
            // The failed attempt must not have partially applied - log-2 stays unbilled.
            assertEquals(setOf("log-1"), ledger.billedLogIds())
        }
    }

    @Test
    fun voidingReleasesItsUnitsAndTheyCanBeBilledAgain() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            ledger.recordIssued("FAC-2026-A", listOf("log-1", "log-2"))

            val released = ledger.recordVoided("FAC-2026-A", reason = "Error en el importe")

            assertEquals(setOf("log-1", "log-2"), released.toSet())
            assertTrue(ledger.billedLogIds().isEmpty())
            assertFalse(ledger.isIssuedAndNotVoided("FAC-2026-A"))
            assertEquals(setOf("FAC-2026-A"), ledger.voidedInvoiceNumbers())

            // Once voided, the same units are billable again under a new invoice.
            ledger.recordIssued("FAC-2026-C", listOf("log-1", "log-2"))
            assertEquals(setOf("log-1", "log-2"), ledger.billedLogIds())
        }
    }

    @Test
    fun voidingTwiceIsIdempotentAndDoesNotDoubleReleaseOrThrow() = runBlocking {
        withTempLedgerFile { file ->
            val ledger = DesktopInvoiceBillingLedger(file)
            ledger.recordIssued("FAC-2026-A", listOf("log-1"))

            val firstVoid = ledger.recordVoided("FAC-2026-A", reason = null)
            val secondVoid = ledger.recordVoided("FAC-2026-A", reason = null)

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

    @Test
    fun invoiceNumbersAreUniqueAndCarryTheIssuedYear() {
        val ledger = DesktopInvoiceBillingLedger(Files.createTempFile("ledger-unused", ".jsonl"))
        val a = ledger.nextInvoiceNumber("2026-07-15")
        val b = ledger.nextInvoiceNumber("2026-07-15")
        assertTrue(a.startsWith("FAC-2026-"))
        assertTrue(a != b)
    }

    private fun withTempLedgerFile(block: suspend (java.nio.file.Path) -> Unit) = runBlocking {
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
