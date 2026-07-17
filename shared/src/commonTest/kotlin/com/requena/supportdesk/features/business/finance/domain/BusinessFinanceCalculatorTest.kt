package com.requena.supportdesk.features.business.finance.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BusinessFinanceCalculatorTest {
    @Test
    fun `sales line calculates quantity discount and tax in integer cents`() {
        val result = BusinessFinanceCalculator.calculateSalesLine(
            SalesLineInput(
                description = "Consultoría",
                quantityMilli = 1_500,
                unitPriceCents = 1_000,
                taxRateBasisPoints = 2_100,
                discountBasisPoints = 1_000,
            ),
        )

        assertEquals(1_350, result.subtotalCents)
        assertEquals(284, result.taxCents)
        assertEquals(1_634, result.totalCents)
    }

    @Test
    fun `real calendar dates are accepted and impossible dates are rejected`() {
        BusinessFinanceCalculator.calculateFinanceEntry(
            FinanceEntryInput(
                direction = BusinessFinanceDirection.EXPENSE,
                occurredOn = "2028-02-29",
                description = "Licencia",
                netCents = 100,
            ),
        )

        assertFailsWith<BusinessFinanceValidationException> {
            BusinessFinanceCalculator.calculateFinanceEntry(
                FinanceEntryInput(
                    direction = BusinessFinanceDirection.EXPENSE,
                    occurredOn = "2026-99-99",
                    description = "Licencia",
                    netCents = 100,
                ),
            )
        }
    }

    @Test
    fun `non euro and negative amounts are rejected in beta`() {
        assertFailsWith<BusinessFinanceValidationException> {
            BusinessFinanceCalculator.calculateFinanceEntry(
                FinanceEntryInput(
                    direction = BusinessFinanceDirection.INCOME,
                    occurredOn = "2026-07-17",
                    description = "Venta",
                    netCents = -1,
                    currency = "USD",
                ),
            )
        }
    }
}
