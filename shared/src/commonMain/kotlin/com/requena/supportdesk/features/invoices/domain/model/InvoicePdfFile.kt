package com.requena.supportdesk.features.invoices.domain.model

/** A PDF saved by the desktop application in the local invoice library. */
data class InvoicePdfFile(
    val fileName: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
)
