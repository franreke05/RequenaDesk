package com.requena.supportdesk.app.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.admin.AdminLayoutMode
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.navigation.AdminSectionDivider
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.CommentBubble
import com.requena.supportdesk.designsystem.components.tickets.MessageBubble
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

@Composable
fun AdminTicketsScreen(
    layoutMode: AdminLayoutMode,
    state: TicketsUiState,
    currentAdminId: String,
    currentAdminName: String,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenCreateTicket: () -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = "Tickets",
            subtitle = "Cola operativa, notas internas y registro manual de horas por ticket.",
            eyebrow = "Admin queue",
            actions = { PrimaryButton(text = "Create ticket", onClick = onOpenCreateTicket) },
        )
        if (layoutMode == AdminLayoutMode.EXPANDED) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TicketListPane(state = state, onEvent = onEvent, onOpenDetail = onOpenDetail, modifier = Modifier.weight(0.42f))
                TicketDetailPane(
                    ticket = state.selectedTicket,
                    currentAdminId = currentAdminId,
                    currentAdminName = currentAdminName,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.58f),
                )
            }
        } else {
            TicketListPane(state = state, onEvent = onEvent, onOpenDetail = onOpenDetail, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AdminTicketDetailScreen(
    ticket: Ticket?,
    currentAdminId: String,
    currentAdminName: String,
    onBack: () -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = ticket?.subject ?: "Ticket detail",
            subtitle = ticket?.affectedApp ?: "Review conversation, notes and hours.",
            eyebrow = ticket?.ticketNumber ?: "Admin detail",
            actions = { SecondaryButton(text = "Back", onClick = onBack) },
        )
        TicketDetailPane(ticket = ticket, currentAdminId = currentAdminId, currentAdminName = currentAdminName, onEvent = onEvent, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TicketListPane(state: TicketsUiState, onEvent: (TicketsUiEvent) -> Unit, onOpenDetail: (Ticket) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier, title = "Queue", subtitle = "${state.tickets.size} visible tickets after filters.") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            SatisfactionSnapshot(tickets = state.tickets)
            SearchField(value = state.searchQuery, onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) }, placeholder = "Search ticket, client or app")
            FilterBar(label = "Status", options = TicketStatus.entries.map { FilterOption(it, it.displayName()) }, selected = state.statusFilter, onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) })
            FilterBar(label = "Priority", options = TicketPriority.entries.map { FilterOption(it, it.displayName()) }, selected = state.priorityFilter, onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) })
            FilterBar(label = "Category", options = TicketCategory.entries.map { FilterOption(it, it.displayName()) }, selected = state.categoryFilter, onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) })
            FilterBar(label = "Platform", options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) }, selected = state.platformFilter, onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) })
            FilterBar(label = "Waiting on", options = WaitingOn.entries.map { FilterOption(it, it.displayName()) }, selected = state.waitingOnFilter, onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) })
            when {
                state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 5)
                state.tickets.isEmpty() -> EmptyState(title = "No tickets found", message = state.errorMessage ?: "Clear filters or create a new ticket.")
                else -> LazyColumn(modifier = Modifier.heightIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(state.tickets, key = { it.id }) { ticket ->
                        TicketListItem(ticket = ticket, selected = state.selectedTicket?.id == ticket.id, onClick = {
                            onEvent(TicketsUiEvent.SelectTicket(ticket.id))
                            onOpenDetail(ticket)
                        }, showClient = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun SatisfactionSnapshot(tickets: List<Ticket>) {
    val ratedTickets = tickets.mapNotNull { it.satisfactionRating }
    val average = if (ratedTickets.isEmpty()) {
        "Sin valoraciones"
    } else {
        val normalized = ratedTickets.sum().toFloat() / ratedTickets.size.toFloat()
        "${(normalized * 10).toInt() / 10f}/5"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.md),
    ) {
        MetricCard(
            label = "Satisfaccion",
            value = average,
            supportingText = "${ratedTickets.size} tickets valorados",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Pendientes cliente",
            value = tickets.count { it.waitingOn == WaitingOn.CLIENT }.toString(),
            supportingText = "Tickets que esperan informacion externa",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TicketDetailPane(ticket: Ticket?, currentAdminId: String, currentAdminName: String, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(title = "No ticket selected", message = "Choose a ticket to review notes and hours.", modifier = modifier)
        return
    }
    var replyDraft by rememberSaveable(ticket.id) { mutableStateOf("") }
    var noteDraft by rememberSaveable(ticket.id) { mutableStateOf("") }
    var timeMinutes by rememberSaveable(ticket.id) { mutableStateOf("") }
    var timeNote by rememberSaveable(ticket.id) { mutableStateOf("") }
    var billable by rememberSaveable(ticket.id) { mutableStateOf(true) }
    SectionCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                TicketStatusBadge(ticket.status); TicketPriorityBadge(ticket.priority); WaitingOnBadge(ticket.waitingOn); TicketCategoryBadge(ticket.category); SupportPlatformBadge(ticket.platform)
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 960.dp
                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    TicketContextCard(ticket); TicketOpsCard(ticket, currentAdminId, currentAdminName, replyDraft, { replyDraft = it }, noteDraft, { noteDraft = it }, timeMinutes, { timeMinutes = it }, timeNote, { timeNote = it }, billable, { billable = it }, onEvent)
                } else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    TicketContextCard(ticket, Modifier.weight(0.58f))
                    TicketOpsCard(ticket, currentAdminId, currentAdminName, replyDraft, { replyDraft = it }, noteDraft, { noteDraft = it }, timeMinutes, { timeMinutes = it }, timeNote, { timeNote = it }, billable, { billable = it }, onEvent, Modifier.weight(0.42f))
                }
            }
        }
    }
}

