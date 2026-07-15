package com.requena.supportdesk.features.invoices.domain.model

/**
 * Returns complete billable hours, always rounding a partial hour upwards.
 * A task without recorded time remains at zero hours.
 */
fun roundedInvoiceHours(recordedSeconds: Int): Int =
    ((recordedSeconds.coerceAtLeast(0).toLong() + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR).toInt()

private const val SECONDS_PER_HOUR = 3_600L
