package com.requena.supportdesk.core.model

data class Attachment(
    val id: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: String,
)
