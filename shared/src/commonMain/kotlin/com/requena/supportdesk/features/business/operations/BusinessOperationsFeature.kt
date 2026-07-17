package com.requena.supportdesk.features.business.operations

import com.requena.supportdesk.core.common.BaseViewModel
import com.requena.supportdesk.core.network.jsonRequestBody
import com.requena.supportdesk.core.network.requireApiData
import com.requena.supportdesk.core.network.supportDeskBaseUrl
import com.requena.supportdesk.core.result.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

@Serializable data class ClientAppointmentDto(val id: String, val serviceId: String, val resourceId: String, val startsAt: String, val endsAt: String, val timeZone: String, val status: String, val contactName: String? = null, val contactEmail: String? = null, val contactPhone: String? = null, val notes: String? = null, val createdAt: String = "")
@Serializable data class BookingServiceDto(val id: String, val name: String, val durationMinutes: Int, val active: Boolean)
@Serializable data class BookingResourceDto(val id: String, val name: String, val timeZone: String, val active: Boolean)
@Serializable data class BookingConfigurationDto(val services: List<BookingServiceDto>, val resources: List<BookingResourceDto>)
@Serializable data class CreateBookingServiceDto(val name: String, val durationMinutes: Int)
@Serializable data class CreateBookingResourceDto(val name: String, val timeZone: String)
@Serializable data class CreateAvailabilityRuleDto(val resourceId: String, val weekday: Int, val startsAt: String, val endsAt: String, val timeZone: String)
@Serializable data class AvailableSlotDto(val startsAt: String, val endsAt: String, val timeZone: String)
@Serializable data class ClientDocumentDto(val id: String, val folderId: String? = null, val title: String, val status: String, val createdAt: String, val updatedAt: String)
@Serializable data class DocumentVersionDto(val id: String, val documentId: String, val versionNumber: Int, val fileName: String, val contentType: String, val sizeBytes: Long, val sha256: String? = null, val scanStatus: String, val createdAt: String)
@Serializable data class UploadIntentDto(val versionId: String, val uploadUrl: String, val expiresAt: String)
@Serializable data class CreateAppointmentDto(val serviceId: String, val resourceId: String, val startsAt: String, val endsAt: String, val timeZone: String, val contactName: String? = null, val contactEmail: String? = null, val contactPhone: String? = null, val notes: String? = null)
@Serializable data class CreateDocumentDto(val title: String, val folderId: String? = null)
@Serializable data class PrepareUploadDto(val fileName: String, val contentType: String, val sizeBytes: Long)
@Serializable data class CancelAppointmentDto(val reason: String? = null)
@Serializable data class ConfirmationRequestDto(val documentVersionId: String, val title: String, val statement: String, val expiresAt: String? = null)

interface BusinessOperationsDataSource {
    suspend fun bookingConfiguration(): BookingConfigurationDto
    suspend fun createBookingService(request: CreateBookingServiceDto): BookingServiceDto
    suspend fun createBookingResource(request: CreateBookingResourceDto): BookingResourceDto
    suspend fun createAvailabilityRule(request: CreateAvailabilityRuleDto): String
    suspend fun agenda(from: String, to: String): List<ClientAppointmentDto>
    suspend fun availability(serviceId: String, resourceId: String, date: String): List<AvailableSlotDto>
    suspend fun createAppointment(request: CreateAppointmentDto): ClientAppointmentDto
    suspend fun cancelAppointment(id: String, reason: String?): ClientAppointmentDto
    suspend fun documents(): List<ClientDocumentDto>
    suspend fun createDocument(request: CreateDocumentDto): ClientDocumentDto
    suspend fun prepareUpload(documentId: String, request: PrepareUploadDto): UploadIntentDto
    suspend fun completeUpload(versionId: String): DocumentVersionDto
    suspend fun createConfirmationRequest(request: ConfirmationRequestDto): String
    suspend fun acceptConfirmation(requestId: String): String
}

