package com.requena.supportdesk.desktop.screens.tickets

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.UserRole
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TicketListScreen(
    state: TicketsUiState,
    role: UserRole,
    currentUserId: String?,
    onEvent: (TicketsUiEvent) -> Unit,
    onCreateTicket: () -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    onReply: (String) -> Unit,
    onChangeStatus: (TicketStatus) -> Unit,
    onChangePriority: (TicketPriority) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusOptions = remember { TicketStatus.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val categoryOptions = remember { TicketCategory.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val platformOptions = remember { SupportPlatform.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val waitingOnOptions = remember { WaitingOn.entries.map { FilterOption(value = it, label = it.displayName()) } }
    val errorMessage = state.errorMessage

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Tickets",
            subtitle = "Scan the queue fast, keep the active conversation open, and triage the next move without leaving the workspace.",
            eyebrow = if (role == UserRole.ADMIN) "Admin queue" else "Client tickets",
            actions = {
                PrimaryButton(text = "Create ticket", onClick = onCreateTicket)
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SectionCard(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                title = "Inbox",
                subtitle = "${state.tickets.size} visible tickets after the current filters.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        FilterBar(
                            label = "Status",
                            options = statusOptions,
                            selected = state.statusFilter,
                            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Priority",
                            options = priorityOptions,
                            selected = state.priorityFilter,
                            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Category",
                            options = categoryOptions,
                            selected = state.categoryFilter,
                            onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) },
                        )
                        FilterBar(
                            label = "Platform",
                            options = platformOptions,
                            selected = state.platformFilter,
                            onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) },
                        )
                        if (role == UserRole.ADMIN) {
                            FilterBar(
                                label = "Waiting on",
                                options = waitingOnOptions,
                                selected = state.waitingOnFilter,
                                onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) },
                            )
                        }
                    }
                    when {
                        state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 5)
                        errorMessage != null && state.tickets.isEmpty() -> EmptyState(
                            title = "Ticket queue unavailable",
                            message = errorMessage,
                        )
                        state.tickets.isEmpty() -> EmptyState(
                            title = "No tickets match these filters",
                            message = "Clear some filters or create a new ticket to populate the workspace.",
                            actionText = "Create ticket",
                            onAction = onCreateTicket,
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            items(state.tickets, key = { it.id }) { ticket ->
                                TicketListItem(
                                    ticket = ticket,
                                    selected = state.selectedTicket?.id == ticket.id,
                                    onClick = { onOpenDetail(ticket) },
                                    showClient = role == UserRole.ADMIN,
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                title = "Active ticket",
                subtitle = "Conversation, context and next actions stay visible while you work through the queue.",
            ) {
                Crossfade(
                    targetState = state.selectedTicket,
                    label = "desktopTicketDetail",
                ) { selectedTicket ->
                    if (selectedTicket == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Select a ticket to review the thread, context fields, attachments and workflow controls.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        TicketDetailScreen(
                            ticket = selectedTicket,
                            currentRole = role,
                            currentUserId = currentUserId,
                            onReply = onReply,
                            onChangeStatus = onChangeStatus,
                            onChangePriority = onChangePriority,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
