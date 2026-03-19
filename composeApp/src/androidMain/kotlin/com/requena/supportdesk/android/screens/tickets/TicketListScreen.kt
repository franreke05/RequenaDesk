package com.requena.supportdesk.android.screens.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
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
fun TicketListScreen(
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenDetail: (Ticket) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusOptions = TicketStatus.entries.map { FilterOption(value = it, label = it.displayName()) }
    val categoryOptions = TicketCategory.entries.map { FilterOption(value = it, label = it.displayName()) }
    val waitingOnOptions = WaitingOn.entries.map { FilterOption(value = it, label = it.displayName()) }
    val errorMessage = state.errorMessage

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Tickets",
            subtitle = "Quick triage for urgent work, client blockers and the next ticket to open.",
            eyebrow = "Mobile queue",
        )
        SearchField(
            value = state.searchQuery,
            onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
            placeholder = "Search ticket, app or reference",
        )
        FilterBar(
            label = "Status",
            options = statusOptions,
            selected = state.statusFilter,
            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
        )
        FilterBar(
            label = "Category",
            options = categoryOptions,
            selected = state.categoryFilter,
            onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) },
        )
        FilterBar(
            label = "Waiting on",
            options = waitingOnOptions,
            selected = state.waitingOnFilter,
            onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) },
        )
        when {
            state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 4)
            errorMessage != null && state.tickets.isEmpty() -> EmptyState(
                title = "Tickets unavailable",
                message = errorMessage,
            )
            state.tickets.isEmpty() -> EmptyState(
                title = "No tickets here",
                message = "Your current search and filter combination returned an empty queue.",
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(state.tickets, key = { it.id }) { ticket ->
                    TicketListItem(
                        ticket = ticket,
                        selected = state.selectedTicket?.id == ticket.id,
                        onClick = { onOpenDetail(ticket) },
                        showClient = true,
                    )
                }
            }
        }
        if (!state.isLoading && state.tickets.isNotEmpty()) {
            Text(
                text = "${state.tickets.size} tickets visible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