/** Real HTTP adapter; the app module only has to bind this class once the server routes are registered. */
class RemoteBusinessOperationsDataSource(private val httpClient: HttpClient) : BusinessOperationsDataSource {
    private val base get() = "${supportDeskBaseUrl()}/client/business"
    override suspend fun bookingConfiguration(): BookingConfigurationDto = httpClient.get("$base/bookings/configuration").requireApiData()
    override suspend fun createBookingService(request: CreateBookingServiceDto): BookingServiceDto = httpClient.post("$base/bookings/services") { setBody(jsonRequestBody(request)) }.requireApiData()
    override suspend fun createBookingResource(request: CreateBookingResourceDto): BookingResourceDto = httpClient.post("$base/bookings/resources") { setBody(jsonRequestBody(request)) }.requireApiData()
    override suspend fun createAvailabilityRule(request: CreateAvailabilityRuleDto): String = httpClient.post("$base/bookings/availability-rules") { setBody(jsonRequestBody(request)) }.requireApiData<IdDto>().id
    override suspend fun agenda(from: String, to: String): List<ClientAppointmentDto> = httpClient.get("$base/bookings/appointments?from=$from&to=$to").requireApiData()
    override suspend fun availability(serviceId: String, resourceId: String, date: String): List<AvailableSlotDto> = httpClient.get("$base/bookings/availability?serviceId=$serviceId&resourceId=$resourceId&date=$date").requireApiData()
    override suspend fun createAppointment(request: CreateAppointmentDto): ClientAppointmentDto = httpClient.post("$base/bookings/appointments") { setBody(jsonRequestBody(request)) }.requireApiData()
    override suspend fun cancelAppointment(id: String, reason: String?): ClientAppointmentDto = httpClient.post("$base/bookings/appointments/$id/cancel") { setBody(jsonRequestBody(CancelAppointmentDto(reason))) }.requireApiData()
    override suspend fun documents(): List<ClientDocumentDto> = httpClient.get("$base/documents").requireApiData()
    override suspend fun createDocument(request: CreateDocumentDto): ClientDocumentDto = httpClient.post("$base/documents") { setBody(jsonRequestBody(request)) }.requireApiData()
    override suspend fun prepareUpload(documentId: String, request: PrepareUploadDto): UploadIntentDto = httpClient.post("$base/documents/$documentId/versions/upload-intents") { setBody(jsonRequestBody(request)) }.requireApiData()
    override suspend fun completeUpload(versionId: String): DocumentVersionDto = httpClient.post("$base/documents/versions/$versionId/complete").requireApiData()
    override suspend fun createConfirmationRequest(request: ConfirmationRequestDto): String = httpClient.post("$base/documents/confirmation-requests") { setBody(jsonRequestBody(request)) }.requireApiData<ConfirmationIdDto>().id
    override suspend fun acceptConfirmation(requestId: String): String = httpClient.post("$base/documents/confirmation-requests/$requestId/accept").requireApiData<ConfirmationIdDto>().id
}
@Serializable private data class ConfirmationIdDto(val id: String)
@Serializable private data class IdDto(val id: String)

interface BusinessOperationsRepository {
    suspend fun bookingConfiguration(): AppResult<BookingConfigurationDto>
    suspend fun createBookingService(request: CreateBookingServiceDto): AppResult<BookingServiceDto>
    suspend fun createBookingResource(request: CreateBookingResourceDto): AppResult<BookingResourceDto>
    suspend fun createAvailabilityRule(request: CreateAvailabilityRuleDto): AppResult<Unit>
    suspend fun agenda(from: String, to: String): AppResult<List<ClientAppointmentDto>>
    suspend fun createAppointment(request: CreateAppointmentDto): AppResult<ClientAppointmentDto>
    suspend fun cancelAppointment(id: String, reason: String?): AppResult<ClientAppointmentDto>
    suspend fun documents(): AppResult<List<ClientDocumentDto>>
    suspend fun createDocument(request: CreateDocumentDto): AppResult<ClientDocumentDto>
}
class BusinessOperationsRepositoryImpl(private val source: BusinessOperationsDataSource) : BusinessOperationsRepository {
    override suspend fun bookingConfiguration() = request { source.bookingConfiguration() }
    override suspend fun createBookingService(request: CreateBookingServiceDto) = request { source.createBookingService(request) }
    override suspend fun createBookingResource(request: CreateBookingResourceDto) = request { source.createBookingResource(request) }
    override suspend fun createAvailabilityRule(request: CreateAvailabilityRuleDto) = request { source.createAvailabilityRule(request); Unit }
    override suspend fun agenda(from: String, to: String) = request { source.agenda(from, to) }
    override suspend fun createAppointment(request: CreateAppointmentDto) = request { source.createAppointment(request) }
    override suspend fun cancelAppointment(id: String, reason: String?) = request { source.cancelAppointment(id, reason) }
    override suspend fun documents() = request { source.documents() }
    override suspend fun createDocument(request: CreateDocumentDto) = request { source.createDocument(request) }
    private suspend fun <T> request(block: suspend () -> T): AppResult<T> = try { AppResult.Success(block()) } catch (error: Throwable) { AppResult.Error(error.message ?: "No se pudo completar la operación", error) }
}

