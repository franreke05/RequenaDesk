package com.requena.supportdesk.features.invoices.domain.model

data class InvoiceLineAmounts(
    val subtotal: Double,
    val tax: Double,
    val total: Double,
)

data class InvoiceTotals(
    val subtotal: Double,
    val tax: Double,
    val total: Double,
)

/**
 * Calculates a line using the quantity exactly as supplied. Task durations are
 * rounded to complete hours before they become invoice items; manual activity
 * quantities must never be rounded as if they were time.
 */
fun calculateInvoiceLineAmounts(
    item: CreateInvoiceItemInput,
    taxPercent: Double,
): InvoiceLineAmounts {
    val subtotal = item.quantity * item.unitPrice
    val tax = subtotal * taxPercent / PERCENT_DIVISOR
    return InvoiceLineAmounts(subtotal = subtotal, tax = tax, total = subtotal + tax)
}

fun calculateInvoiceTotals(
    items: List<CreateInvoiceItemInput>,
    taxPercent: Double,
): InvoiceTotals {
    val subtotal = items.sumOf { it.quantity * it.unitPrice }
    val tax = subtotal * taxPercent / PERCENT_DIVISOR
    return InvoiceTotals(subtotal = subtotal, tax = tax, total = subtotal + tax)
}

private const val PERCENT_DIVISOR = 100.0
