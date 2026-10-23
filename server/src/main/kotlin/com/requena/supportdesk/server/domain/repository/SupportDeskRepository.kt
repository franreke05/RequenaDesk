package com.requena.supportdesk.server.domain.repository

import com.requena.supportdesk.server.domain.model.CreateClientRequest
import com.requena.supportdesk.server.domain.model.CreateClientProgramRequestsRequest
import com.requena.supportdesk.server.domain.model.CreateClientActivityRequest
import com.requena.supportdesk.server.domain.model.CreateClientContactRequest
import com.requena.supportdesk.server.domain.model.CreateTaskLabelRequest
import com.requena.supportdesk.server.domain.model.CreateTaskRequest
import com.requena.supportdesk.server.domain.model.CreateTicketMessageRequest
import com.requena.supportdesk.server.domain.model.CreateTicketRequest
import com.requena.supportdesk.server.domain.model.RegisterDeviceRequest
import com.requena.supportdesk.server.domain.model.StartWorkSessionRequest
import com.requena.supportdesk.server.domain.model.ServerAttachmentCreated
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerAuthIdentity
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientAccessCredentials
import com.requena.supportdesk.server.domain.model.ServerClientActivitySnapshot
import com.requena.supportdesk.server.domain.model.ServerClientContactSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProvisioning
import com.requena.supportdesk.server.domain.model.ServerClientProgramsSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientProgramRequestSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientBillingPreviewSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTaskLabelSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardPlacementSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskBoardSnapshot
import com.requena.supportdesk.server.domain.model.ServerTaskSnapshot
import com.requena.supportdesk.server.domain.model.ServerTicketFieldUpdate
import com.requena.supportdesk.server.domain.model.ServerTicketMessageCreated
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot
import com.requena.supportdesk.server.domain.model.ServerTimeLogSnapshot
import com.requena.supportdesk.server.domain.model.ServerWorkSessionSnapshot
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
import java.time.Instant

interface SupportDeskRepository : OrganizationRepository {
    fun authenticate(email: String, password: String): ServerAuthIdentity?

    /** Current identity (with company) of an active user; used for tokens minted before V17. */
    fun identityForUser(userId: String): ServerAuthIdentity?

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
    fun getClients(organizationId: String): List<ServerClientSnapshot>
    fun createClient(request: CreateClientRequest, organizationId: String): ServerClientProvisioning
    fun updateClient(clientId: String, request: UpdateClientRequest, organizationId: String): ServerClientSnapshot
    fun updateClientCredentials(clientId: String, request: UpdateClientCredentialsRequest, organizationId: String)
    fun regenerateClientCredentials(clientId: String, organizationId: String): ServerClientAccessCredentials
    fun updateClientComponents(clientId: String, request: UpdateClientComponentsRequest, organizationId: String): ServerClientSnapshot
    fun getClientPrograms(clientId: String): ServerClientProgramsSnapshot
    fun createClientProgramRequests(
        clientId: String,
        requestedByUserId: String,
        request: CreateClientProgramRequestsRequest,
    ): List<ServerClientProgramRequestSnapshot>
    fun getClientProgramRequests(organizationId: String, status: String? = null): List<ServerClientProgramRequestSnapshot>
    fun approveClientProgramRequest(
        requestId: String,
        request: ApproveClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot
    fun rejectClientProgramRequest(
        requestId: String,
        request: RejectClientProgramRequest,
        reviewedByUserId: String,
        organizationId: String,
    ): ServerClientProgramRequestSnapshot
    fun getClientBillingPreview(
        clientId: String,
        period: String,
        organizationId: String,
    ): ServerClientBillingPreviewSnapshot
    fun deleteClient(clientId: String, organizationId: String)
    fun getClientContacts(clientId: String, organizationId: String): List<ServerClientContactSnapshot>
    fun createClientContact(clientId: String, request: CreateClientContactRequest, organizationId: String): ServerClientContactSnapshot
    fun updateClientContact(clientId: String, contactId: String, request: UpdateClientContactRequest, organizationId: String): ServerClientContactSnapshot
    fun deleteClientContact(clientId: String, contactId: String, organizationId: String)
    fun getClientActivities(clientId: String, organizationId: String): List<ServerClientActivitySnapshot>
    fun createClientActivity(clientId: String, request: CreateClientActivityRequest, createdById: String, organizationId: String): ServerClientActivitySnapshot
    fun updateClientActivity(clientId: String, activityId: String, request: UpdateClientActivityRequest, organizationId: String): ServerClientActivitySnapshot
    fun deleteClientActivity(clientId: String, activityId: String, organizationId: String)
    fun getTaskLabels(organizationId: String): List<ServerTaskLabelSnapshot>
    fun createTaskLabel(request: CreateTaskLabelRequest, organizationId: String): ServerTaskLabelSnapshot
    fun updateTaskLabel(labelId: String, request: UpdateTaskLabelRequest, organizationId: String): ServerTaskLabelSnapshot
    fun deleteTaskLabel(labelId: String, organizationId: String)
    fun getTasks(clientId: String? = null, labelId: String? = null, organizationId: String): List<ServerTaskSnapshot>
    fun createTask(request: CreateTaskRequest, createdByUserId: String, organizationId: String): ServerTaskSnapshot
    fun updateTask(taskId: String, request: UpdateTaskRequest, organizationId: String): ServerTaskSnapshot
    fun deleteTask(taskId: String, organizationId: String)
    fun getTaskBoard(organizationId: String): ServerTaskBoardSnapshot
    fun upsertTaskBoardPlacement(
        taskId: String,
        organizationId: String,
        boardDate: String,
        positionX: Double,
        positionY: Double,
    ): ServerTaskBoardPlacementSnapshot
    fun deleteTaskBoardPlacement(taskId: String, organizationId: String)
    fun getTimeLogs(clientId: String? = null, taskId: String? = null, organizationId: String): List<ServerTimeLogSnapshot>
    fun getDashboard(clientId: String? = null, labelId: String? = null, organizationId: String): ServerDashboardSnapshot
    fun getAttachment(id: String): ServerAttachmentSnapshot?
    fun registerDevice(request: RegisterDeviceRequest): ServerDeviceRegistration

    /** The staff user's single non-finished (ACTIVE/PAUSED) session, if any - used for recovery on app start. */
    fun activeWorkSession(actorUserId: String): ServerWorkSessionSnapshot?
    fun workSessionsForTask(taskId: String, actorUserId: String): List<ServerWorkSessionSnapshot>
    fun startedWorkSession(request: StartWorkSessionRequest, actorUserId: String, authorId: String): ServerWorkSessionSnapshot
    fun pausedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot
    fun resumedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot
    fun stoppedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot
    fun checkpointedWorkSession(sessionId: String, actorUserId: String): ServerWorkSessionSnapshot
}
