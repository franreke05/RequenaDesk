package com.requena.supportdesk.server.business.operations

import com.requena.supportdesk.server.data.datasource.PostgresSupportDeskDataSource
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** PostgreSQL implementation. RLS remains active in the database; the service also scopes every query by client_id. */
class PostgresBusinessOperationsRepository(
    private val dataSource: PostgresSupportDeskDataSource,
) : BusinessOperationsRepository {
    override fun bookingServices(clientId: String) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_booking_services WHERE client_id=? ORDER BY name").use { s -> s.uuid(1, clientId); s.executeQuery().use { rs -> rs.map(::service) } }
    }
    override fun bookingResources(clientId: String) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_booking_resources WHERE client_id=? ORDER BY name").use { s -> s.uuid(1, clientId); s.executeQuery().use { rs -> rs.map(::resource) } }
    }
    override fun createBookingService(service: BookingService): BookingService = dataSource.withConnection { c ->
        c.prepareStatement("INSERT INTO business_booking_services (id,client_id,name,duration_minutes,active) VALUES (?,?,?,?,?)").use { s -> s.uuid(1,service.id);s.uuid(2,service.clientId);s.setString(3,service.name);s.setInt(4,service.durationMinutes);s.setBoolean(5,service.active);s.executeUpdate() }; service
    }
    override fun createBookingResource(resource: BookingResource): BookingResource = dataSource.withConnection { c ->
        c.prepareStatement("INSERT INTO business_booking_resources (id,client_id,name,time_zone,active) VALUES (?,?,?,?,?)").use { s -> s.uuid(1,resource.id);s.uuid(2,resource.clientId);s.setString(3,resource.name);s.setString(4,resource.timeZone);s.setBoolean(5,resource.active);s.executeUpdate() }; resource
    }
    override fun availabilityRules(clientId: String, resourceId: String) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_availability_rules WHERE client_id=? AND resource_id=? ORDER BY weekday, starts_at").use { s -> s.uuid(1, clientId); s.uuid(2, resourceId); s.executeQuery().use { rs -> rs.map(::rule) } }
    }
    override fun createAvailabilityRule(rule: AvailabilityRule): AvailabilityRule = dataSource.withConnection { c ->
        c.prepareStatement("INSERT INTO business_availability_rules (id,client_id,resource_id,weekday,starts_at,ends_at,time_zone) VALUES (?,?,?,?,?,?,?)").use { s -> s.uuid(1,rule.id);s.uuid(2,rule.clientId);s.uuid(3,rule.resourceId);s.setInt(4,rule.weekday);s.setObject(5,rule.startsAt);s.setObject(6,rule.endsAt);s.setString(7,rule.timeZone);s.executeUpdate() }; rule
    }
    override fun availabilityExceptions(clientId: String, resourceId: String, date: LocalDate) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_availability_exceptions WHERE client_id=? AND resource_id=? AND exception_date=?").use { s -> s.uuid(1, clientId); s.uuid(2, resourceId); s.setObject(3, date); s.executeQuery().use { rs -> rs.map(::exception) } }
    }
    override fun appointments(clientId: String, from: Instant, to: Instant) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_appointments WHERE client_id=? AND starts_at < ? AND ends_at > ? ORDER BY starts_at").use { s -> s.uuid(1, clientId); s.setObject(2, to); s.setObject(3, from); s.executeQuery().use { rs -> rs.map(::appointment) } }
    }
    override fun appointment(clientId: String, appointmentId: String) = dataSource.withConnection { c ->
        c.prepareStatement("SELECT * FROM business_appointments WHERE client_id=? AND id=?").use { s -> s.uuid(1, clientId); s.uuid(2, appointmentId); s.executeQuery().use { if (it.next()) appointment(it) else null } }
    }
    override fun createAppointment(appointment: Appointment): Appointment = dataSource.withConnection { c ->
        c.prepareStatement("""INSERT INTO business_appointments (id,client_id,service_id,resource_id,starts_at,ends_at,time_zone,status,contact_name,contact_email,contact_phone,notes,created_by,cancelled_at,cancellation_reason,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""").use { s ->
            s.uuid(1, appointment.id); s.uuid(2, appointment.clientId); s.uuid(3, appointment.serviceId); s.uuid(4, appointment.resourceId); s.setObject(5, appointment.startsAt); s.setObject(6, appointment.endsAt); s.setString(7, appointment.timeZone); s.setString(8, appointment.status.name); s.setString(9, appointment.contactName); s.setString(10, appointment.contactEmail); s.setString(11, appointment.contactPhone); s.setString(12, appointment.notes); s.uuid(13, appointment.createdBy); s.setObject(14, appointment.cancelledAt); s.setString(15, appointment.cancellationReason); s.setObject(16, appointment.createdAt); s.setObject(17, appointment.updatedAt); s.executeUpdate()
        }; appointment
    }
    override fun updateAppointment(appointment: Appointment): Appointment = dataSource.withConnection { c ->
        c.prepareStatement("""UPDATE business_appointments SET starts_at=?,ends_at=?,time_zone=?,status=?,contact_name=?,contact_email=?,contact_phone=?,notes=?,cancelled_at=?,cancellation_reason=?,updated_at=? WHERE client_id=? AND id=?""").use { s ->
            s.setObject(1, appointment.startsAt); s.setObject(2, appointment.endsAt); s.setString(3, appointment.timeZone); s.setString(4, appointment.status.name); s.setString(5, appointment.contactName); s.setString(6, appointment.contactEmail); s.setString(7, appointment.contactPhone); s.setString(8, appointment.notes); s.setObject(9, appointment.cancelledAt); s.setString(10, appointment.cancellationReason); s.setObject(11, appointment.updatedAt); s.uuid(12, appointment.clientId); s.uuid(13, appointment.id)
            if (s.executeUpdate() != 1) throw BusinessOperationsNotFoundException("Appointment not found")
        }; appointment
    }

    override fun folders(clientId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_document_folders WHERE client_id=? ORDER BY name").use { s -> s.uuid(1, clientId); s.executeQuery().use { it.map(::folder) } } }
    override fun createFolder(folder: DocumentFolder): DocumentFolder = dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_document_folders (id,client_id,name,parent_folder_id,created_by,created_at) VALUES (?,?,?,?,?,?)").use { s -> s.uuid(1,folder.id);s.uuid(2,folder.clientId);s.setString(3,folder.name);s.uuid(4,folder.parentFolderId);s.uuid(5,folder.createdBy);s.setObject(6,folder.createdAt);s.executeUpdate() }; folder }
    override fun documents(clientId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_documents WHERE client_id=? ORDER BY updated_at DESC").use { s -> s.uuid(1,clientId);s.executeQuery().use { it.map(::document) } } }
    override fun document(clientId: String, documentId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_documents WHERE client_id=? AND id=?").use { s -> s.uuid(1,clientId);s.uuid(2,documentId);s.executeQuery().use { if(it.next()) document(it) else null } } }
    override fun createDocument(document: BusinessDocument): BusinessDocument = dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_documents (id,client_id,folder_id,title,status,created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)").use { s -> s.uuid(1,document.id);s.uuid(2,document.clientId);s.uuid(3,document.folderId);s.setString(4,document.title);s.setString(5,document.status.name);s.uuid(6,document.createdBy);s.setObject(7,document.createdAt);s.setObject(8,document.updatedAt);s.executeUpdate() };document }
    override fun updateDocument(document: BusinessDocument): BusinessDocument = dataSource.withConnection { c -> c.prepareStatement("UPDATE business_documents SET folder_id=?,title=?,status=?,updated_at=? WHERE client_id=? AND id=?").use { s -> s.uuid(1,document.folderId);s.setString(2,document.title);s.setString(3,document.status.name);s.setObject(4,document.updatedAt);s.uuid(5,document.clientId);s.uuid(6,document.id);if(s.executeUpdate()!=1)throw BusinessOperationsNotFoundException("Document not found") };document }
    override fun documentVersions(clientId: String, documentId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_document_versions WHERE client_id=? AND document_id=? ORDER BY version_number DESC").use { s -> s.uuid(1,clientId);s.uuid(2,documentId);s.executeQuery().use { it.map(::version) } } }
    override fun documentVersion(clientId: String, versionId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_document_versions WHERE client_id=? AND id=?").use { s -> s.uuid(1,clientId);s.uuid(2,versionId);s.executeQuery().use { if(it.next()) version(it) else null } } }
    override fun createDocumentVersion(version: DocumentVersion): DocumentVersion = dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_document_versions (id,client_id,document_id,version_number,file_name,content_type,size_bytes,sha256,private_object_key,storage_intent_id,scan_status,created_by,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)").use { s -> s.uuid(1,version.id);s.uuid(2,version.clientId);s.uuid(3,version.documentId);s.setInt(4,version.versionNumber);s.setString(5,version.fileName);s.setString(6,version.contentType);s.setLong(7,version.sizeBytes);s.setString(8,version.sha256);s.setString(9,version.privateObjectKey);s.setString(10,version.storageIntentId);s.setString(11,version.scanStatus.name);s.uuid(12,version.createdBy);s.setObject(13,version.createdAt);s.executeUpdate() };version }
    override fun updateDocumentVersion(version: DocumentVersion): DocumentVersion = dataSource.withConnection { c -> c.prepareStatement("UPDATE business_document_versions SET sha256=?,private_object_key=?,storage_intent_id=?,scan_status=? WHERE client_id=? AND id=?").use { s -> s.setString(1,version.sha256);s.setString(2,version.privateObjectKey);s.setString(3,version.storageIntentId);s.setString(4,version.scanStatus.name);s.uuid(5,version.clientId);s.uuid(6,version.id);if(s.executeUpdate()!=1)throw BusinessOperationsNotFoundException("Document version not found") };version }
    override fun confirmationRequest(clientId: String, requestId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_document_confirmation_requests WHERE client_id=? AND id=?").use { s -> s.uuid(1,clientId);s.uuid(2,requestId);s.executeQuery().use { if(it.next()) confirmationRequest(it) else null } } }
    override fun createConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest = dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_document_confirmation_requests (id,client_id,document_version_id,title,statement,status,expires_at,created_by,created_at) VALUES (?,?,?,?,?,?,?,?,?)").use { s -> s.uuid(1,request.id);s.uuid(2,request.clientId);s.uuid(3,request.documentVersionId);s.setString(4,request.title);s.setString(5,request.statement);s.setString(6,request.status.name);s.setObject(7,request.expiresAt);s.uuid(8,request.createdBy);s.setObject(9,request.createdAt);s.executeUpdate() };request }
    override fun updateConfirmationRequest(request: ConfirmationRequest): ConfirmationRequest = dataSource.withConnection { c -> c.prepareStatement("UPDATE business_document_confirmation_requests SET status=? WHERE client_id=? AND id=?").use { s -> s.setString(1,request.status.name);s.uuid(2,request.clientId);s.uuid(3,request.id);if(s.executeUpdate()!=1)throw BusinessOperationsNotFoundException("Confirmation request not found") };request }
    override fun confirmationForRequest(clientId: String, requestId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_document_confirmations WHERE client_id=? AND confirmation_request_id=?").use { s -> s.uuid(1,clientId);s.uuid(2,requestId);s.executeQuery().use { if(it.next()) confirmation(it) else null } } }
    override fun createConfirmation(confirmation: DocumentConfirmation): DocumentConfirmation = dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_document_confirmations (id,client_id,confirmation_request_id,document_version_id,accepted_by_user_id,accepted_by_email,statement_snapshot,document_sha256,accepted_at,ip_address,user_agent) VALUES (?,?,?,?,?,?,?,?,?,?,?)").use { s -> s.uuid(1,confirmation.id);s.uuid(2,confirmation.clientId);s.uuid(3,confirmation.confirmationRequestId);s.uuid(4,confirmation.documentVersionId);s.uuid(5,confirmation.acceptedByUserId);s.setString(6,confirmation.acceptedByEmail);s.setString(7,confirmation.statementSnapshot);s.setString(8,confirmation.documentSha256);s.setObject(9,confirmation.acceptedAt);s.setString(10,confirmation.ipAddress);s.setString(11,confirmation.userAgent);s.executeUpdate() };confirmation }
    override fun appendAudit(event: OperationsAuditEvent) { dataSource.withConnection { c -> c.prepareStatement("INSERT INTO business_operations_audit_events (id,client_id,actor_user_id,action,entity_type,entity_id,occurred_at,details) VALUES (?,?,?,?,?,?,?,?)").use { s -> s.uuid(1,event.id);s.uuid(2,event.clientId);s.uuid(3,event.actorUserId);s.setString(4,event.action);s.setString(5,event.entityType);s.uuid(6,event.entityId);s.setObject(7,event.occurredAt);s.setString(8,event.details);s.executeUpdate() } } }
    override fun auditEvents(clientId: String, entityId: String) = dataSource.withConnection { c -> c.prepareStatement("SELECT * FROM business_operations_audit_events WHERE client_id=? AND entity_id=? ORDER BY occurred_at").use { s -> s.uuid(1,clientId);s.uuid(2,entityId);s.executeQuery().use { rs -> rs.map(::audit) } } }

    private fun service(rs: ResultSet) = BookingService(rs.id("id"),rs.id("client_id"),rs.getString("name"),rs.getInt("duration_minutes"),rs.getBoolean("active"))
    private fun resource(rs: ResultSet) = BookingResource(rs.id("id"),rs.id("client_id"),rs.getString("name"),rs.getString("time_zone"),rs.getBoolean("active"))
    private fun rule(rs: ResultSet) = AvailabilityRule(rs.id("id"),rs.id("client_id"),rs.id("resource_id"),rs.getInt("weekday"),rs.getObject("starts_at",LocalTime::class.java),rs.getObject("ends_at",LocalTime::class.java),rs.getString("time_zone"))
    private fun exception(rs: ResultSet) = AvailabilityException(rs.id("id"),rs.id("client_id"),rs.id("resource_id"),rs.getObject("exception_date",LocalDate::class.java),rs.getBoolean("is_unavailable"),rs.getObject("starts_at",LocalTime::class.java),rs.getObject("ends_at",LocalTime::class.java),rs.getString("time_zone"))
    private fun appointment(rs: ResultSet) = Appointment(rs.id("id"),rs.id("client_id"),rs.id("service_id"),rs.id("resource_id"),rs.getObject("starts_at",Instant::class.java),rs.getObject("ends_at",Instant::class.java),rs.getString("time_zone"),AppointmentStatus.valueOf(rs.getString("status")),rs.getString("contact_name"),rs.getString("contact_email"),rs.getString("contact_phone"),rs.getString("notes"),rs.id("created_by"),rs.getObject("cancelled_at",Instant::class.java),rs.getString("cancellation_reason"),rs.getObject("created_at",Instant::class.java),rs.getObject("updated_at",Instant::class.java))
    private fun folder(rs: ResultSet) = DocumentFolder(rs.id("id"),rs.id("client_id"),rs.getString("name"),rs.getString("parent_folder_id"),rs.id("created_by"),rs.getObject("created_at",Instant::class.java))
    private fun document(rs: ResultSet) = BusinessDocument(rs.id("id"),rs.id("client_id"),rs.getString("folder_id"),rs.getString("title"),DocumentStatus.valueOf(rs.getString("status")),rs.id("created_by"),rs.getObject("created_at",Instant::class.java),rs.getObject("updated_at",Instant::class.java))
    private fun version(rs: ResultSet) = DocumentVersion(rs.id("id"),rs.id("client_id"),rs.id("document_id"),rs.getInt("version_number"),rs.getString("file_name"),rs.getString("content_type"),rs.getLong("size_bytes"),rs.getString("sha256"),rs.getString("private_object_key"),rs.getString("storage_intent_id"),DocumentScanStatus.valueOf(rs.getString("scan_status")),rs.id("created_by"),rs.getObject("created_at",Instant::class.java))
    private fun confirmationRequest(rs: ResultSet) = ConfirmationRequest(rs.id("id"),rs.id("client_id"),rs.id("document_version_id"),rs.getString("title"),rs.getString("statement"),ConfirmationStatus.valueOf(rs.getString("status")),rs.getObject("expires_at",Instant::class.java),rs.id("created_by"),rs.getObject("created_at",Instant::class.java))
    private fun confirmation(rs: ResultSet) = DocumentConfirmation(rs.id("id"),rs.id("client_id"),rs.id("confirmation_request_id"),rs.id("document_version_id"),rs.id("accepted_by_user_id"),rs.getString("accepted_by_email"),rs.getString("statement_snapshot"),rs.getString("document_sha256"),rs.getObject("accepted_at",Instant::class.java),rs.getString("ip_address"),rs.getString("user_agent"))
    private fun audit(rs: ResultSet) = OperationsAuditEvent(rs.id("id"),rs.id("client_id"),rs.id("actor_user_id"),rs.getString("action"),rs.getString("entity_type"),rs.id("entity_id"),rs.getObject("occurred_at",Instant::class.java),rs.getString("details"))
}

private fun java.sql.PreparedStatement.uuid(index: Int, value: String?) { if (value == null) setObject(index, null) else setObject(index, UUID.fromString(value)) }
private fun ResultSet.id(column: String): String = getObject(column).toString()
private fun <T> ResultSet.map(mapper: (ResultSet) -> T): List<T> {
    val values = mutableListOf<T>()
    while (next()) values += mapper(this)
    return values
}
