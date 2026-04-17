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
import com.requena.supportdesk.server.domain.model.ServerValidationException
import com.requena.supportdesk.server.domain.model.ServerSession
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import com.requena.supportdesk.server.domain.repository.SupportDeskRepository
import java.time.Instant
import java.time.LocalDate
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

    fun clients(ownerAdminId: String? = null) = repository.getClients(ownerAdminId)

    fun createdClient(request: CreateClientRequest, ownerAdminId: String? = null) = repository.createClient(request, ownerAdminId)

    fun updatedClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String? = null) =
        repository.updateClient(clientId, request, ownerAdminId)

    fun deletedClient(clientId: String, ownerAdminId: String? = null) = repository.deleteClient(clientId, ownerAdminId)

    fun taskLabels(ownerAdminId: String? = null) = repository.getTaskLabels(ownerAdminId)

    fun createdTaskLabel(request: CreateTaskLabelRequest) = repository.createTaskLabel(request)

    fun updatedTaskLabel(labelId: String, request: UpdateTaskLabelRequest) = repository.updateTaskLabel(labelId, request)

    fun deletedTaskLabel(labelId: String) = repository.deleteTaskLabel(labelId)

    fun tasks(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null) =
        repository.getTasks(clientId, labelId, ownerAdminId)

    fun createdTask(request: CreateTaskRequest, ownerAdminId: String? = null): com.requena.supportdesk.server.domain.model.ServerTaskSnapshot {
        validateTaskDueDate(request.dueDate)
        return repository.createTask(request, ownerAdminId)
    }

    fun updatedTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String? = null) =
        repository.updateTask(taskId, request, ownerAdminId)

    fun deletedTask(taskId: String, ownerAdminId: String? = null) = repository.deleteTask(taskId, ownerAdminId)

    fun timeLogs(clientId: String? = null, taskId: String? = null, ownerAdminId: String? = null) =
        repository.getTimeLogs(clientId, taskId, ownerAdminId)

    fun createdTimeLog(request: CreateTimeLogRequest, ownerAdminId: String? = null): com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot {
        validateWorkDate(request.workDate)
        return repository.createTimeLog(request, ownerAdminId)
    }

    fun dashboard(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null) =
        repository.getDashboard(clientId, labelId, ownerAdminId)

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

    private fun validateWorkDate(workDate: String) {
        val parsedDate = runCatching { LocalDate.parse(workDate.trim()) }
            .getOrElse { throw ServerValidationException("Work date must use YYYY-MM-DD format") }
        if (parsedDate != LocalDate.now()) {
            throw ServerValidationException("Time can only be logged on the current day")
        }
    }
}
