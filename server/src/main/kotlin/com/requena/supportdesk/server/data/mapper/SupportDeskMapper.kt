package com.requena.supportdesk.server.data.mapper

import com.requena.supportdesk.server.data.entity.AttachmentEntity
import com.requena.supportdesk.server.data.entity.ClientEntity
import com.requena.supportdesk.server.data.entity.DashboardEntity
import com.requena.supportdesk.server.data.entity.DeviceEntity
import com.requena.supportdesk.server.data.entity.TicketEntity
import com.requena.supportdesk.server.domain.model.ServerAttachmentSnapshot
import com.requena.supportdesk.server.domain.model.ServerClientSnapshot
import com.requena.supportdesk.server.domain.model.ServerDashboardSnapshot
import com.requena.supportdesk.server.domain.model.ServerDeviceRegistration
import com.requena.supportdesk.server.domain.model.ServerTicketSnapshot

object SupportDeskMapper {
    fun ticket(entity: TicketEntity): ServerTicketSnapshot = ServerTicketSnapshot(
        id = entity.id,
        clientId = entity.clientId,
        ticketNumber = entity.ticketNumber,
        subject = entity.subject,
        description = entity.description,
        category = entity.category,
        affectedApp = entity.affectedApp,
        platform = entity.platform,
        appVersion = entity.appVersion,
        stepsToReproduce = entity.stepsToReproduce,
        clientReference = entity.clientReference,
        status = entity.status,
        priority = entity.priority,
        waitingOn = entity.waitingOn,
        resolutionSummary = entity.resolutionSummary,
        requesterId = entity.requesterId,
        requesterName = entity.requesterName,
        requesterEmail = entity.requesterEmail,
        assigneeId = entity.assigneeId,
        assigneeName = entity.assigneeName,
        assigneeEmail = entity.assigneeEmail,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun client(entity: ClientEntity): ServerClientSnapshot = ServerClientSnapshot(
        id = entity.id,
        ownerAdminId = entity.ownerAdminId,
        companyName = entity.companyName,
        productName = entity.productName,
        contactName = entity.companyName,
        email = entity.email,
        accountStatus = entity.accountStatus,
        serviceTier = entity.serviceTier,
        preferredContactChannel = entity.preferredContactChannel,
        activeTicketCount = entity.activeTicketCount,
    )

    fun dashboard(entity: DashboardEntity): ServerDashboardSnapshot = ServerDashboardSnapshot(
        openTickets = entity.openTickets,
        pendingClientTickets = entity.pendingClientTickets,
        resolvedToday = entity.resolvedToday,
        activeClients = entity.activeClients,
    )

    fun attachment(entity: AttachmentEntity): ServerAttachmentSnapshot = ServerAttachmentSnapshot(
        id = entity.id,
        fileName = entity.fileName,
        contentType = entity.contentType,
    )

    fun device(entity: DeviceEntity): ServerDeviceRegistration = ServerDeviceRegistration(
        id = entity.id,
        userId = entity.userId,
        platform = entity.platform,
    )
}
