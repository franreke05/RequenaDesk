package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.layout.FormSection
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.layout.VerticalSectionSpacer
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.CommentBubble
import com.requena.supportdesk.designsystem.components.tickets.MessageBubble
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime

@Composable
fun TicketDetailScreen(
    ticket: Ticket?,
    currentRole: UserRole,
    currentUserId: String?,
    onReply: (String) -> Unit,
    onChangeStatus: (TicketStatus) -> Unit,
    onChangePriority: (TicketPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "No ticket selected",
            message = "Choose a ticket from the queue to review the conversation, technical context and next actions.",
            modifier = modifier,
        )
        return
    }

    var replyDraft by rememberSaveable(ticket.id) { mutableStateOf("") }
    val statusOptions = remember {
        TicketStatus.entries.map { FilterOption(value = it, label = it.displayName()) }
    }
    val priorityOptions = remember {
        TicketPriority.entries.map { FilterOption(value = it, label = it.displayName()) }
    }

    SectionCard(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            PageHeader(
                title = ticket.subject,
                subtitle = ticket.description,
                eyebrow = ticket.ticketNumber,
                actions = {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        TicketStatusBadge(ticket.status)
                        TicketPriorityBadge(ticket.priority)
                        WaitingOnBadge(ticket.waitingOn)
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Column(
                    modifier = Modifier.weight(0.62f),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SectionCard(
                        title = "Issue context",
                        subtitle = "The technical fields that should explain what is affected and how to reproduce it.",
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            TicketCategoryBadge(ticket.category)
                            SupportPlatformBadge(ticket.platform)
                            WaitingOnBadge(ticket.waitingOn)
                        }
                        InfoRow(label = "Affected app", value = ticket.affectedApp)
                        InfoRow(label = "Version", value = ticket.appVersion ?: "-")
                        InfoRow(label = "Reference", value = ticket.clientReference ?: "-")
                        InfoRow(
                            label = "Steps to reproduce",
                            value = ticket.stepsToReproduce ?: "The client did not include repro steps yet.",
                        )
                    }

                    SectionCard(
                        title = "Conversation",
                        subtitle = "${ticket.messages.size} messages across client and admin updates.",
                    ) {
                        if (ticket.messages.isEmpty()) {
                            EmptyState(
                                title = "No messages yet",
                                message = "The ticket exists, but the conversation has not started.",
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
                                    if (message.attachments.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.padding(start = spacing.lg),
                                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                                        ) {
                                            message.attachments.forEach { attachment ->
                                                AttachmentRow(attachment)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FormSection(
                        title = "Quick reply",
                        subtitle = "Keep the response short, ask for the next artifact, or confirm the next step.",
                    ) {
                        OutlinedTextField(
                            value = replyDraft,
                            onValueChange = { replyDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Reply") },
                            placeholder = { Text("Share an update, ask for a log, or confirm the next step.") },
                            minLines = 4,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            PrimaryButton(
                                text = "Send reply",
                                onClick = {
                                    val message = replyDraft.ifBlank {
                                        "Thanks for the update. I am reviewing the ticket and will follow up shortly."
                                    }
                                    onReply(message)
                                    replyDraft = ""
                                },
                            )
                            SecondaryButton(
                                text = "Mark resolved",
                                onClick = { onChangeStatus(TicketStatus.RESOLVED) },
                            )
                        }
                    }

                    AnimatedVisibility(visible = currentRole == UserRole.ADMIN && ticket.internalComments.isNotEmpty()) {
                        SectionCard(
                            title = "Internal comments",
                            subtitle = "Notes that stay visible only to the admin role.",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                ticket.internalComments.forEach { comment ->
                                    CommentBubble(
                                        authorName = comment.authorName,
                                        body = comment.body,
                                        timestamp = comment.createdAt,
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.38f),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    SectionCard(
                        title = "Ticket info",
                        subtitle = "Core metadata, owner and workflow controls.",
                    ) {
                        InfoRow(label = "Requester", value = ticket.requester.name, supportingText = ticket.requester.email)
                        InfoRow(
                            label = "Assignee",
                            value = ticket.assignee?.name ?: "Unassigned",
                            supportingText = ticket.assignee?.email,
                        )
                        InfoRow(label = "Created", value = formatSupportDeskDateTime(ticket.createdAt))
                        InfoRow(label = "Updated", value = formatSupportDeskDateTime(ticket.updatedAt))
                        InfoRow(label = "Messages", value = ticket.messages.size.toString())
                        VerticalSectionSpacer()
                        if (currentRole == UserRole.ADMIN) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FilterBar(
                                label = "Status",
                                options = statusOptions,
                                selected = ticket.status,
                                onSelected = { selected -> selected?.let(onChangeStatus) },
                            )
                            Text(
                                text = "Priority",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FilterBar(
                                label = "Priority",
                                options = priorityOptions,
                                selected = ticket.priority,
                                onSelected = { selected -> selected?.let(onChangePriority) },
                            )
                        }
                    }

                    AnimatedVisibility(visible = !ticket.resolutionSummary.isNullOrBlank()) {
                        SectionCard(
                            title = "Resolution summary",
                            subtitle = "A short close-out note that can be reused in reports and future triage.",
                        ) {
                            Text(
                                text = ticket.resolutionSummary.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    SectionCard(
                        title = "Attachments",
                        subtitle = if (ticket.attachments.isEmpty()) {
                            "No ticket-level files uploaded yet."
                        } else {
                            "${ticket.attachments.size} files available."
                        },
                    ) {
                        if (ticket.attachments.isEmpty()) {
                            EmptyState(
                                title = "No attachments",
                                message = "Files uploaded in replies will appear here when that backend flow is connected.",
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                ticket.attachments.forEach { attachment ->
                                    AttachmentRow(attachment)
                                }
                            }
                        }
                    }

                    SectionCard(
                        title = "Event history",
                        subtitle = "A compact audit trail of ticket state changes.",
                    ) {
                        if (ticket.events.isEmpty()) {
                            EmptyState(
                                title = "No history yet",
                                message = "Events will appear here as the workflow becomes more complete.",
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                ticket.events.forEach { event ->
                                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = "${event.actorName}  ${formatSupportDeskDateTime(event.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
