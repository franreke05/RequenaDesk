package com.requena.supportdesk.features.invoices.domain.model

data class InvoiceLineAmounts(
    val subtotal: Double,
    val tax: Double,
    val total: Double,
)

data class InvoiceTotals(
    val subtotal: Double,
    val tax: Double,
    /** Retención de IRPF; se resta del total a pagar. */
    val withholding: Double = 0.0,
    /** Total a pagar: base + IVA - retención. */
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

/** IVA and IRPF are both calculated on the taxable base (base imponible), never on base + IVA. */
fun calculateInvoiceTotals(
    items: List<CreateInvoiceItemInput>,
    taxPercent: Double,
    withholdingPercent: Double = 0.0,
): InvoiceTotals {
    // Each printed amount is rounded to cents first, so base + IVA - IRPF always matches the
    // total shown on the invoice to the cent.
    val subtotal = roundToCents(items.sumOf { it.quantity * it.unitPrice })
    val tax = roundToCents(subtotal * taxPercent / PERCENT_DIVISOR)
    val withholding = roundToCents(subtotal * withholdingPercent / PERCENT_DIVISOR)
    return InvoiceTotals(
        subtotal = subtotal,
        tax = tax,
        withholding = withholding,
        total = roundToCents(subtotal + tax - withholding),
    )
}

/** Half-up rounding to cents; the epsilon absorbs binary noise such as 26.775 -> 26.77499999. */
internal fun roundToCents(value: Double): Double {
    val sign = if (value < 0) -1.0 else 1.0
    return sign * kotlin.math.floor(kotlin.math.abs(value) * CENTS + 0.5 + ROUNDING_EPSILON) / CENTS
}

private const val CENTS = 100.0
private const val ROUNDING_EPSILON = 1e-7

private const val PERCENT_DIVISOR = 100.0