@Composable
private fun TicketContextCard(ticket: Ticket, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(title = "Ticket context", subtitle = "Requester, app and technical detail.") {
            InfoRow(label = "Requester", value = ticket.requester.name, supportingText = ticket.requester.email)
            InfoRow(label = "Affected app", value = ticket.affectedApp)
            InfoRow(label = "Version", value = ticket.appVersion ?: "-")
            InfoRow(label = "Reference", value = ticket.clientReference ?: "-")
            InfoRow(label = "Steps", value = ticket.stepsToReproduce ?: "No repro steps yet.")
            InfoRow(label = "Satisfaction", value = ticket.satisfactionRating?.let { "$it/5" } ?: "Not rated")
            InfoRow(label = "Updated", value = formatSupportDeskDateTime(ticket.updatedAt))
        }
        SectionCard(title = "Conversation", subtitle = "${ticket.messages.size} visible messages.") {
            if (ticket.messages.isEmpty()) EmptyState(title = "No messages yet", message = "The thread will appear here once the ticket receives updates.") else Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ticket.messages.forEach { MessageBubble(authorName = it.authorName, body = it.body, timestamp = it.createdAt, isOwnMessage = false) }
            }
        }
        if (ticket.attachments.isNotEmpty()) SectionCard(title = "Attachments", subtitle = "${ticket.attachments.size} linked files.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) { ticket.attachments.forEach { AttachmentRow(it) } }
        }
    }
}

