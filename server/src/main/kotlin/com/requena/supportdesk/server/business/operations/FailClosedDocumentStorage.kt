package com.requena.supportdesk.server.business.operations

import java.time.Instant

/**
 * Safe default for deployments that have not wired a private storage provider and malware scanner.
 * It intentionally refuses upload and download instead of degrading to local/public files.
 */
class FailClosedPrivateDocumentStorage : PrivateDocumentStorage {
    override suspend fun issueUpload(version: DocumentVersion): IssuedPrivateUpload =
        throw BusinessOperationsConflictException("Private document storage is not configured")

    override suspend fun finalizeUpload(storageIntentId: String): StoredPrivateObject =
        throw BusinessOperationsConflictException("Private document storage is not configured")

    override suspend fun issueDownloadUrl(privateObjectKey: String, expiresAt: Instant): String =
        throw BusinessOperationsConflictException("Private document storage is not configured")
}

/** A scanner must positively classify content as clean; unknown content is rejected, never published. */
class FailClosedDocumentContentScanner : DocumentContentScanner {
    override suspend fun scan(storedObject: StoredPrivateObject): DocumentScanStatus = DocumentScanStatus.REJECTED
}
