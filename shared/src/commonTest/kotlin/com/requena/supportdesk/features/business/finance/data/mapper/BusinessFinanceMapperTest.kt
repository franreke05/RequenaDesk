package com.requena.supportdesk.features.business.finance.data.mapper

import com.requena.supportdesk.features.business.finance.data.dto.BusinessFinanceEntryDto
import com.requena.supportdesk.features.business.finance.data.dto.BusinessSalesDocumentDto
import com.requena.supportdesk.features.business.finance.data.dto.CalculatedSalesLineDto
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceDirection
import com.requena.supportdesk.features.business.finance.domain.BusinessFinanceEntryStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessPaymentStatus
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentKind
import com.requena.supportdesk.features.business.finance.domain.BusinessSalesDocumentStatus
import com.requena.supportdesk.features.business.finance.domain.FinanceEntryInput
import com.requena.supportdesk.features.business.finance.domain.SalesDocumentDraftInput
import com.requena.supportdesk.features.business.finance.domain.SalesLineInput
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessFinanceMapperTest {
    @Test
    fun `sales document request preserves pricing inputs`() {
        val request = BusinessFinanceMapper.salesDocumentRequest(
            SalesDocumentDraftInput(
                kind = BusinessSalesDocumentKind.PROFORMA,
                issuerName = "Estudio Norte",
                customerName = "Cliente Uno",
                issueDate = "2026-07-17",
                dueDate = "2026-08-17",
                notes = "Trabajo mensual",
                lines = listOf(SalesLineInput("ConsultorÃ­a", 1_500, 2_000, 2_100, 500)),
            ),
        )

        assertEquals(BusinessSalesDocumentKind.PROFORMA, request.kind)
        assertEquals("Estudio Norte", request.issuerName)
        assertEquals(1_500, request.lines.single().quantityMilli)
        assertEquals(2_100, request.lines.single().taxRateBasisPoints)
        assertEquals(500, request.lines.single().discountBasisPoints)
    }

    @Test
    fun `transport responses map to domain models without leaking dto fields`() {
        val document = BusinessFinanceMapper.salesDocument(
            BusinessSalesDocumentDto(
                id = "document-1",
                kind = BusinessSalesDocumentKind.DRAFT_INVOICE,
                status = BusinessSalesDocumentStatus.DRAFT,
                issuerName = "Estudio Norte",
                customerName = "Cliente Uno",
                issueDate = "2026-07-17",
                subtotalCents = 2_000,
                taxCents = 420,
                totalCents = 2_420,
                version = 3,
                lines = listOf(
                    CalculatedSalesLineDto("ConsultorÃ­a", 1_000, 2_000, 2_100, 0, 2_000, 420, 2_420),
                ),
            ),
        )
        val entry = BusinessFinanceMapper.financeEntry(
            BusinessFinanceEntryDto(
                id = "entry-1",
                direction = BusinessFinanceDirection.EXPENSE,
                status = BusinessFinanceEntryStatus.RECORDED,
                occurredOn = "2026-07-17",
                description = "SuscripciÃ³n",
                netCents = 1_000,
                taxRateBasisPoints = 2_100,
                taxCents = 210,
                grossCents = 1_210,
                paymentStatus = BusinessPaymentStatus.PAID,
                version = 2,
            ),
        )

        assertEquals(2_420, document.totalCents)
        assertEquals(3, document.version)
        assertEquals("ConsultorÃ­a", document.lines.single().description)
        assertEquals(BusinessFinanceEntryStatus.RECORDED, entry.status)
        assertEquals(1_210, entry.grossCents)

        val entryRequest = BusinessFinanceMapper.financeEntryRequest(
            FinanceEntryInput(
                direction = BusinessFinanceDirection.INCOME,
                occurredOn = "2026-07-17",
                description = "Servicio",
                netCents = 1_000,
                paymentStatus = BusinessPaymentStatus.PAID,
            ),
        )
        assertEquals(BusinessPaymentStatus.PAID, entryRequest.paymentStatus)
    }
}
