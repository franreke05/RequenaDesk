package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeClaimRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeCreateRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import com.requena.supportdesk.server.security.SupportDeskTokenService
import java.time.Instant
import java.time.LocalDate

class SupportDeskService(
    private val repository: SupportDeskRepository,
    private val tokenService: SupportDeskTokenService,
) {
    fun login(email: String, password: String): ServerSession? {
        val identity = repository.authenticate(email = email, password = password) ?: return null
        return createSession(identity)
    }

    fun claimClientAccessCode(request: ClientAccessCodeClaimRequest): ServerSession? {
        if (request.code.isBlank() || request.email.isBlank() || request.password.length < 8) return null
        val identity = repository.claimClientAccessCode(request) ?: return null
        return createSession(identity)
    }

    private fun createSession(identity: ServerAuthIdentity): ServerSession {
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
        )
    }

    fun refresh(request: RefreshSessionRequest): ServerSession? {
        if (request.refreshToken.isBlank()) return null
        val replacementToken = tokenService.createRefreshToken()
        val identity = repository.rotateRefreshToken(
            refreshToken = request.refreshToken,
            replacementRefreshToken = replacementToken,
            expiresAt = tokenService.refreshTokenExpiresAt(Instant.now()),
        ) ?: return null
        return ServerSession(
            userId = identity.userId,
            name = identity.name,
            email = identity.email,
            role = identity.role,
            clientId = identity.clientId,
            accessToken = tokenService.createAccessToken(identity),
            refreshToken = replacementToken,
        )
    }

    fun logout(request: LogoutRequest): Boolean {
        if (request.refreshToken.isBlank()) return false
        return repository.revokeRefreshToken(request.refreshToken)
    }

    fun tickets(ownerAdminId: String? = null, limit: Int = 100, offset: Int = 0) =
        repository.getTickets(ownerAdminId = ownerAdminId, limit = limit.boundedLimit(MAX_LIST_LIMIT), offset = offset.boundedOffset())

    fun clientTickets(clientId: String, limit: Int = 100, offset: Int = 0) =
        repository.getTickets(clientId = clientId, limit = limit.boundedLimit(CLIENT_TICKET_LIST_LIMIT), offset = offset.boundedOffset())

    fun ticket(id: String, ownerAdminId: String? = null, clientId: String? = null) =
        repository.getTicket(id, ownerAdminId = ownerAdminId, clientId = clientId)

    fun createdAdminTicket(request: CreateTicketRequest, ownerAdminId: String) =
        repository.createTicket(request, ownerAdminId = ownerAdminId)

    fun createdClientTicket(request: CreateTicketRequest, identity: ServerAuthIdentity): com.requena.supportdesk.server.domain.model.ServerTicketSnapshot {
        val clientId = identity.clientId ?: throw ServerValidationException("Client identity is required")
        val today = LocalDate.now().toString()
        val todayTicketCount = repository.countClientTicketsCreatedOn(clientId = clientId, datePrefix = today)
        if (todayTicketCount >= CLIENT_DAILY_TICKET_LIMIT) {
            throw ServerValidationException("Daily ticket limit reached")
        }
        return repository.createTicket(
            request.copy(clientId = clientId, priority = "MEDIUM"),
            requesterId = identity.userId,
        )
    }

    fun createdAdminMessage(ticketId: String, ownerAdminId: String, request: CreateTicketMessageRequest): com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated {
        val scopedTicket = ticket(ticketId, ownerAdminId = ownerAdminId)
            ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Ticket not found")
        return repository.createTicketMessage(scopedTicket.id, request)
    }

    fun createdClientMessage(ticketId: String, identity: ServerAuthIdentity, request: CreateTicketMessageRequest): com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated {
        val clientId = identity.clientId ?: throw ServerValidationException("Client identity is required")
        val scopedTicket = ticket(ticketId, clientId = clientId)
            ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Ticket not found")
        return repository.createTicketMessage(scopedTicket.id, request.copy(authorId = identity.userId))
    }

    fun createdMessage(ticketId: String, request: CreateTicketMessageRequest) =
        repository.createTicketMessage(ticketId, request)

    fun updatedStatus(ticketId: String, request: UpdateTicketStatusRequest) =
        repository.updateTicketStatus(ticketId, request)

    fun updatedPriority(ticketId: String, request: UpdateTicketPriorityRequest) =
        repository.updateTicketPriority(ticketId, request)

    fun acceptedAdminClose(ticketId: String, ownerAdminId: String, actorId: String, resolutionSummary: String?) =
        ticket(ticketId, ownerAdminId = ownerAdminId)?.let {
            repository.acceptTicketClose(it.id, actorId, "ADMIN", resolutionSummary)
        } ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Ticket not found")

    fun acceptedClientClose(ticketId: String, identity: ServerAuthIdentity): com.requena.supportdesk.server.domain.model.ServerTicketSnapshot {
        val clientId = identity.clientId ?: throw ServerValidationException("Client identity is required")
        return ticket(ticketId, clientId = clientId)?.let {
            repository.acceptTicketClose(it.id, identity.userId, "CLIENT")
        } ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Ticket not found")
    }

    fun ratedClientTicket(ticketId: String, identity: ServerAuthIdentity, rating: Int): com.requena.supportdesk.server.domain.model.ServerTicketSnapshot {
        val clientId = identity.clientId ?: throw ServerValidationException("Client identity is required")
        val scopedTicket = ticket(ticketId, clientId = clientId)
            ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Ticket not found")
        return repository.rateTicket(scopedTicket.id, clientId, rating)
    }

    fun uploadedAttachment(ticketId: String, request: UploadAttachmentRequest) =
        repository.createAttachment(ticketId, request)

    fun attachment(id: String, ownerAdminId: String? = null) = repository.getAttachment(id, ownerAdminId)

    fun clients(ownerAdminId: String? = null) = repository.getClients(ownerAdminId)

    fun createdClient(request: CreateClientRequest, ownerAdminId: String? = null) = repository.createClient(request, ownerAdminId)

    fun createdClientAccessCode(clientId: String, ownerAdminId: String, request: ClientAccessCodeCreateRequest): String =
        repository.createClientAccessCode(clientId, ownerAdminId, request.expiresInDays)

    fun updatedClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String? = null) =
        repository.updateClient(clientId, request, ownerAdminId)

    fun deletedClient(clientId: String, ownerAdminId: String? = null) = repository.deleteClient(clientId, ownerAdminId)

    fun taskLabels(ownerAdminId: String? = null) = repository.getTaskLabels(ownerAdminId)

    fun createdTaskLabel(request: CreateTaskLabelRequest, ownerAdminId: String? = null) =
        repository.createTaskLabel(request, ownerAdminId)

    fun updatedTaskLabel(labelId: String, request: UpdateTaskLabelRequest, ownerAdminId: String? = null) =
        repository.updateTaskLabel(labelId, request, ownerAdminId)

    fun deletedTaskLabel(labelId: String, ownerAdminId: String? = null) = repository.deleteTaskLabel(labelId, ownerAdminId)

    fun tasks(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null) =
        repository.getTasks(clientId, labelId, ownerAdminId)

    fun createdTask(request: CreateTaskRequest, ownerAdminId: String? = null): com.requena.supportdesk.server.domain.model.ServerTaskSnapshot {
        validateTaskDueDate(request.dueDate)
        return repository.createTask(request, ownerAdminId)
    }

    fun updatedTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String? = null) =
        repository.updateTask(taskId, request, ownerAdminId)

    fun deletedTask(taskId: String, ownerAdminId: String? = null) = repository.deleteTask(taskId, ownerAdminId)

    fun timeLogs(clientId: String? = null, taskId: String? = null, ownerAdminId: String? = null, limit: Int = 100, offset: Int = 0) =
        repository.getTimeLogs(clientId, taskId, ownerAdminId, limit.boundedLimit(MAX_LIST_LIMIT), offset.boundedOffset())

    fun createdTimeLog(request: CreateTimeLogRequest, ownerAdminId: String? = null): com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot {
        validateWorkDate(request.workDate)
        return repository.createTimeLog(request, ownerAdminId)
    }

    fun dashboard(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null) =
        repository.getDashboard(clientId, labelId, ownerAdminId)

    fun registerDevice(request: RegisterDeviceRequest) = repository.registerDevice(request)

    fun alerts(userId: String, limit: Int = 50, offset: Int = 0) =
        repository.getAlerts(userId, limit.boundedLimit(ALERT_LIST_LIMIT), offset.boundedOffset())

    fun readAlert(alertId: String, userId: String) =
        repository.markAlertRead(alertId, userId)

    private fun validateTaskDueDate(dueDate: String?) {
        val normalized = dueDate?.trim().orEmpty()
        if (normalized.isBlank()) return
        val parsedDate = runCatching { LocalDate.parse(normalized) }
            .getOrElse { throw ServerValidationException("Task due date must use YYYY-MM-DD format") }
        if (parsedDate.isBefore(LocalDate.now())) {
            throw ServerValidationException("You cannot schedule tasks on previous days")
        }
    }

    private fun validateWorkDate(workDate: String) {
        val parsedDate = runCatching { LocalDate.parse(workDate.trim()) }
            .getOrElse { throw ServerValidationException("Work date must use YYYY-MM-DD format") }
        if (parsedDate != LocalDate.now()) {
            throw ServerValidationException("Time can only be logged on the current day")
        }
    }

    private companion object {
        const val CLIENT_DAILY_TICKET_LIMIT = 15
        const val CLIENT_TICKET_LIST_LIMIT = 100
        const val ALERT_LIST_LIMIT = 100
        const val MAX_LIST_LIMIT = 200
    }
}

private fun Int.boundedLimit(max: Int): Int = coerceIn(1, max)

private fun Int.boundedOffset(): Int = coerceAtLeast(0)