data class OperationsUiState(val appointments: List<ClientAppointmentDto> = emptyList(), val documents: List<ClientDocumentDto> = emptyList(), val configuration: BookingConfigurationDto? = null, val isLoading: Boolean = false, val isSaving: Boolean = false, val message: String? = null)
sealed interface OperationsUiEvent {
    data object LoadBookingConfiguration : OperationsUiEvent
    data class CreateBookingService(val request: CreateBookingServiceDto) : OperationsUiEvent
    data class CreateBookingResource(val request: CreateBookingResourceDto) : OperationsUiEvent
    data class CreateAvailabilityRule(val request: CreateAvailabilityRuleDto) : OperationsUiEvent
    data class LoadAgenda(val from: String, val to: String) : OperationsUiEvent
    data class CreateAppointment(val request: CreateAppointmentDto) : OperationsUiEvent
    data class CancelAppointment(val id: String, val reason: String? = null) : OperationsUiEvent
    data object LoadDocuments : OperationsUiEvent
    data class CreateDocument(val request: CreateDocumentDto) : OperationsUiEvent
}

/** UI state holder with mutations backed by the server; it deliberately has no local-only CRUD. */
class OperationsViewModel(private val repository: BusinessOperationsRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(OperationsUiState())
    val state: StateFlow<OperationsUiState> = _state.asStateFlow()
    fun onEvent(event: OperationsUiEvent) = when (event) {
        OperationsUiEvent.LoadBookingConfiguration -> loadBookingConfiguration()
        is OperationsUiEvent.CreateBookingService -> createBookingService(event.request)
        is OperationsUiEvent.CreateBookingResource -> createBookingResource(event.request)
        is OperationsUiEvent.CreateAvailabilityRule -> createAvailabilityRule(event.request)
        is OperationsUiEvent.LoadAgenda -> loadAgenda(event.from, event.to)
        is OperationsUiEvent.CreateAppointment -> createAppointment(event.request)
        is OperationsUiEvent.CancelAppointment -> cancel(event.id, event.reason)
        OperationsUiEvent.LoadDocuments -> loadDocuments()
        is OperationsUiEvent.CreateDocument -> createDocument(event.request)
    }
    private fun loadBookingConfiguration() = launch { _state.update { it.copy(isLoading = true, message = null) }; when (val r = repository.bookingConfiguration()) { is AppResult.Success -> _state.update { it.copy(isLoading = false, configuration = r.data) }; is AppResult.Error -> fail(r.message) } }
    private fun createBookingService(request: CreateBookingServiceDto) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.createBookingService(request)) { is AppResult.Success -> _state.update { state -> state.copy(isSaving = false, configuration = state.configuration?.copy(services = state.configuration.services + r.data), message = "Servicio guardado") }; is AppResult.Error -> fail(r.message, true) } }
    private fun createBookingResource(request: CreateBookingResourceDto) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.createBookingResource(request)) { is AppResult.Success -> _state.update { state -> state.copy(isSaving = false, configuration = state.configuration?.copy(resources = state.configuration.resources + r.data), message = "Recurso guardado") }; is AppResult.Error -> fail(r.message, true) } }
    private fun createAvailabilityRule(request: CreateAvailabilityRuleDto) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.createAvailabilityRule(request)) { is AppResult.Success -> _state.update { it.copy(isSaving = false, message = "Horario guardado") }; is AppResult.Error -> fail(r.message, true) } }
    private fun loadAgenda(from: String, to: String) = launch { _state.update { it.copy(isLoading = true, message = null) }; when (val r = repository.agenda(from, to)) { is AppResult.Success -> _state.update { it.copy(isLoading = false, appointments = r.data) }; is AppResult.Error -> fail(r.message) } }
    private fun createAppointment(request: CreateAppointmentDto) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.createAppointment(request)) { is AppResult.Success -> _state.update { it.copy(isSaving = false, appointments = (it.appointments + r.data).sortedBy(ClientAppointmentDto::startsAt), message = "Reserva confirmada") }; is AppResult.Error -> fail(r.message, true) } }
    private fun cancel(id: String, reason: String?) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.cancelAppointment(id, reason)) { is AppResult.Success -> _state.update { s -> s.copy(isSaving = false, appointments = s.appointments.map { if (it.id == id) r.data else it }) }; is AppResult.Error -> fail(r.message, true) } }
    private fun loadDocuments() = launch { _state.update { it.copy(isLoading = true, message = null) }; when (val r = repository.documents()) { is AppResult.Success -> _state.update { it.copy(isLoading = false, documents = r.data) }; is AppResult.Error -> fail(r.message) } }
    private fun createDocument(request: CreateDocumentDto) = launch { _state.update { it.copy(isSaving = true, message = null) }; when (val r = repository.createDocument(request)) { is AppResult.Success -> _state.update { it.copy(isSaving = false, documents = listOf(r.data) + it.documents, message = "Documento creado") }; is AppResult.Error -> fail(r.message, true) } }
    private fun fail(message: String, saving: Boolean = false) { _state.update { it.copy(isLoading = false, isSaving = if (saving) false else it.isSaving, message = message) } }
}
