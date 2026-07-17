package com.requena.supportdesk.server.business.operations

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

const val BOOKINGS_PRODUCT_KEY = "BUSINESS_BOOKINGS"
const val DOCUMENTS_PRODUCT_KEY = "BUSINESS_DOCUMENTS"

enum class OperationsRole { OWNER, MANAGER, MEMBER, VIEWER }
enum class OperationsCapability { BOOKINGS_READ, BOOKINGS_WRITE, BOOKINGS_CONFIGURE, DOCUMENTS_READ, DOCUMENTS_WRITE, DOCUMENTS_CONFIRM }
enum class AppointmentStatus { HELD, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW }
enum class DocumentStatus { DRAFT, PENDING_UPLOAD, AVAILABLE, REJECTED, ARCHIVED }
enum class DocumentScanStatus { PENDING, CLEAN, REJECTED }
enum class ConfirmationStatus { PENDING, ACCEPTED, REVOKED, EXPIRED }

data class OperationsPrincipal(
    val clientId: String,
    val userId: String,
    val role: OperationsRole,
    val email: String = "",
) {
    fun can(capability: OperationsCapability): Boolean = when (role) {
        OperationsRole.OWNER -> true
        OperationsRole.MANAGER -> capability != OperationsCapability.DOCUMENTS_CONFIRM
        OperationsRole.MEMBER -> capability in setOf(
            OperationsCapability.BOOKINGS_READ,
            OperationsCapability.BOOKINGS_WRITE,
            OperationsCapability.DOCUMENTS_READ,
            OperationsCapability.DOCUMENTS_WRITE,
            OperationsCapability.DOCUMENTS_CONFIRM,
        )
        OperationsRole.VIEWER -> capability in setOf(OperationsCapability.BOOKINGS_READ, OperationsCapability.DOCUMENTS_READ)
    }
}

data class BookingService(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val name: String,
    val durationMinutes: Int,
    val active: Boolean = true,
)

data class BookingResource(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val name: String,
    val timeZone: String,
    val active: Boolean = true,
)

data class AvailabilityRule(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val resourceId: String,
    /** ISO weekday 1 (Monday) to 7 (Sunday), evaluated in [timeZone]. */
    val weekday: Int,
    val startsAt: LocalTime,
    val endsAt: LocalTime,
    val timeZone: String,
)

data class AvailabilityException(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val resourceId: String,
    val date: LocalDate,
    val isUnavailable: Boolean,
    val startsAt: LocalTime? = null,
    val endsAt: LocalTime? = null,
    val timeZone: String,
)

data class Appointment(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val serviceId: String,
    val resourceId: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val timeZone: String,
    val status: AppointmentStatus,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val cancelledAt: Instant? = null,
    val cancellationReason: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class AvailableSlot(val startsAt: Instant, val endsAt: Instant, val timeZone: String)

data class DocumentFolder(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val name: String,
    val parentFolderId: String? = null,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
)

data class BusinessDocument(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val folderId: String? = null,
    val title: String,
    val status: DocumentStatus = DocumentStatus.DRAFT,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/** Object keys are never serialized in public responses. */
data class DocumentVersion(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val documentId: String,
    val versionNumber: Int,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String? = null,
    val privateObjectKey: String,
    val storageIntentId: String? = null,
    val scanStatus: DocumentScanStatus = DocumentScanStatus.PENDING,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
)

data class ConfirmationRequest(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val documentVersionId: String,
    val title: String,
    val statement: String,
    val status: ConfirmationStatus = ConfirmationStatus.PENDING,
    val expiresAt: Instant? = null,
    val createdBy: String,
    val createdAt: Instant = Instant.now(),
)

data class DocumentConfirmation(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val confirmationRequestId: String,
    val documentVersionId: String,
    val acceptedByUserId: String,
    val acceptedByEmail: String,
    val statementSnapshot: String,
    val documentSha256: String,
    val acceptedAt: Instant = Instant.now(),
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class OperationsAuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val clientId: String,
    val actorUserId: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val occurredAt: Instant = Instant.now(),
    val details: String? = null,
)

data class UploadIntent(
    val versionId: String,
    val uploadUrl: String,
    val expiresAt: Instant,
)

data class StoredPrivateObject(
    val privateObjectKey: String,
    val contentType: String,
    val sizeBytes: Long,
    val sha256: String,
)

data class IssuedPrivateUpload(
    val storageIntentId: String,
    val uploadUrl: String,
    val expiresAt: Instant,
)

interface PrivateDocumentStorage {
    suspend fun issueUpload(version: DocumentVersion): IssuedPrivateUpload
    suspend fun finalizeUpload(storageIntentId: String): StoredPrivateObject
    suspend fun issueDownloadUrl(privateObjectKey: String, expiresAt: Instant): String
}

interface DocumentContentScanner {
    suspend fun scan(storedObject: StoredPrivateObject): DocumentScanStatus
}

class BusinessOperationsValidationException(message: String) : IllegalArgumentException(message)
class BusinessOperationsConflictException(message: String) : IllegalStateException(message)
class BusinessOperationsNotFoundException(message: String) : NoSuchElementException(message)
class BusinessOperationsForbiddenException(message: String) : SecurityException(message)

internal fun validatedZone(zone: String): ZoneId = try {
    ZoneId.of(zone)
} catch (_: Exception) {
    throw BusinessOperationsValidationException("timeZone must be a valid IANA time zone")
}
