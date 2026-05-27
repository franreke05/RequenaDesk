package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.CreateTimeLogRequest
import com.requena.supportdesk.server.domain.model.ClientAccessCodeClaimRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerNotificationAlertSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
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
    fun claimClientAccessCode(request: ClientAccessCodeClaimRequest): ServerAuthIdentity?
    fun createClientAccessCode(clientId: String, ownerAdminId: String, expiresInDays: Int): String
    fun storeRefreshToken(userId: String, refreshToken: String, expiresAt: Instant)
    fun rotateRefreshToken(refreshToken: String, replacementRefreshToken: String, expiresAt: Instant): ServerAuthIdentity?
    fun revokeRefreshToken(refreshToken: String): Boolean
    fun getTickets(ownerAdminId: String? = null, clientId: String? = null, viewerUserId: String? = null, limit: Int = 100, offset: Int = 0): List<ServerTicketSnapshot>
    fun countClientTicketsCreatedOn(clientId: String, datePrefix: String, priority: String? = null): Int
    fun getTicket(id: String, ownerAdminId: String? = null, clientId: String? = null, viewerUserId: String? = null): ServerTicketSnapshot?
    fun createTicket(request: CreateTicketRequest, ownerAdminId: String? = null, requesterId: String? = null): ServerTicketSnapshot
    fun deleteTicket(ticketId: String, ownerAdminId: String? = null)
    fun updateTicketStatus(ticketId: String, request: UpdateTicketStatusRequest): ServerTicketFieldUpdate
    fun updateTicketPriority(ticketId: String, request: UpdateTicketPriorityRequest): ServerTicketFieldUpdate
    fun acceptTicketClose(ticketId: String, actorId: String, actorRole: String, resolutionSummary: String? = null): ServerTicketSnapshot
    fun rateTicket(ticketId: String, clientId: String, rating: Int): ServerTicketSnapshot
    fun createAttachment(ticketId: String, request: UploadAttachmentRequest): ServerAttachmentCreated
    fun getClients(ownerAdminId: String? = null): List<ServerClientSnapshot>
    fun createClient(request: CreateClientRequest, ownerAdminId: String? = null): ServerClientSnapshot
    fun updateClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String? = null): ServerClientSnapshot
    fun deleteClient(clientId: String, ownerAdminId: String? = null)
    fun getTaskLabels(ownerAdminId: String? = null): List<ServerTaskLabelSnapshot>
    fun createTaskLabel(request: CreateTaskLabelRequest, ownerAdminId: String? = null): ServerTaskLabelSnapshot
    fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest, ownerAdminId: String? = null): ServerTaskLabelSnapshot
    fun deleteTaskLabel(labelId: String, ownerAdminId: String? = null)
    fun getTasks(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null, viewerUserId: String? = null): List<ServerTaskSnapshot>
    fun createTask(request: CreateTaskRequest, ownerAdminId: String? = null): ServerTaskSnapshot
    fun updateTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String? = null): ServerTaskSnapshot
    fun deleteTask(taskId: String, ownerAdminId: String? = null)
    fun setTaskPinned(taskId: String, userId: String, ownerAdminId: String, pinned: Boolean)
    fun getTimeLogs(clientId: String? = null, taskId: String? = null, ownerAdminId: String? = null, limit: Int = 100, offset: Int = 0): List<ServerTimeLogSnapshot>
    fun createTimeLog(request: CreateTimeLogRequest, ownerAdminId: String? = null): ServerTimeLogSnapshot
    fun getDashboard(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null): ServerDashboardSnapshot
    fun getAttachment(id: String, ownerAdminId: String? = null): ServerAttachmentSnapshot?
    fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration
    fun getAlerts(userId: String, limit: Int = 50, offset: Int = 0): List<ServerNotificationAlertSnapshot>
    fun markAlertRead(alertId: String, userId: String): ServerNotificationAlertSnapshot?
}
