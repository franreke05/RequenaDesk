package com.requena.supportdesk.server.domain.service

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.LogoutRequest
import com.requena.supportdesk.server.domain.model.RefreshSessionRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class SupportDeskService(
    private val repository: SupportDeskRepository,
) {
    fun login(email: String, password: String): ServerSession? {
        val identity = repository.authenticate(email = email, password = password) ?: return null
        val refreshToken = UUID.randomUUID().toString()
        repository.storeRefreshToken(
            userId = identity.userId,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
        )
        return ServerSession(
            userId = identity.userId,
            name = identity.name,
            email = identity.email,
            role = identity.role,
            clientId = identity.clientId,
            accessToken = UUID.randomUUID().toString(),
            refreshToken = refreshToken,
        )
    }

    fun refresh(request: RefreshSessionRequest): ServerSession? {
        if (request.refreshToken.isBlank()) return null
        val replacementToken = UUID.randomUUID().toString()
        val identity = repository.rotateRefreshToken(
            refreshToken = request.refreshToken,
            replacementRefreshToken = replacementToken,
            expiresAt = Instant.now().plus(30, ChronoUnit.DAYS),
        ) ?: return null
        return ServerSession(
            userId = identity.userId,
            name = identity.name,
            email = identity.email,
            role = identity.role,
            clientId = identity.clientId,
            accessToken = UUID.randomUUID().toString(),
            refreshToken = replacementToken,
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

    fun clients() = repository.getClients()

    fun createdClient(request: CreateClientRequest) = repository.createClient(request)

    fun updatedClient(clientId: String, request: UpdateClientRequest) = repository.updateClient(clientId, request)

    fun deletedClient(clientId: String) = repository.deleteClient(clientId)

    fun taskLabels() = repository.getTaskLabels()

    fun createdTaskLabel(request: CreateTaskLabelRequest) = repository.createTaskLabel(request)

    fun updatedTaskLabel(labelId: String, request: UpdateTaskLabelRequest) = repository.updateTaskLabel(labelId, request)

    fun deletedTaskLabel(labelId: String) = repository.deleteTaskLabel(labelId)

    fun tasks(clientId: String? = null, labelId: String? = null) = repository.getTasks(clientId, labelId)

    fun createdTask(request: CreateTaskRequest) = repository.createTask(request)

    fun updatedTask(taskId: String, request: UpdateTaskRequest) = repository.updateTask(taskId, request)

    fun deletedTask(taskId: String) = repository.deleteTask(taskId)

    fun timeLogs(clientId: String? = null, taskId: String? = null) = repository.getTimeLogs(clientId, taskId)

    fun createdTimeLog(request: CreateTimeLogRequest) = repository.createTimeLog(request)

    fun dashboard(clientId: String? = null, labelId: String? = null) = repository.getDashboard(clientId, labelId)

    fun registerDevice(request: RegisterDeviceRequest) = repository.registerDevice(request)
}
