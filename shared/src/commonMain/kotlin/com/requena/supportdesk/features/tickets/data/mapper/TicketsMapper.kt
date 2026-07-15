package com.requena.supportdesk.features.tickets.data.mapper

import com.requena.supportdesk.core.model.User
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.core.model.Attachment
import com.requena.supportdesk.core.model.InternalComment
import com.requena.supportdesk.core.model.TicketEvent
import com.requena.supportdesk.core.model.TicketMessage
import com.requena.supportdesk.features.tickets.data.dto.TicketDto

object TicketsMapper {
    fun fromDto(dto: TicketDto): Ticket {
        return Ticket(
            id = dto.id,
            clientId = dto.clientId,
            ticketNumber = dto.ticketNumber,
            subject = dto.subject,
            description = dto.description,
            category = TicketCategory.valueOf(dto.category),
            affectedApp = dto.affectedApp,
            platform = SupportPlatform.valueOf(dto.platform),
            appVersion = dto.appVersion,
            stepsToReproduce = dto.stepsToReproduce,
            clientReference = dto.clientReference,
            status = TicketStatus.valueOf(dto.status),
            priority = TicketPriority.valueOf(dto.priority),
            waitingOn = WaitingOn.valueOf(dto.waitingOn),
            resolutionSummary = dto.resolutionSummary,
            requester = User(
                id = dto.requesterId,
                name = dto.requesterName,
                email = dto.requesterEmail,
                role = UserRole.CLIENT,
                clientId = dto.clientId,
            ),
            assignee = dto.assigneeId?.let { assigneeId ->
                User(
                    id = assigneeId,
                    name = dto.assigneeName.orEmpty(),
                    email = dto.assigneeEmail.orEmpty(),
                    role = UserRole.ADMIN,
                )
            },
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            messages = dto.messages.map { message ->
                TicketMessage(
                    id = message.id,
                    ticketId = message.ticketId,
                    authorId = message.authorId,
                    authorName = message.authorName,
                    body = message.body,
                    createdAt = message.createdAt,
                )
            },
            internalComments = dto.internalComments.map { comment ->
                InternalComment(
                    id = comment.id,
                    ticketId = comment.ticketId,
                    authorId = comment.authorId,
                    authorName = comment.authorName,
                    body = comment.body,
                    createdAt = comment.createdAt,
                )
            },
            events = dto.events.map { event ->
                TicketEvent(
                    id = event.id,
                    ticketId = event.ticketId,
                    type = event.type,
                    description = event.description,
                    actorName = event.actorName,
                    createdAt = event.createdAt,
                )
            },
            attachments = dto.attachments.map { attachment ->
                Attachment(
                    id = attachment.id,
                    fileName = attachment.fileName,
                    contentType = attachment.contentType,
                    sizeBytes = attachment.sizeBytes,
                    uploadedBy = attachment.uploadedBy,
                    uploadedAt = attachment.uploadedAt,
                )
            },
        )
    }
}
