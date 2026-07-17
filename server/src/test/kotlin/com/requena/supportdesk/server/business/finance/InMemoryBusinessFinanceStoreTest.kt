package com.requena.supportdesk.server.business.finance

import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceCalculator
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InMemoryBusinessFinanceStoreTest {
    private val ids = listOf("doc-a", "entry-a", "entry-b").iterator()
    private val store = InMemoryBusinessFinanceStore { ids.next() }

    @Test
    fun `sales documents stay isolated to their client`() {
        val draft = BusinessFinanceCalculator.calculateSalesDocument(
            SalesDocumentDraftInput(
                issuerName = "Emisor",
                customerName = "Comprador",
                issueDate = "2026-07-17",
                lines = listOf(SalesLineInput("Servicio", 1_000, 500)),
            ),
        )
        store.createSalesDocument("client-a", "user-a", draft)

        assertEquals(1, store.listSalesDocuments("client-a").size)
        assertEquals(emptyList(), store.listSalesDocuments("client-b"))
        assertFailsWith<BusinessFinanceNotFoundException> {
            store.archiveSalesDocument("client-b", "user-b", "doc-a", 1)
        }
    }

    @Test
    fun `recorded entry can be voided but stale edit conflicts`() {
        val entry = store.createFinanceEntry(
            "client-a",
            "user-a",
            BusinessFinanceCalculator.calculateFinanceEntry(
                FinanceEntryInput(BusinessFinanceDirection.EXPENSE, "2026-07-17", "Herramienta", 1000),
            ),
        )
        val recorded = store.recordFinanceEntry("client-a", "user-a", entry.id, entry.version)
        assertEquals(BusinessFinanceEntryStatus.RECORDED, recorded.status)
        assertFailsWith<BusinessFinanceConflictException> {
            store.recordFinanceEntry("client-a", "user-a", entry.id, entry.version)
        }
        val voided = store.voidFinanceEntry("client-a", "user-a", entry.id, recorded.version, "Duplicado")
        assertEquals(BusinessFinanceEntryStatus.VOID, voided.status)
        assertEquals(0, store.financeOverview("client-a", "2026-07").expenseCents)
    }
}
