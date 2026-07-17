package com.requena.supportdesk.server.business.operations

import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/** Deterministic repository for local development and unit tests. */
class InMemoryBusinessOperationsRepository : BusinessOperationsRepository {
    private val services = ConcurrentHashMap<String, BookingService>()
    private val resources = ConcurrentHashMap<String, BookingResource>()
    private val rules = ConcurrentHashMap<String, AvailabilityRule>()
    private val exceptions = ConcurrentHashMap<String, AvailabilityException>()
    private val appointments = ConcurrentHashMap<String, Appointment>()
    private val folders = ConcurrentHashMap<String, DocumentFolder>()
    private val documents = ConcurrentHashMap<String, BusinessDocument>()
    private val versions = ConcurrentHashMap<String, DocumentVersion>()
    private val confirmationRequests = ConcurrentHashMap<String, ConfirmationRequest>()
    private val confirmations = ConcurrentHashMap<String, DocumentConfirmation>()
    private val audit = mutableListOf<OperationsAuditEvent>()

    fun seed(service: BookingService) { services[service.id] = service }
    fun seed(resource: BookingResource) { resources[resource.id] = resource }
    fun seed(rule: AvailabilityRule) { rules[rule.id] = rule }
    fun seed(exception: AvailabilityException) { exceptions[exception.id] = exception }

    override fun bookingServices(clientId: String) = services.values.filter { it.clientId == clientId }.sortedBy { it.name }
    override fun bookingResources(clientId: String) = resources.values.filter { it.clientId == clientId }.sortedBy { it.name }
    override fun createBookingService(service: BookingService): BookingService { services[service.id] = service; return service }
    override fun createBookingResource(resource: BookingResource): BookingResource { resources[resource.id] = resource; return resource }
    override fun availabilityRules(clientId: String, resourceId: String) = rules.values.filter { it.clientId == clientId && it.resourceId == resourceId }
    override fun createAvailabilityRule(rule: AvailabilityRule): AvailabilityRule { rules[rule.id] = rule; return rule }
    override fun availabilityExceptions(clientId: String, resourceId: String, date: LocalDate) = exceptions.values.filter { it.clientId == clientId && it.resourceId == resourceId && it.date == date }
    override fun appointments(clientId: String, from: Instant, to: Instant) = appointments.values.filter { it.clientId == clientId && it.startsAt < to && it.endsAt > from }.sortedBy { it.startsAt }
    override fun appointment(clientId: String, appointmentId: String) = appointments[appointmentId]?.takeIf { it.clientId == clientId }
    override fun createAppointment(appointment: Appointment): Appointment = synchronized(appointments) {
        check(appointments[appointment.id] == null) { "Appointment already exists" }
        check(!hasAppointmentOverlap(appointment)) { "Resource already has an appointment in that interval" }
        appointments[appointment.id] = appointment
        appointment
    }
    override fun updateAppointment(appointment: Appointment): Appointment = synchronized(appointments) {
        check(appointments[appointment.id]?.clientId == appointment.clientId) { "Appointment does not exist" }
        check(!hasAppointmentOverlap(appointment)) { "Resource already has an appointment in that interval" }
        appointments[appointment.id] = appointment
        appointment
    }

    private fun hasAppointmentOverlap(candidate: Appointment) = appointments.values.any { other ->
        other.id != candidate.id && other.clientId == candidate.clientId && other.resourceId == candidate.resourceId &&
            other.status in ACTIVE_APPOINTMENT_STATUSES && candidate.status in ACTIVE_APPOINTMENT_STATUSES &&
            candidate.startsAt < other.endsAt && candidate.endsAt > other.startsAt
    }

    override fun folders(clientId: String) = folders.values.filter { it.clientId == clientId }.sortedBy { it.name }
    override fun createFolder(folder: DocumentFolder): DocumentFolder { folders[folder.id] = folder; return folder }
    override fun documents(clientId: String) = documents.values.filter { it.clientId == clientId }.sortedByDescending { it.updatedAt }
    override fun document(clientId: String, documentId: String) = documents[documentId]?.takeIf { it.clientId == clientId }
    override fun createDocument(document: BusinessDocument): BusinessDocument { documents[document.id] = document; return document }
    override fun updateDocument(document: BusinessDocument): BusinessDocument { check(documents[document.id]?.clientId == document.clientId); documents[document.id] = document; return document }
    override fun documentVersions(clientId: String, documentId: String) = versions.values.filter { it.clientId == clientId && it.documentId == documentId }.sortedByDescending { it.versionNumber }
    override fun documentVersion(clientId: String, versionId: String) = versions[versionId]?.takeIf { it.clientId == clientId }
    override fun createDocumentVersion(version: DocumentVersion): DocumentVersion { versions[version.id] = version; return version }
    override fun updateDocumentVersion(version: DocumentVersion): DocumentVersion { check(versions[version.id]?.clientId == version.clientId); versions[version.id] = version; return version }
    override fun confirmationRequest(clientId: String, requestId: String) = confirmationRequests[requestId]?.takeIf { it.clientId == clientId }
    override fun createConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest { confirmationRequests[request.id] = request; return request }
    override fun updateConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest { check(confirmationRequests[request.id]?.clientId == request.clientId); confirmationRequests[request.id] = request; return request }
    override fun confirmationForRequest(clientId: String, requestId: String) = confirmations.values.firstOrNull { it.clientId == clientId && it.confirmationRequestId == requestId }
    override fun createConfirmation(confirmation: DocumentConfirmation): DocumentConfirmation { confirmations[confirmation.id] = confirmation; return confirmation }
    override fun appendAudit(event: OperationsAuditEvent) = synchronized(audit) { audit += event }
    override fun auditEvents(clientId: String, entityId: String) = synchronized(audit) { audit.filter { it.clientId == clientId && it.entityId == entityId } }
}

val ACTIVE_APPOINTMENT_STATUSES = setOf(AppointmentStatus.HELD, AppointmentStatus.CONFIRMED)
