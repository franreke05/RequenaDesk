package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateClientProgramRequestsRequest
import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.StartWorkSessionRequest
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientActivityRequest
import com.requena.supportdesk.server.domain.model.UpdateClientContactRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateClientComponentsRequest
import com.requena.supportdesk.server.domain.model.ApproveClientProgramRequest
import com.requena.supportdesk.server.domain.model.RejectClientProgramRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.security.SupportDeskTokenService
import com.requena.supportdesk.server.utils.TASK_BOARD_TIME_ZONE
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SupportDeskService(
    private val repository: SupportDeskRepository,
    private val tokenService: SupportDeskTokenService,
) {
    fun login(email: String, password: String): ServerSession? {
        val identity = repository.authenticate(email = email, password = password)
            // A user without a company could never use its token (see verifyAccessToken).
            ?.takeIf { !it.organizationId.isNullOrBlank() }
            ?: return null
        val refreshToken = tokenService.createRefreshToken()
        repository.storeRefreshToken(
            userId = identity.userId,
            refreshToken = refreshToken,
            expiresAt = tokenService.refreshTokenExpiresAt(Instant.now()),
        )
        return ServerSession(
            userId = identity.userId,
            name = identity.name,
            email = identity.email,
            role = identity.role,
            clientId = identity.clientId,
            accessToken = tokenService.createAccessToken(identity),
            refreshToken = refreshToken,
            organizationId = identity.organizationId,
            organizationRole = identity.organizationRole,
            organizationName = identity.organizationName,
        )
    }

    fun refresh(request: RefreshSessionRequest): ServerSession? {
        if (request.refreshToken.isBlank()) return null
        val replacementToken = tokenService.createRefreshToken()
        val identity = repository.rotateRefreshToken(
            refreshToken = request.refreshToken,
            replacementRefreshToken = replacementToken,
            expiresAt = tokenService.refreshTokenExpiresAt(Instant.now()),
        )?.takeIf { !it.organizationId.isNullOrBlank() } ?: return null
        return ServerSession(
            userId = identity.userId,
            name = identity.name,
            email = identity.email,
            role = identity.role,
            clientId = identity.clientId,
            accessToken = tokenService.createAccessToken(identity),
            refreshToken = replacementToken,
            organizationId = identity.organizationId,
            organizationRole = identity.organizationRole,
            organizationName = identity.organizationName,
        )
    }

    fun logout(request: LogoutRequest): Boolean {
        if (request.refreshToken.isBlank()) return false
        return repository.revokeRefreshToken(request.refreshToken)
    }

    fun tickets() = repository.getTickets()

    fun ticket(id: String) = repository.getTicket(id)

    fun createdTicket(request: CreateTicketRequest) = repository.createTicket(request)

    fun createdMessage(ticketId: String, request: CreateTicketMessageRequest) =
        repository.createTicketMessage(ticketId, request)

    fun updatedStatus(ticketId: String, request: UpdateTicketStatusRequest) =
        repository.updateTicketStatus(ticketId, request)

    fun updatedPriority(ticketId: String, request: UpdateTicketPriorityRequest) =
        repository.updateTicketPriority(ticketId, request)

    fun uploadedAttachment(ticketId: String, request: UploadAttachmentRequest) =
        repository.createAttachment(ticketId, request)

    fun attachment(id: String) = repository.getAttachment(id)

    fun clients(organizationId: String) = repository.getClients(organizationId)

    fun createdClient(request: CreateClientRequest, organizationId: String) = repository.createClient(request, organizationId)

    fun updatedClient(clientId: String, request: UpdateClientRequest, organizationId: String) =
        repository.updateClient(clientId, request, organizationId)

    fun updatedClientCredentials(clientId: String, request: UpdateClientCredentialsRequest, organizationId: String) =
        repository.updateClientCredentials(clientId, request, organizationId)

    fun regeneratedClientCredentials(clientId: String, organizationId: String) =
        repository.regenerateClientCredentials(clientId, organizationId)

    fun updatedClientComponents(clientId: String, request: UpdateClientComponentsRequest, organizationId: String) =
        repository.updateClientComponents(clientId, request, organizationId)

    fun clientPrograms(clientId: String) = repository.getClientPrograms(clientId)

    fun createdClientProgramRequests(
        clientId: String,
        requestedByUserId: String,
        request: CreateClientProgramRequestsRequest,
    ) = repository.createClientProgramRequests(clientId, requestedByUserId, request)

    fun clientProgramRequests(organizationId: String, status: String? = null) =
        repository.getClientProgramRequests(organizationId, status)

    fun approvedClientProgramRequest(
        requestId: String,
        request: ApproveClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ) : com.requena.supportdesk.server.domain.model.ServerClientProgramRequestSnapshot {
        if (request.monthlyPriceCents != 0L) {
            throw ServerValidationException("Business programs are free during the beta")
        }
        return repository.approveClientProgramRequest(requestId, request, reviewedByUserId, organizationId)
    }

    fun rejectedClientProgramRequest(
        requestId: String,
        request: RejectClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ) = repository.rejectClientProgramRequest(requestId, request, reviewedByUserId, organizationId)

    fun clientBillingPreview(clientId: String, period: String, organizationId: String) : com.requena.supportdesk.server.domain.model.ServerClientBillingPreviewSnapshot {
        runCatching { YearMonth.parse(period) }
            .getOrElse { throw ServerValidationException("Billing period must use YYYY-MM format") }
        return repository.getClientBillingPreview(clientId, period, organizationId)
    }

    fun deletedClient(clientId: String, organizationId: String) = repository.deleteClient(clientId, organizationId)

    fun clientContacts(clientId: String, organizationId: String) =
        repository.getClientContacts(clientId, organizationId)

    fun createdClientContact(clientId: String, request: CreateClientContactRequest, organizationId: String) =
        repository.createClientContact(clientId, request, organizationId)

    fun updatedClientContact(
        clientId: String,
        contactId: String,
        request: UpdateClientContactRequest,
        organizationId: String,
    ) = repository.updateClientContact(clientId, contactId, request, organizationId)

    fun deletedClientContact(clientId: String, contactId: String, organizationId: String) =
        repository.deleteClientContact(clientId, contactId, organizationId)

    fun clientActivities(clientId: String, organizationId: String) =
        repository.getClientActivities(clientId, organizationId)

    fun createdClientActivity(
        clientId: String,
        request: CreateClientActivityRequest,
        createdById: String,
        organizationId: String,
    ) = repository.createClientActivity(clientId, request, createdById, organizationId)

    fun updatedClientActivity(
        clientId: String,
        activityId: String,
        request: UpdateClientActivityRequest,
        organizationId: String,
    ) = repository.updateClientActivity(clientId, activityId, request, organizationId)

    fun deletedClientActivity(clientId: String, activityId: String, organizationId: String) =
        repository.deleteClientActivity(clientId, activityId, organizationId)

    fun taskLabels(organizationId: String) = repository.getTaskLabels(organizationId)

    fun createdTaskLabel(request: CreateTaskLabelRequest, organizationId: String) =
        repository.createTaskLabel(request, organizationId)

    fun updatedTaskLabel(labelId: String, request: UpdateTaskLabelRequest, organizationId: String) =
        repository.updateTaskLabel(labelId, request, organizationId)

    fun deletedTaskLabel(labelId: String, organizationId: String) = repository.deleteTaskLabel(labelId, organizationId)

    fun tasks(clientId: String? = null, labelId: String? = null, organizationId: String) =
        repository.getTasks(clientId, labelId, organizationId)

    fun createdTask(
        request: CreateTaskRequest,
        createdByUserId: String,
        organizationId: String,
    ): com.requena.supportdesk.server.domain.model.ServerTaskSnapshot {
        validateTaskDueDate(request.dueDate)
        return repository.createTask(request, createdByUserId, organizationId)
    }

    fun updatedTask(taskId: String, request: UpdateTaskRequest, organizationId: String) =
        repository.updateTask(taskId, request, organizationId)

    fun deletedTask(taskId: String, organizationId: String) = repository.deleteTask(taskId, organizationId)

    fun taskBoard(organizationId: String) = repository.getTaskBoard(organizationId)

    fun placedTaskOnBoard(
        taskId: String,
        organizationId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): com.requena.supportdesk.server.domain.model.ServerTaskBoardPlacementSnapshot {
        val parsedDate = runCatching { LocalDate.parse(boardDate.trim()) }
            .getOrElse { throw ServerValidationException("Board date must use YYYY-MM-DD format") }
        val today = LocalDate.now(ZoneId.of(TASK_BOARD_TIME_ZONE))
        if (parsedDate.isBefore(today)) {
            throw ServerValidationException("Past days cannot be planned")
        }
        if (!positionX.isFinite() || positionX !in 0.0..1.0 ||
            !positionY.isFinite() || positionY !in 0.0..1.0
        ) {
            throw ServerValidationException("Board position must be between 0 and 1")
        }
        return repository.upsertTaskBoardPlacement(
            taskId = taskId,
            organizationId = organizationId,
            boardDate = parsedDate.toString(),
            positionX = positionX,
            positionY = positionY,
        )
    }

    fun removedTaskFromBoard(taskId: String, organizationId: String) =
        repository.deleteTaskBoardPlacement(taskId, organizationId)

    fun timeLogs(clientId: String? = null, taskId: String? = null, organizationId: String) =
        repository.getTimeLogs(clientId, taskId, organizationId)

    fun activeWorkSession(actorUserId: String) = repository.activeWorkSession(actorUserId)

    fun workSessionsForTask(taskId: String, actorUserId: String) = repository.workSessionsForTask(taskId, actorUserId)

    fun startedWorkSession(request: StartWorkSessionRequest, actorUserId: String, authorId: String) =
        repository.startedWorkSession(request, actorUserId, authorId)

    fun pausedWorkSession(sessionId: String, actorUserId: String) = repository.pausedWorkSession(sessionId, actorUserId)

    fun resumedWorkSession(sessionId: String, actorUserId: String) = repository.resumedWorkSession(sessionId, actorUserId)

    fun stoppedWorkSession(sessionId: String, actorUserId: String) = repository.stoppedWorkSession(sessionId, actorUserId)

    fun checkpointedWorkSession(sessionId: String, actorUserId: String) = repository.checkpointedWorkSession(sessionId, actorUserId)

    fun dashboard(clientId: String? = null, labelId: String? = null, organizationId: String) =
        repository.getDashboard(clientId, labelId, organizationId)

    fun registerDevice(request: RegisterDeviceRequest) = repository.registerDevice(request)

    private fun validateTaskDueDate(dueDate: String?) {
        val normalized = dueDate?.trim().orEmpty()
        if (normalized.isBlank()) return
        val parsedDate = runCatching { LocalDate.parse(normalized) }
            .getOrElse { throw ServerValidationException("Task due date must use YYYY-MM-DD format") }
        if (parsedDate.isBefore(LocalDate.now())) {
            throw ServerValidationException("You cannot schedule tasks on previous days")
        }
    }

}
