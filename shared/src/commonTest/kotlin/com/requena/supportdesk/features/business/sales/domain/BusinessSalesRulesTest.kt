package com.requena.supportdesk.features.business.sales.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BusinessSalesRulesTest {
    @Test
    fun quoteTotalsAreCalculatedFromSnapshotsUsingIntegerMath() {
        val calculated = BusinessSalesRules.calculateQuote(
            CreateBusinessQuoteInput(
                buyerName = "Empresa compradora",
                issueDate = "2026-07-17",
                idempotencyKey = "quote-test-0001",
                lines = listOf(
                    BusinessQuoteLineInput(
                        position = 1,
                        description = "Servicio",
                        quantityMilli = 1_500,
                        unitPriceCents = 101,
                        discountBasisPoints = 1_000,
                        taxBasisPoints = 2_100,
                    ),
                ),
            ),
        )

        assertEquals(137, calculated.subtotalCents)
        assertEquals(29, calculated.taxCents)
        assertEquals(166, calculated.totalCents)
    }

    @Test
    fun quoteRejectsDuplicatePositionsAndInvalidDates() {
        assertFailsWith<BusinessSalesValidationException> {
            BusinessSalesRules.calculateQuote(
                CreateBusinessQuoteInput(
                    buyerName = "Empresa",
                    issueDate = "17-07-2026",
                    idempotencyKey = "quote-test-0002",
                    lines = listOf(
                        BusinessQuoteLineInput(1, description = "A", quantityMilli = 1_000, unitPriceCents = 1),
                        BusinessQuoteLineInput(1, description = "B", quantityMilli = 1_000, unitPriceCents = 1),
                    ),
                ),
            )
        }
    }

    @Test
    fun serviceCannotTrackStock() {
        assertFailsWith<BusinessSalesValidationException> {
            BusinessSalesRules.validateCatalogItem(
                CreateBusinessCatalogItemInput(
                    type = BusinessCatalogItemType.SERVICE,
                    name = "ConsultorÃ­a",
                    referencePriceCents = 10_000,
                    tracksStock = true,
                ),
            )
        }
    }
}
