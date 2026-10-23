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
        // Rounded to cents, as printed on the invoice.
        assertEquals(26.78, totals.tax)
        assertEquals(154.28, totals.total)
    }

    @Test
    fun withholdingIsCalculatedOnTheBaseAndSubtractedFromTheTotal() {
        val totals = calculateInvoiceTotals(
            items = listOf(
                CreateInvoiceItemInput("Tarea", quantity = 2.0, unitPrice = 30.0, sortOrder = 0),
                CreateInvoiceItemInput("Actividad", quantity = 1.5, unitPrice = 45.0, sortOrder = 1, kind = InvoiceItemKind.ACTIVITY),
            ),
            taxPercent = 21.0,
            withholdingPercent = 15.0,
        )

        assertEquals(26.78, totals.tax)
        assertEquals(19.13, totals.withholding)
        assertEquals(135.15, totals.total)
    }

    @Test
    fun validatesTheControlLetterOfSpanishPersonalTaxIds() {
        assertEquals(true, isSpanishPersonalTaxIdValid("12345678Z"))
        assertEquals(true, isSpanishPersonalTaxIdValid("12.345.678-z"))
        assertEquals(false, isSpanishPersonalTaxIdValid("12345678A"))
        assertEquals(true, isSpanishPersonalTaxIdValid("X1234567L"))
        // Company CIFs and foreign VAT numbers are not checked.
        assertEquals(null, isSpanishPersonalTaxIdValid("B00000000"))
    }
}
