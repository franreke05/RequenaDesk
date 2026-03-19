package com.requena.supportdesk.android.screens.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.MessageBubble
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime

@Composable
fun TicketDetailScreen(
    ticket: Ticket?,
    currentUserId: String?,
    onBack: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "No ticket selected",
            message = "Return to the queue and open one ticket to review its latest activity.",
            actionText = "Back to queue",
            onAction = onBack,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = ticket.subject,
            subtitle = ticket.affectedApp,
            eyebrow = ticket.ticketNumber,
            actions = {
                SecondaryButton(text = "Back", onClick = onBack)
            },
        )
        SectionCard(
            title = "Summary",
            subtitle = "Core ticket metadata for quick review.",
        ) {
            TicketStatusBadge(ticket.status)
            TicketPriorityBadge(ticket.priority)
            WaitingOnBadge(ticket.waitingOn)
            TicketCategoryBadge(ticket.category)
            SupportPlatformBadge(ticket.platform)
            InfoRow(label = "Requester", value = ticket.requester.name, supportingText = ticket.requester.email)
            InfoRow(label = "Version", value = ticket.appVersion ?: "-")
            InfoRow(label = "Reference", value = ticket.clientReference ?: "-")
            InfoRow(label = "Updated", value = formatSupportDeskDateTime(ticket.updatedAt))
        }
        SectionCard(
            title = "Description",
            subtitle = "Issue summary and reproduction context.",
        ) {
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!ticket.stepsToReproduce.isNullOrBlank()) {
                Text(
                    text = ticket.stepsToReproduce.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!ticket.resolutionSummary.isNullOrBlank()) {
            SectionCard(
                title = "Resolution summary",
                subtitle = "Visible when the issue is close to done or already resolved.",
            ) {
                Text(
                    text = ticket.resolutionSummary.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        SectionCard(
            title = "Conversation",
            subtitle = "${ticket.messages.size} updates in the thread.",
        ) {
            if (ticket.messages.isEmpty()) {
                EmptyState(
                    title = "No messages yet",
                    message = "The message timeline will appear here once the ticket starts receiving updates.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    ticket.messages.forEach { message ->
                        MessageBubble(
                            authorName = message.authorName,
                            body = message.body,
                            timestamp = message.createdAt,
                            isOwnMessage = currentUserId != null && currentUserId == message.authorId,
                        )
                    }
                }
            }
        }
        SectionCard(
            title = "Attachments",
            subtitle = if (ticket.attachments.isEmpty()) "No files attached yet." else "${ticket.attachments.size} files attached.",
        ) {
            if (ticket.attachments.isEmpty()) {
                Text(
                    text = "Uploads will appear here once the attachment flow is connected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ticket.attachments.forEach { attachment ->
                        AttachmentRow(attachment = attachment, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
