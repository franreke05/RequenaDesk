package com.requena.supportdesk.server.business.operations

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.max

data class CreateAppointmentCommand(
    val serviceId: String,
    val resourceId: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val timeZone: String,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
)

data class CreateBookingServiceCommand(val name: String, val durationMinutes: Int)
data class CreateBookingResourceCommand(val name: String, val timeZone: String)
data class CreateAvailabilityRuleCommand(val resourceId: String, val weekday: Int, val startsAt: String, val endsAt: String, val timeZone: String)
data class CreateDocumentCommand(val title: String, val folderId: String? = null)
data class PrepareDocumentUploadCommand(val fileName: String, val contentType: String, val sizeBytes: Long)
data class CreateConfirmationRequestCommand(val documentVersionId: String, val title: String, val statement: String, val expiresAt: Instant? = null)
data class ConfirmationEvidence(val ipAddress: String? = null, val userAgent: String? = null)

class BusinessOperationsService(
    private val repository: BusinessOperationsRepository,
    private val storage: PrivateDocumentStorage,
    private val scanner: DocumentContentScanner,
    private val now: () -> Instant = { Instant.now() },
) {
    fun bookingConfiguration(principal: OperationsPrincipal): Pair<List<BookingService>, List<BookingResource>> {
        requireCapability(principal, OperationsCapability.BOOKINGS_READ)
        return repository.bookingServices(principal.clientId) to repository.bookingResources(principal.clientId)
    }

    fun createBookingService(principal: OperationsPrincipal, command: CreateBookingServiceCommand): BookingService {
        requireCapability(principal, OperationsCapability.BOOKINGS_CONFIGURE)
        if (command.durationMinutes !in 5..720) throw BusinessOperationsValidationException("durationMinutes must be between 5 and 720")
        return repository.createBookingService(BookingService(clientId = principal.clientId, name = required(command.name, "name", 120), durationMinutes = command.durationMinutes))
            .also { audit(principal, "BOOKING_SERVICE_CREATED", "BOOKING_SERVICE", it.id) }
    }

    fun createBookingResource(principal: OperationsPrincipal, command: CreateBookingResourceCommand): BookingResource {
        requireCapability(principal, OperationsCapability.BOOKINGS_CONFIGURE)
        val zone = validatedZone(command.timeZone).id
        return repository.createBookingResource(BookingResource(clientId = principal.clientId, name = required(command.name, "name", 120), timeZone = zone))
            .also { audit(principal, "BOOKING_RESOURCE_CREATED", "BOOKING_RESOURCE", it.id) }
    }

    fun createAvailabilityRule(principal: OperationsPrincipal, command: CreateAvailabilityRuleCommand): AvailabilityRule {
        requireCapability(principal, OperationsCapability.BOOKINGS_CONFIGURE)
        if (command.weekday !in 1..7) throw BusinessOperationsValidationException("weekday must be between 1 and 7")
        val resource = repository.bookingResources(principal.clientId).firstOrNull { it.id == command.resourceId } ?: throw BusinessOperationsNotFoundException("Booking resource not found")
        val zone = validatedZone(command.timeZone).id
        if (zone != resource.timeZone) throw BusinessOperationsValidationException("timeZone must match the selected resource")
        val start = runCatching { java.time.LocalTime.parse(command.startsAt) }.getOrElse { throw BusinessOperationsValidationException("startsAt must be HH:mm") }
        val end = runCatching { java.time.LocalTime.parse(command.endsAt) }.getOrElse { throw BusinessOperationsValidationException("endsAt must be HH:mm") }
        if (!start.isBefore(end)) throw BusinessOperationsValidationException("startsAt must be before endsAt")
        return repository.createAvailabilityRule(AvailabilityRule(clientId = principal.clientId, resourceId = resource.id, weekday = command.weekday, startsAt = start, endsAt = end, timeZone = zone))
            .also { audit(principal, "BOOKING_AVAILABILITY_CREATED", "AVAILABILITY_RULE", it.id) }
    }

    fun agenda(principal: OperationsPrincipal, from: Instant, to: Instant): List<Appointment> {
        requireCapability(principal, OperationsCapability.BOOKINGS_READ)
        validateInterval(from, to)
        return repository.appointments(principal.clientId, from, to)
    }

    fun availability(principal: OperationsPrincipal, serviceId: String, resourceId: String, date: LocalDate): List<AvailableSlot> {
        requireCapability(principal, OperationsCapability.BOOKINGS_READ)
        val service = repository.bookingServices(principal.clientId).firstOrNull { it.id == serviceId && it.active }
            ?: throw BusinessOperationsNotFoundException("Booking service not found")
        val resource = repository.bookingResources(principal.clientId).firstOrNull { it.id == resourceId && it.active }
            ?: throw BusinessOperationsNotFoundException("Booking resource not found")
        val zone = validatedZone(resource.timeZone)
        val exceptions = repository.availabilityExceptions(principal.clientId, resourceId, date)
        if (exceptions.any { it.isUnavailable }) return emptyList()
        val windows = exceptions.filterNot { it.isUnavailable && it.startsAt != null }.mapNotNull { exception ->
            exception.startsAt?.let { start -> exception.endsAt?.let { end -> start to end } }
        }.ifEmpty {
            repository.availabilityRules(principal.clientId, resourceId)
                .filter { it.weekday == date.dayOfWeek.value }
                .map { it.startsAt to it.endsAt }
        }
        val duration = Duration.ofMinutes(service.durationMinutes.toLong())
        val appointments = repository.appointments(principal.clientId, date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
            .filter { it.resourceId == resourceId && it.status in ACTIVE_APPOINTMENT_STATUSES }
        return windows.flatMap { (start, end) ->
            generateSequence(date.atTime(start).atZone(zone).toInstant()) { it.plus(duration) }
                .takeWhile { it.plus(duration) <= date.atTime(end).atZone(zone).toInstant() }
                .map { slot -> AvailableSlot(slot, slot.plus(duration), zone.id) }
                .filter { slot -> appointments.none { booked -> slot.startsAt < booked.endsAt && slot.endsAt > booked.startsAt } }
                .toList()
        }
    }

    fun createAppointment(principal: OperationsPrincipal, command: CreateAppointmentCommand): Appointment {
        requireCapability(principal, OperationsCapability.BOOKINGS_WRITE)
        validateAppointment(command)
        repository.bookingServices(principal.clientId).firstOrNull { it.id == command.serviceId && it.active }
            ?: throw BusinessOperationsNotFoundException("Booking service not found")
        val resource = repository.bookingResources(principal.clientId).firstOrNull { it.id == command.resourceId && it.active }
            ?: throw BusinessOperationsNotFoundException("Booking resource not found")
        if (resource.timeZone != command.timeZone) throw BusinessOperationsValidationException("timeZone must match the selected resource")
        if (!isInsideAvailability(principal.clientId, command.resourceId, command.startsAt, command.endsAt, resource.timeZone)) {
            throw BusinessOperationsConflictException("The selected interval is not available")
        }
        val appointment = Appointment(
            clientId = principal.clientId, serviceId = command.serviceId, resourceId = command.resourceId,
            startsAt = command.startsAt, endsAt = command.endsAt, timeZone = resource.timeZone,
            status = AppointmentStatus.CONFIRMED, contactName = clean(command.contactName, 120),
            contactEmail = clean(command.contactEmail, 254), contactPhone = clean(command.contactPhone, 40),
            notes = clean(command.notes, 1_000), createdBy = principal.userId, createdAt = now(), updatedAt = now(),
        )
        return try {
            repository.createAppointment(appointment).also { audit(principal, "APPOINTMENT_CREATED", "APPOINTMENT", it.id) }
        } catch (error: BusinessOperationsConflictException) { throw error }
        catch (error: IllegalStateException) { throw BusinessOperationsConflictException("The selected interval is no longer available") }
    }

    fun cancelAppointment(principal: OperationsPrincipal, appointmentId: String, reason: String?): Appointment {
        requireCapability(principal, OperationsCapability.BOOKINGS_WRITE)
        val current = repository.appointment(principal.clientId, appointmentId) ?: throw BusinessOperationsNotFoundException("Appointment not found")
        if (current.status !in ACTIVE_APPOINTMENT_STATUSES) throw BusinessOperationsConflictException("This appointment cannot be cancelled")
        val cancelled = current.copy(status = AppointmentStatus.CANCELLED, cancelledAt = now(), cancellationReason = clean(reason, 500), updatedAt = now())
        return repository.updateAppointment(cancelled).also { audit(principal, "APPOINTMENT_CANCELLED", "APPOINTMENT", it.id) }
    }

    fun listDocuments(principal: OperationsPrincipal): List<BusinessDocument> {
        requireCapability(principal, OperationsCapability.DOCUMENTS_READ)
        return repository.documents(principal.clientId)
    }

    fun listFolders(principal: OperationsPrincipal): List<DocumentFolder> {
        requireCapability(principal, OperationsCapability.DOCUMENTS_READ)
        return repository.folders(principal.clientId)
    }

    fun createFolder(principal: OperationsPrincipal, name: String, parentFolderId: String?): DocumentFolder {
        requireCapability(principal, OperationsCapability.DOCUMENTS_WRITE)
        parentFolderId?.let { id -> if (repository.folders(principal.clientId).none { it.id == id }) throw BusinessOperationsNotFoundException("Parent folder not found") }
        return repository.createFolder(DocumentFolder(clientId = principal.clientId, name = required(name, "name", 180), parentFolderId = parentFolderId, createdBy = principal.userId, createdAt = now()))
            .also { audit(principal, "DOCUMENT_FOLDER_CREATED", "DOCUMENT_FOLDER", it.id) }
    }

    fun createDocument(principal: OperationsPrincipal, command: CreateDocumentCommand): BusinessDocument {
        requireCapability(principal, OperationsCapability.DOCUMENTS_WRITE)
        val title = required(command.title, "title", 180)
        command.folderId?.let { folderId -> if (repository.folders(principal.clientId).none { it.id == folderId }) throw BusinessOperationsNotFoundException("Folder not found") }
        return repository.createDocument(BusinessDocument(clientId = principal.clientId, folderId = command.folderId, title = title, createdBy = principal.userId, createdAt = now(), updatedAt = now()))
            .also { audit(principal, "DOCUMENT_CREATED", "DOCUMENT", it.id) }
    }

    suspend fun prepareUpload(principal: OperationsPrincipal, documentId: String, command: PrepareDocumentUploadCommand): UploadIntent {
        requireCapability(principal, OperationsCapability.DOCUMENTS_WRITE)
        val document = repository.document(principal.clientId, documentId) ?: throw BusinessOperationsNotFoundException("Document not found")
        val fileName = required(command.fileName, "fileName", 255)
        val contentType = required(command.contentType, "contentType", 127).lowercase()
        if (contentType !in SAFE_DOCUMENT_CONTENT_TYPES) throw BusinessOperationsValidationException("Unsupported document content type")
        if (command.sizeBytes !in 1..MAX_DOCUMENT_BYTES) throw BusinessOperationsValidationException("Document size is outside the allowed limit")
        val versionNumber = max(1, repository.documentVersions(principal.clientId, document.id).maxOfOrNull { it.versionNumber + 1 } ?: 1)
        val version = DocumentVersion(
            clientId = principal.clientId, documentId = document.id, versionNumber = versionNumber, fileName = fileName,
            contentType = contentType, sizeBytes = command.sizeBytes, privateObjectKey = "private/${principal.clientId}/${document.id}/${java.util.UUID.randomUUID()}",
            createdBy = principal.userId, createdAt = now(),
        )
        val issued = storage.issueUpload(version)
        val pending = version.copy(storageIntentId = issued.storageIntentId)
        repository.createDocumentVersion(pending)
        repository.updateDocument(document.copy(status = DocumentStatus.PENDING_UPLOAD, updatedAt = now()))
        audit(principal, "DOCUMENT_UPLOAD_ISSUED", "DOCUMENT_VERSION", pending.id)
        return UploadIntent(pending.id, issued.uploadUrl, issued.expiresAt)
    }

    suspend fun completeUpload(principal: OperationsPrincipal, versionId: String): DocumentVersion {
        requireCapability(principal, OperationsCapability.DOCUMENTS_WRITE)
        val pending = repository.documentVersion(principal.clientId, versionId) ?: throw BusinessOperationsNotFoundException("Document version not found")
        val intent = pending.storageIntentId ?: throw BusinessOperationsConflictException("No upload intent exists for this version")
        if (pending.scanStatus != DocumentScanStatus.PENDING) throw BusinessOperationsConflictException("This upload was already finalized")
        val stored = storage.finalizeUpload(intent)
        if (stored.privateObjectKey != pending.privateObjectKey || stored.sizeBytes != pending.sizeBytes || stored.contentType != pending.contentType) {
            throw BusinessOperationsConflictException("Uploaded object does not match the issued upload intent")
        }
        if (!SHA256.matches(stored.sha256)) throw BusinessOperationsConflictException("Storage did not return a valid SHA-256")
        val scanStatus = scanner.scan(stored)
        val completed = pending.copy(sha256 = stored.sha256.lowercase(), scanStatus = scanStatus, storageIntentId = null)
        repository.updateDocumentVersion(completed)
        val document = repository.document(principal.clientId, pending.documentId) ?: throw BusinessOperationsNotFoundException("Document not found")
        repository.updateDocument(document.copy(status = if (scanStatus == DocumentScanStatus.CLEAN) DocumentStatus.AVAILABLE else DocumentStatus.REJECTED, updatedAt = now()))
        audit(principal, if (scanStatus == DocumentScanStatus.CLEAN) "DOCUMENT_UPLOAD_COMPLETED" else "DOCUMENT_UPLOAD_REJECTED", "DOCUMENT_VERSION", completed.id)
        return completed
    }

    suspend fun issueDownload(principal: OperationsPrincipal, versionId: String): String {
        requireCapability(principal, OperationsCapability.DOCUMENTS_READ)
        val version = repository.documentVersion(principal.clientId, versionId) ?: throw BusinessOperationsNotFoundException("Document version not found")
        if (version.scanStatus != DocumentScanStatus.CLEAN) throw BusinessOperationsConflictException("Document is not available")
        audit(principal, "DOCUMENT_DOWNLOAD_ISSUED", "DOCUMENT_VERSION", version.id)
        return storage.issueDownloadUrl(version.privateObjectKey, now().plus(Duration.ofMinutes(5)))
    }

    fun createConfirmationRequest(principal: OperationsPrincipal, command: CreateConfirmationRequestCommand): ConfirmationRequest {
        requireCapability(principal, OperationsCapability.DOCUMENTS_WRITE)
        val version = repository.documentVersion(principal.clientId, command.documentVersionId) ?: throw BusinessOperationsNotFoundException("Document version not found")
        if (version.scanStatus != DocumentScanStatus.CLEAN || version.sha256 == null) throw BusinessOperationsConflictException("Only an available, scanned document version can be confirmed")
        if (command.expiresAt?.isBefore(now()) == true) throw BusinessOperationsValidationException("expiresAt must be in the future")
        val statement = required(command.statement, "statement", 2_000)
        if (statement.contains("firma cualificada", true) || statement.contains("firma avanzada", true)) throw BusinessOperationsValidationException("Beta confirmations must not be labelled as advanced or qualified signatures")
        return repository.createConfirmationRequest(ConfirmationRequest(clientId = principal.clientId, documentVersionId = version.id, title = required(command.title, "title", 180), statement = statement, expiresAt = command.expiresAt, createdBy = principal.userId, createdAt = now()))
            .also { audit(principal, "DOCUMENT_CONFIRMATION_REQUESTED", "CONFIRMATION_REQUEST", it.id) }
    }

    fun acceptConfirmation(principal: OperationsPrincipal, requestId: String, email: String, evidence: ConfirmationEvidence = ConfirmationEvidence()): DocumentConfirmation {
        requireCapability(principal, OperationsCapability.DOCUMENTS_CONFIRM)
        val request = repository.confirmationRequest(principal.clientId, requestId) ?: throw BusinessOperationsNotFoundException("Confirmation request not found")
        if (request.status != ConfirmationStatus.PENDING || request.expiresAt?.isBefore(now()) == true) throw BusinessOperationsConflictException("This confirmation request is no longer active")
        if (repository.confirmationForRequest(principal.clientId, requestId) != null) throw BusinessOperationsConflictException("This confirmation has already been accepted")
        val version = repository.documentVersion(principal.clientId, request.documentVersionId) ?: throw BusinessOperationsNotFoundException("Document version not found")
        val hash = version.sha256 ?: throw BusinessOperationsConflictException("Document hash is unavailable")
        val confirmation = DocumentConfirmation(clientId = principal.clientId, confirmationRequestId = request.id, documentVersionId = version.id, acceptedByUserId = principal.userId, acceptedByEmail = required(email, "email", 254), statementSnapshot = request.statement, documentSha256 = hash, acceptedAt = now(), ipAddress = evidence.ipAddress, userAgent = evidence.userAgent)
        repository.createConfirmation(confirmation)
        repository.updateConfirmationRequest(request.copy(status = ConfirmationStatus.ACCEPTED))
        audit(principal, "DOCUMENT_CONFIRMATION_ACCEPTED", "CONFIRMATION_REQUEST", request.id)
        return confirmation
    }

    private fun isInsideAvailability(clientId: String, resourceId: String, startsAt: Instant, endsAt: Instant, zoneId: String): Boolean {
        val zone = validatedZone(zoneId)
        val start = startsAt.atZone(zone); val end = endsAt.atZone(zone)
        if (start.toLocalDate() != end.toLocalDate()) return false
        val exceptions = repository.availabilityExceptions(clientId, resourceId, start.toLocalDate())
        if (exceptions.any { it.isUnavailable }) return false
        val windows = exceptions.filter { !it.isUnavailable }.mapNotNull { it.startsAt?.let { from -> it.endsAt?.let { to -> from to to } } }
            .ifEmpty { repository.availabilityRules(clientId, resourceId).filter { it.weekday == start.dayOfWeek.value }.map { it.startsAt to it.endsAt } }
        return windows.any { (from, to) -> !start.toLocalTime().isBefore(from) && !end.toLocalTime().isAfter(to) }
    }

    private fun validateAppointment(command: CreateAppointmentCommand) {
        validatedZone(command.timeZone); validateInterval(command.startsAt, command.endsAt)
        if (Duration.between(command.startsAt, command.endsAt) > Duration.ofHours(12)) throw BusinessOperationsValidationException("An appointment cannot exceed 12 hours")
    }
    private fun validateInterval(from: Instant, to: Instant) { if (!from.isBefore(to)) throw BusinessOperationsValidationException("startsAt must be before endsAt") }
    private fun requireCapability(principal: OperationsPrincipal, capability: OperationsCapability) { if (!principal.can(capability)) throw BusinessOperationsForbiddenException("You do not have permission to perform this action") }
    private fun audit(principal: OperationsPrincipal, action: String, type: String, id: String) = repository.appendAudit(OperationsAuditEvent(clientId = principal.clientId, actorUserId = principal.userId, action = action, entityType = type, entityId = id, occurredAt = now()))
}

private const val MAX_DOCUMENT_BYTES = 25L * 1024L * 1024L
private val SAFE_DOCUMENT_CONTENT_TYPES = setOf("application/pdf", "image/png", "image/jpeg", "text/plain")
private val SHA256 = Regex("^[a-fA-F0-9]{64}$")
private fun required(value: String, field: String, maxLength: Int): String = value.trim().also { if (it.isEmpty() || it.length > maxLength) throw BusinessOperationsValidationException("$field is required and must not exceed $maxLength characters") }
private fun clean(value: String?, maxLength: Int): String? = value?.trim()?.takeIf(String::isNotEmpty)?.also { if (it.length > maxLength) throw BusinessOperationsValidationException("Value must not exceed $maxLength characters") }
