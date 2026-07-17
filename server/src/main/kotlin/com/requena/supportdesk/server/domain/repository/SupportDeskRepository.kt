package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
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
import com.requena.supportdesk.server.domain.model.ServerClientAccessCredentials
import com.requena.supportdesk.server.domain.model.ServerClientActivitySnapshot
import com.requena.supportdesk.server.domain.model.ServerClientContactSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProvisioning
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.UpdateClientRequest
import com.requena.supportdesk.server.domain.model.UpdateClientActivityRequest
import com.requena.supportdesk.server.domain.model.UpdateClientContactRequest
import com.requena.supportdesk.server.domain.model.UpdateClientCredentialsRequest
import com.requena.supportdesk.server.domain.model.UpdateClientComponentsRequest
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
    fun getClients(ownerAdminId: String? = null): List<ServerClientSnapshot>
    fun createClient(request: CreateClientRequest, ownerAdminId: String? = null): ServerClientProvisioning
    fun updateClient(clientId: String, request: UpdateClientRequest, ownerAdminId: String? = null): ServerClientSnapshot
    fun updateClientCredentials(clientId: String, request: UpdateClientCredentialsRequest, ownerAdminId: String? = null)
    fun regenerateClientCredentials(clientId: String, ownerAdminId: String? = null): ServerClientAccessCredentials
    fun updateClientComponents(clientId: String, request: UpdateClientComponentsRequest, ownerAdminId: String? = null): ServerClientSnapshot
    fun deleteClient(clientId: String, ownerAdminId: String? = null)
    fun getClientContacts(clientId: String, ownerAdminId: String? = null): List<ServerClientContactSnapshot>
    fun createClientContact(clientId: String, request: CreateClientContactRequest, ownerAdminId: String? = null): ServerClientContactSnapshot
    fun updateClientContact(clientId: String, contactId: String, request: UpdateClientContactRequest, ownerAdminId: String? = null): ServerClientContactSnapshot
    fun deleteClientContact(clientId: String, contactId: String, ownerAdminId: String? = null)
    fun getClientActivities(clientId: String, ownerAdminId: String? = null): List<ServerClientActivitySnapshot>
    fun createClientActivity(clientId: String, request: CreateClientActivityRequest, createdById: String, ownerAdminId: String? = null): ServerClientActivitySnapshot
    fun updateClientActivity(clientId: String, activityId: String, request: UpdateClientActivityRequest, ownerAdminId: String? = null): ServerClientActivitySnapshot
    fun deleteClientActivity(clientId: String, activityId: String, ownerAdminId: String? = null)
    fun getTaskLabels(ownerAdminId: String? = null): List<ServerTaskLabelSnapshot>
    fun createTaskLabel(request: CreateTaskLabelRequest, ownerAdminId: String? = null): ServerTaskLabelSnapshot
    fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest, ownerAdminId: String? = null): ServerTaskLabelSnapshot
    fun deleteTaskLabel(labelId: String, ownerAdminId: String? = null)
    fun getTasks(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null): List<ServerTaskSnapshot>
    fun createTask(request: CreateTaskRequest, ownerAdminId: String? = null): ServerTaskSnapshot
    fun updateTask(taskId: String, request: UpdateTaskRequest, ownerAdminId: String? = null): ServerTaskSnapshot
    fun deleteTask(taskId: String, ownerAdminId: String? = null)
    fun getTimeLogs(clientId: String? = null, taskId: String? = null, ownerAdminId: String? = null): List<ServerTimeLogSnapshot>
    fun createTimeLog(request: CreateTimeLogRequest, ownerAdminId: String? = null): ServerTimeLogSnapshot
    fun getDashboard(clientId: String? = null, labelId: String? = null, ownerAdminId: String? = null): ServerDashboardSnapshot
    fun getAttachment(id: String): ServerAttachmentSnapshot?
    fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration
}
