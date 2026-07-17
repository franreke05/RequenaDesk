package com.requena.supportdesk.server.business.operations

import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BusinessOperationsServiceTest {
    private val repository = InMemoryBusinessOperationsRepository()
    private val service = BusinessOperationsService(repository, FakeStorage(), CleanScanner())
    private val principal = OperationsPrincipal("00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000011", OperationsRole.OWNER, "owner@example.test")

    @Test fun `active appointments cannot overlap on a resource`() {
        val (bookingService, resource) = seedBooking()
        val start = Instant.parse("2026-08-10T08:00:00Z")
        service.createAppointment(principal, CreateAppointmentCommand(bookingService.id, resource.id, start, start.plusSeconds(1_800), "Europe/Madrid"))

        assertFailsWith<BusinessOperationsConflictException> {
            service.createAppointment(principal, CreateAppointmentCommand(bookingService.id, resource.id, start.plusSeconds(900), start.plusSeconds(2_700), "Europe/Madrid"))
        }
    }

    @Test fun `a tenant cannot read another tenant appointment`() {
        val (bookingService, resource) = seedBooking()
        val start = Instant.parse("2026-08-10T08:00:00Z")
        service.createAppointment(principal, CreateAppointmentCommand(bookingService.id, resource.id, start, start.plusSeconds(1_800), "Europe/Madrid"))
        val stranger = principal.copy(clientId = "00000000-0000-0000-0000-000000000002")
        assertTrue(service.agenda(stranger, start, start.plusSeconds(3_600)).isEmpty())
    }

    @Test fun `viewer cannot mutate bookings even with a tenant principal`() {
        val (bookingService, resource) = seedBooking()
        val viewer = principal.copy(role = OperationsRole.VIEWER)
        val start = Instant.parse("2026-08-10T08:00:00Z")
        assertFailsWith<BusinessOperationsForbiddenException> {
            service.createAppointment(viewer, CreateAppointmentCommand(bookingService.id, resource.id, start, start.plusSeconds(1_800), "Europe/Madrid"))
        }
    }

    @Test fun `document confirmation records authenticated identity and immutable hash`() = runBlocking {
        val document = service.createDocument(principal, CreateDocumentCommand("Acuerdo comercial"))
        val upload = service.prepareUpload(principal, document.id, PrepareDocumentUploadCommand("acuerdo.pdf", "application/pdf", 100))
        val version = service.completeUpload(principal, upload.versionId)
        val request = service.createConfirmationRequest(principal, CreateConfirmationRequestCommand(version.id, "Confirmación", "Confirmo haber leído el documento."))
        val confirmation = service.acceptConfirmation(principal, request.id, principal.email)
        assertEquals(principal.userId, confirmation.acceptedByUserId)
        assertEquals("a".repeat(64), confirmation.documentSha256)
        assertEquals(ConfirmationStatus.ACCEPTED, repository.confirmationRequest(principal.clientId, request.id)?.status)
    }

    private fun seedBooking(): Pair<BookingService, BookingResource> {
        val bookingService = BookingService(clientId = principal.clientId, name = "Consulta", durationMinutes = 30)
        val resource = BookingResource(clientId = principal.clientId, name = "Sala", timeZone = "Europe/Madrid")
        repository.seed(bookingService); repository.seed(resource)
        repository.seed(AvailabilityRule(clientId = principal.clientId, resourceId = resource.id, weekday = 1, startsAt = LocalTime.of(9, 0), endsAt = LocalTime.of(18, 0), timeZone = "Europe/Madrid"))
        return bookingService to resource
    }
}

private class FakeStorage : PrivateDocumentStorage {
    private val keys = mutableMapOf<String, String>()
    override suspend fun issueUpload(version: DocumentVersion): IssuedPrivateUpload {
        keys["intent-${version.id}"] = version.privateObjectKey
        return IssuedPrivateUpload("intent-${version.id}", "https://private-upload.example/${version.id}", Instant.parse("2030-01-01T00:00:00Z"))
    }
    override suspend fun finalizeUpload(storageIntentId: String): StoredPrivateObject {
        // This mirrors a storage adapter that derives key/hash from the object, not a client request field.
        return StoredPrivateObject(requireNotNull(keys[storageIntentId]), "application/pdf", 100, "a".repeat(64))
    }
    override suspend fun issueDownloadUrl(privateObjectKey: String, expiresAt: Instant) = "https://private-download.example/token"
}
private class CleanScanner : DocumentContentScanner { override suspend fun scan(storedObject: StoredPrivateObject) = DocumentScanStatus.CLEAN }