@Composable
private fun TicketOpsCard(ticket: Ticket, currentAdminId: String, currentAdminName: String, replyDraft: String, onReplyDraftChange: (String) -> Unit, noteDraft: String, onNoteDraftChange: (String) -> Unit, timeMinutes: String, onTimeMinutesChange: (String) -> Unit, timeNote: String, onTimeNoteChange: (String) -> Unit, billable: Boolean, onBillableChange: (Boolean) -> Unit, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(title = "Workflow", subtitle = "Update status and urgency without leaving the detail.") {
            Text("Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FilterBar(label = "Status", options = TicketStatus.entries.map { FilterOption(it, it.displayName()) }, selected = ticket.status, onSelected = { it?.let { onEvent(TicketsUiEvent.ChangeSelectedStatus(it)) } })
            Text("Priority", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FilterBar(label = "Priority", options = TicketPriority.entries.map { FilterOption(it, it.displayName()) }, selected = ticket.priority, onSelected = { it?.let { onEvent(TicketsUiEvent.ChangeSelectedPriority(it)) } })
        }
        SectionCard(title = "Reply", subtitle = "Customer-visible next message.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(value = replyDraft, onValueChange = onReplyDraftChange, modifier = Modifier.fillMaxWidth(), label = { Text("Reply to client") }, minLines = 4)
                PrimaryButton(text = "Send reply", onClick = { onEvent(TicketsUiEvent.ReplyToSelected(replyDraft)); onReplyDraftChange("") }, enabled = replyDraft.isNotBlank())
            }
        }
        SectionCard(title = "Internal notes", subtitle = "Shared admin-only context.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(value = noteDraft, onValueChange = onNoteDraftChange, modifier = Modifier.fillMaxWidth(), label = { Text("Add internal note") }, minLines = 3)
                Text(text = "Saved as $currentAdminName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton(text = "Save note", onClick = { onEvent(TicketsUiEvent.AddInternalNote(noteDraft, currentAdminId, currentAdminName)); onNoteDraftChange("") }, enabled = noteDraft.isNotBlank())
                if (ticket.internalComments.isEmpty()) EmptyState(title = "No internal notes yet", message = "Use this section for triage notes and decisions.") else Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ticket.internalComments.forEach { CommentBubble(authorName = it.authorName, body = it.body, timestamp = it.createdAt) }
                }
            }
        }
        SectionCard(title = "Log time", subtitle = "Manual time tracking tied to this ticket.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(value = timeMinutes, onValueChange = onTimeMinutesChange, modifier = Modifier.fillMaxWidth(), label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = timeNote, onValueChange = onTimeNoteChange, modifier = Modifier.fillMaxWidth(), label = { Text("Work note") }, minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) { Checkbox(checked = billable, onCheckedChange = onBillableChange); Text(if (billable) "Billable time" else "Non-billable time", style = MaterialTheme.typography.bodyMedium) }
                PrimaryButton(text = "Add time entry", onClick = { onEvent(TicketsUiEvent.AddTimeEntry(timeMinutes.toIntOrNull() ?: 0, timeNote, billable, currentAdminId, currentAdminName)); onTimeMinutesChange(""); onTimeNoteChange(""); onBillableChange(true) }, enabled = timeMinutes.toIntOrNull()?.let { it > 0 } == true && timeNote.isNotBlank())
                if (ticket.timeEntries.isEmpty()) EmptyState(title = "No time logged yet", message = "The first manual entry will appear here.") else Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ticket.timeEntries.forEach {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            Text("${it.authorName} - ${formatSupportDeskDuration(it.minutes)} - ${it.workDate}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Text(it.note, style = MaterialTheme.typography.bodyMedium)
                            Text(if (it.billable) "Billable" else "Non-billable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AdminSectionDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCreateTicketScreen(clients: List<Client>, onBack: () -> Unit, onCreateTicket: (CreateTicketInput) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    var selectedClientId by remember(clients) { mutableStateOf(clients.firstOrNull()?.id.orEmpty()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var appVersion by rememberSaveable { mutableStateOf("") }
    var clientReference by rememberSaveable { mutableStateOf("") }
    var stepsToReproduce by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(TicketCategory.BUG) }
    var selectedPlatform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    val client = clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(title = "Create ticket", subtitle = "Open new work from the admin side with enough context for follow-up and billing.", eyebrow = "Admin intake", actions = { SecondaryButton(text = "Back", onClick = onBack) })
        SectionCard(title = "Client and scope", subtitle = "Pick the account first so the ticket inherits the right context.") {
            FilterBar(label = "Client", options = clients.map { FilterOption(it.id, it.companyName) }, selected = selectedClientId.takeIf { it.isNotBlank() }, onSelected = { selectedClientId = it.orEmpty() })
            InfoRow(label = "Affected app", value = client?.productName ?: "-")
        }
        SectionCard(title = "Issue overview", subtitle = "Subject, category and problem statement for the queue.") {
            OutlinedTextField(value = subject, onValueChange = { subject = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Subject") }, singleLine = true)
            FilterBar(label = "Category", options = TicketCategory.entries.map { FilterOption(it, it.displayName()) }, selected = selectedCategory, onSelected = { selectedCategory = it ?: TicketCategory.BUG })
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 5)
        }
        SectionCard(title = "Technical context", subtitle = "Only fields that help debugging and future reconstruction.") {
            FilterBar(label = "Platform", options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) }, selected = selectedPlatform, onSelected = { selectedPlatform = it ?: SupportPlatform.DESKTOP })
            OutlinedTextField(value = appVersion, onValueChange = { appVersion = it }, modifier = Modifier.fillMaxWidth(), label = { Text("App version") }, singleLine = true)
            OutlinedTextField(value = clientReference, onValueChange = { clientReference = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Client reference") }, singleLine = true)
            OutlinedTextField(value = stepsToReproduce, onValueChange = { stepsToReproduce = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Steps to reproduce") }, minLines = 4)
            PrimaryButton(text = "Create ticket", onClick = {
                onCreateTicket(CreateTicketInput(clientId = client?.id.orEmpty(), subject = subject, description = description, category = selectedCategory, affectedApp = client?.productName.orEmpty(), platform = selectedPlatform, appVersion = appVersion, stepsToReproduce = stepsToReproduce, clientReference = clientReference))
            }, enabled = client != null && subject.isNotBlank() && description.isNotBlank())
        }
    }
}
