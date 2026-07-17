package com.requena.supportdesk.features.invoices.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class InvoiceAmountsTest {
    @Test
    fun keepsManualActivityQuantitiesExactInsteadOfRoundingThemAsHours() {
        val activity = CreateInvoiceItemInput(
            description = "Desplazamiento",
            quantity = 1.5,
            unitPrice = 45.0,
            sortOrder = 0,
            kind = InvoiceItemKind.ACTIVITY,
        )

        val amounts = calculateInvoiceLineAmounts(activity, taxPercent = 21.0)

        assertEquals(67.5, amounts.subtotal)
        assertEquals(14.175, amounts.tax)
        assertEquals(81.675, amounts.total)
    }

    @Test
    fun combinesTaskHoursAndActivitiesInTheInvoiceTotal() {
        val totals = calculateInvoiceTotals(
            items = listOf(
                CreateInvoiceItemInput("Tarea", quantity = 2.0, unitPrice = 30.0, sortOrder = 0),
                CreateInvoiceItemInput(
                    description = "Actividad",
                    quantity = 1.5,
                    unitPrice = 45.0,
                    sortOrder = 1,
                    kind = InvoiceItemKind.ACTIVITY,
                ),
            ),
            taxPercent = 21.0,
        )

        assertEquals(127.5, totals.subtotal)
        assertEquals(26.775, totals.tax)
        assertEquals(154.275, totals.total)
    }
}
