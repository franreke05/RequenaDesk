package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.UpdateTaskRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketPriorityRequest
import com.requena.supportdesk.server.domain.model.UpdateTicketStatusRequest
import com.requena.supportdesk.server.domain.model.UploadAttachmentRequest
import java.time.Instant

interface SupportDeskRepository {
    fun authenticate(email: String, password: String): ServerAuthIdentity?
    fun storeRefreshToken(userId: String, refreshToken: String, expiresAt: Instant)
    fun rotateRefreshToken(refreshToken: String, replacementRefreshToken: String, expiresAt: Instant): ServerAuthIdentity?
    fun revokeRefreshToken(refreshToken: String): Boolean
    fun getTickets(): List<ServerTicketSnapshot>
    fun getTicket(id: String): ServerTicketSnapshot?
    fun createTicket(request: CreateTicketRequest): ServerTicketSnapshot
    fun createTicketMessage(ticketId: String, request: CreateTicketMessageRequest): ServerTicketMessageCreated
    fun updateTicketStatus(ticketId: String, request: UpdateTicketStatusRequest): ServerTicketFieldUpdate
    fun updateTicketPriority(ticketId: String, request: UpdateTicketPriorityRequest): ServerTicketFieldUpdate
    fun createAttachment(ticketId: String, request: UploadAttachmentRequest): ServerAttachmentCreated
    fun getClients(): List<ServerClientSnapshot>
    fun createClient(request: CreateClientRequest): ServerClientSnapshot
    fun updateClient(clientId: String, request: UpdateClientRequest): ServerClientSnapshot
    fun deleteClient(clientId: String)
    fun getTaskLabels(): List<ServerTaskLabelSnapshot>
    fun createTaskLabel(request: CreateTaskLabelRequest): ServerTaskLabelSnapshot
    fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest): ServerTaskLabelSnapshot
    fun deleteTaskLabel(labelId: String)
    fun getTasks(clientId: String? = null, labelId: String? = null): List<ServerTaskSnapshot>
    fun createTask(request: CreateTaskRequest): ServerTaskSnapshot
    fun updateTask(taskId: String, request: UpdateTaskRequest): ServerTaskSnapshot
    fun deleteTask(taskId: String)
    fun getTimeLogs(clientId: String? = null, taskId: String? = null): List<ServerTimeLogSnapshot>
    fun createTimeLog(request: CreateTimeLogRequest): ServerTimeLogSnapshot
    fun getDashboard(clientId: String? = null, labelId: String? = null): ServerDashboardSnapshot
    fun getAttachment(id: String): ServerAttachmentSnapshot?
    fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration
}
