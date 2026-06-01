package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.AddInternalCommentRequest
import com.requena.supportdesk.server.domain.model.AddTicketTimeEntryRequest
import com.requena.supportdesk.server.domain.model.ChangeTicketAssigneeRequest
import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeClaimRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeCreateRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.CreateInvoiceRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.UpdateInvoiceStatusRequest
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
        if (request.code.isBlank() || request.email.isBlank()) return null
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
            companyName = identity.companyName,
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
            companyName = identity.companyName,
            accessToken = tokenService.createAccessToken(identity),
            refreshToken = replacementToken,
        )
    }

    fun logout(request: LogoutRequest): Boolean {
        if (request.refreshToken.isBlank()) return false
        return repository.revokeRefreshToken(request.refreshToken)
    }

    fun tickets(ownerAdminId: String? = null, viewerUserId: String? = ownerAdminId, limit: Int = 100, offset: Int = 0) =
        repository.getTickets(ownerAdminId = ownerAdminId, viewerUserId = viewerUserId, limit = limit.boundedLimit(MAX_LIST_LIMIT), offset = offset.boundedOffset())

    fun clientTickets(clientId: String, viewerUserId: String? = null, limit: Int = 100, offset: Int = 0) =
        repository.getTickets(clientId = clientId, viewerUserId = viewerUserId, limit = limit.boundedLimit(CLIENT_TICKET_LIST_LIMIT), offset = offset.boundedOffset())

    fun ticket(id: String, ownerAdminId: String? = null, clientId: String? = null, viewerUserId: String? = ownerAdminId) =
        repository.getTicket(id, ownerAdminId = ownerAdminId, clientId = clientId, viewerUserId = viewerUserId)

    fun createdAdminTicket(request: CreateTicketRequest, ownerAdminId: String) =
        repository.createTicket(request, ownerAdminId = ownerAdminId)

    fun createdClientTicket(request: CreateTicketRequest, identity: ServerAuthIdentity): com.requena.supportdesk.server.domain.model.ServerTicketSnapshot {
        val clientId = identity.clientId ?: throw ServerValidationException("Client identity is required")
        val today = LocalDate.now().toString()
        val todayTicketCount = repository.countClientTicketsCreatedOn(clientId = clientId, datePrefix = today)
        if (todayTicketCount >= CLIENT_DAILY_TICKET_LIMIT) {
            throw ServerValidationException("Daily ticket limit reached")
        }
        if (request.priority.equals("URGENT", ignoreCase = true)) {
            val todayUrgentCount = repository.countClientTicketsCreatedOn(clientId = clientId, datePrefix = today, priority = "URGENT")
            if (todayUrgentCount >= CLIENT_DAILY_URGENT_LIMIT) {
                throw ServerValidationException("Daily urgent ticket limit reached")
            }
        }
        return repository.createTicket(
            request.copy(clientId = clientId),
            requesterId = identity.userId,
        )
    }

    fun deletedTicket(ticketId: String, ownerAdminId: String? = null) = repository.deleteTicket(ticketId, ownerAdminId)

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
        repository.createClientAccessCode(clientId, ownerAdminId, request.expiresInDays.coerceIn(1, CLIENT_ACCESS_CODE_MAX_DAYS))

    fun updatedClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String? = null) =
        repository.updateClient(clientId, request, ownerAdminId)

    fun deletedClient(clientId: String, ownerAdminId: String? = null) = repository.deleteClient(clientId, ownerAdminId)

    fun taskLabels(ownerAdminId: String? = null) = repository.getTaskLabels(ownerAdminId)

    fun createdTaskLabel(request: CreateTaskLabelRequest, ownerAdminId: String? = null) =
        repository.createTaskLabel(request, ownerAdminId)

    fun updatedTaskLabel(labelId: String, request: UpdateTaskLabelRequest, ownerAdminId: String? = null) =
        repository.updateTaskLabel(labelId, request, ownerAdminId)

    fun deletedTaskLabel(labelId: String, ownerAdminId: String? = null) = repository.deleteTaskLabel(labelId, ownerAdminId)

    fun tasks(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null, viewerUserId: String? = ownerAdminId) =
        repository.getTasks(clientId, labelId, ownerAdminId, viewerUserId)

    fun createdTask(request: CreateTaskRequest, ownerAdminId: String? = null): com.requena.supportdesk.server.domain.model.ServerTaskSnapshot {
        validateTaskDueDate(request.dueDate)
        return repository.createTask(request, ownerAdminId)
    }

    fun updatedTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String? = null) =
        repository.updateTask(taskId, request, ownerAdminId)

    fun deletedTask(taskId: String, ownerAdminId: String? = null) = repository.deleteTask(taskId, ownerAdminId)

    fun updatedTaskPin(taskId: String, identity: ServerAuthIdentity, pinned: Boolean): com.requena.supportdesk.server.domain.model.ServerTaskSnapshot {
        if (identity.role != "ADMIN") throw ServerValidationException("Admin role is required")
        repository.setTaskPinned(taskId, identity.userId, identity.userId, pinned)
        return repository.getTasks(ownerAdminId = identity.userId, viewerUserId = identity.userId)
            .firstOrNull { it.id == taskId }
            ?: throw com.requena.supportdesk.server.domain.model.ServerNotFoundException("Task not found")
    }

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

    fun addedTicketTimeEntry(ticketId: String, authorId: String, request: AddTicketTimeEntryRequest) =
        repository.addTicketTimeEntry(ticketId, authorId, request)

    fun ticketTimeEntries(ticketId: String) =
        repository.getTicketTimeEntries(ticketId)

    fun addedInternalComment(ticketId: String, authorId: String, request: AddInternalCommentRequest) =
        repository.addInternalComment(ticketId, authorId, request)

    fun changedTicketAssignee(ticketId: String, request: ChangeTicketAssigneeRequest) =
        repository.changeTicketAssignee(ticketId, request)

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

    fun invoices(ownerAdminId: String? = null, clientId: String? = null, limit: Int = 100, offset: Int = 0) =
        repository.getInvoices(ownerAdminId = ownerAdminId, clientId = clientId, limit = limit.boundedLimit(MAX_LIST_LIMIT), offset = offset.boundedOffset())

    fun invoice(id: String, ownerAdminId: String? = null, clientId: String? = null) =
        repository.getInvoice(id, ownerAdminId = ownerAdminId, clientId = clientId)

    fun createdInvoice(request: CreateInvoiceRequest, ownerAdminId: String) = run {
        if (request.clientId.isBlank()) throw ServerValidationException("clientId is required")
        if (request.issuedAt.isBlank()) throw ServerValidationException("issuedAt is required")
        if (request.items.isEmpty()) throw ServerValidationException("At least one item is required")
        repository.createInvoice(request, createdBy = ownerAdminId)
    }

    fun updatedInvoiceStatus(invoiceId: String, request: UpdateInvoiceStatusRequest, ownerAdminId: String) = run {
        val allowedStatuses = setOf("SENT", "PAID", "CANCELLED")
        if (request.status !in allowedStatuses) throw ServerValidationException("Invalid status: ${request.status}")
        repository.updateInvoiceStatus(invoiceId, request, ownerAdminId)
    }

    private companion object {
        const val CLIENT_DAILY_TICKET_LIMIT = 15
        const val CLIENT_DAILY_URGENT_LIMIT = 3
        const val CLIENT_TICKET_LIST_LIMIT = 100
        const val CLIENT_ACCESS_CODE_MAX_DAYS = 3650
        const val ALERT_LIST_LIMIT = 100
        const val MAX_LIST_LIMIT = 200
    }
}

private fun Int.boundedLimit(max: Int): Int = coerceIn(1, max)

private fun Int.boundedOffset(): Int = coerceAtLeast(0)
