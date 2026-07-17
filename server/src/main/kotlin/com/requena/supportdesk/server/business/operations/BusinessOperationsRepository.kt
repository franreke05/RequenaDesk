package com.requena.supportdesk.server.business.operations

import java.time.Instant
import java.time.LocalDate

/**
 * Tenant-scoped persistence contract. Every operation accepts [clientId] explicitly; implementations
 * must never fall back to an unscoped lookup.
 */
interface BusinessOperationsRepository {
    fun bookingServices(clientId: String): List<BookingService>
    fun bookingResources(clientId: String): List<BookingResource>
    fun createBookingService(service: BookingService): BookingService
    fun createBookingResource(resource: BookingResource): BookingResource
    fun availabilityRules(clientId: String, resourceId: String): List<AvailabilityRule>
    fun createAvailabilityRule(rule: AvailabilityRule): AvailabilityRule
    fun availabilityExceptions(clientId: String, resourceId: String, date: LocalDate): List<AvailabilityException>
    fun appointments(clientId: String, from: Instant, to: Instant): List<Appointment>
    fun appointment(clientId: String, appointmentId: String): Appointment?
    fun createAppointment(appointment: Appointment): Appointment
    fun updateAppointment(appointment: Appointment): Appointment

    fun folders(clientId: String): List<DocumentFolder>
    fun createFolder(folder: DocumentFolder): DocumentFolder
    fun documents(clientId: String): List<BusinessDocument>
    fun document(clientId: String, documentId: String): BusinessDocument?
    fun createDocument(document: BusinessDocument): BusinessDocument
    fun updateDocument(document: BusinessDocument): BusinessDocument
    fun documentVersions(clientId: String, documentId: String): List<DocumentVersion>
    fun documentVersion(clientId: String, versionId: String): DocumentVersion?
    fun createDocumentVersion(version: DocumentVersion): DocumentVersion
    fun updateDocumentVersion(version: DocumentVersion): DocumentVersion
    fun confirmationRequest(clientId: String, requestId: String): ConfirmationRequest?
    fun createConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest
    fun updateConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest
    fun confirmationForRequest(clientId: String, requestId: String): DocumentConfirmation?
    fun createConfirmation(confirmation: DocumentConfirmation): DocumentConfirmation
    fun appendAudit(event: OperationsAuditEvent)
    fun auditEvents(clientId: String, entityId: String): List<OperationsAuditEvent>
}
