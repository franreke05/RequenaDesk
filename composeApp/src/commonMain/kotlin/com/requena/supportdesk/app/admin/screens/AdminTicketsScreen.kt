package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.CommentBubble
import com.requena.supportdesk.designsystem.components.tickets.MessageBubble
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

@Composable
fun AdminTicketsScreen(
    layoutMode: AdminLayoutMode,
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenCreateTicket: () -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = "Tickets",
            subtitle = "Cola operativa con conversación, estado y prioridad sincronizados.",
            eyebrow = "Soporte",
            actions = { PrimaryButton(text = "Crear ticket", onClick = onOpenCreateTicket) },
        )
        if (layoutMode == AdminLayoutMode.EXPANDED) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TicketListPane(state = state, onEvent = onEvent, onOpenDetail = onOpenDetail, modifier = Modifier.weight(0.42f))
                TicketDetailPane(
                    ticket = state.selectedTicket,
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
    onBack: () -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = ticket?.subject ?: "Detalle del ticket",
            subtitle = ticket?.affectedApp ?: "Revisa la conversación y el flujo de soporte.",
            eyebrow = ticket?.ticketNumber ?: "Soporte",
            actions = { SecondaryButton(text = "Volver", onClick = onBack) },
        )
        TicketDetailPane(ticket = ticket, onEvent = onEvent, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TicketListPane(state: TicketsUiState, onEvent: (TicketsUiEvent) -> Unit, onOpenDetail: (Ticket) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier, title = "Cola", subtitle = "${state.tickets.size} tickets visibles tras aplicar filtros.") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            SearchField(value = state.searchQuery, onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) }, placeholder = "Buscar ticket, cliente o aplicación")
            FilterBar(label = "Estado", options = TicketStatus.entries.map { FilterOption(it, it.displayName()) }, selected = state.statusFilter, onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) })
            FilterBar(label = "Prioridad", options = TicketPriority.entries.map { FilterOption(it, it.displayName()) }, selected = state.priorityFilter, onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) })
            FilterBar(label = "Categoría", options = TicketCategory.entries.map { FilterOption(it, it.displayName()) }, selected = state.categoryFilter, onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) })
            FilterBar(label = "Plataforma", options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) }, selected = state.platformFilter, onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) })
            FilterBar(label = "Pendiente de", options = WaitingOn.entries.map { FilterOption(it, it.displayName()) }, selected = state.waitingOnFilter, onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) })
            when {
                state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 5)
                state.tickets.isEmpty() -> EmptyState(title = "No se encontraron tickets", message = state.errorMessage ?: "Limpia los filtros o crea un ticket nuevo.")
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
private fun TicketDetailPane(ticket: Ticket?, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Crossfade(
        targetState = ticket,
        modifier = modifier,
        animationSpec = tween(SupportDeskMotion.regular),
        label = "ticketDetailPane",
    ) { current ->
        if (current == null) {
            EmptyState(title = "Ningún ticket seleccionado", message = "Selecciona un ticket para revisar la conversación y su estado.")
        } else {
            var replyDraft by rememberSaveable(current.id) { mutableStateOf("") }
            SectionCard {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        TicketStatusBadge(current.status); TicketPriorityBadge(current.priority); WaitingOnBadge(current.waitingOn); TicketCategoryBadge(current.category); SupportPlatformBadge(current.platform)
                    }
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stacked = maxWidth < SupportDeskBreakpoints.adminTicketsStacked
                        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            TicketContextCard(current); TicketOpsCard(current, replyDraft, { replyDraft = it }, onEvent)
                        } else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            TicketContextCard(current, Modifier.weight(0.58f))
                            TicketOpsCard(current, replyDraft, { replyDraft = it }, onEvent, Modifier.weight(0.42f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketContextCard(ticket: Ticket, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(title = "Contexto del ticket", subtitle = "Solicitante, aplicación y detalle técnico.") {
            InfoRow(label = "Solicitante", value = ticket.requester.name, supportingText = ticket.requester.email)
            InfoRow(label = "Aplicación afectada", value = ticket.affectedApp)
            InfoRow(label = "Versión", value = ticket.appVersion ?: "-")
            InfoRow(label = "Referencia", value = ticket.clientReference ?: "-")
            InfoRow(label = "Pasos", value = ticket.stepsToReproduce ?: "Sin pasos de reproducción.")
            InfoRow(label = "Actualizado", value = formatSupportDeskDateTime(ticket.updatedAt))
        }
        SectionCard(title = "Conversación", subtitle = "${ticket.messages.size} mensajes visibles.") {
            if (ticket.messages.isEmpty()) EmptyState(title = "Todavía no hay mensajes", message = "La conversación aparecerá cuando el ticket reciba respuestas.") else Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                ticket.messages.forEach { MessageBubble(authorName = it.authorName, body = it.body, timestamp = it.createdAt, isOwnMessage = false) }
            }
        }
        if (ticket.internalComments.isNotEmpty()) {
            SectionCard(title = "Notas internas", subtitle = "Contexto persistido por el equipo.") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    ticket.internalComments.forEach {
                        CommentBubble(authorName = it.authorName, body = it.body, timestamp = it.createdAt)
                    }
                }
            }
        }
        if (ticket.attachments.isNotEmpty()) SectionCard(title = "Adjuntos", subtitle = "${ticket.attachments.size} archivos vinculados.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) { ticket.attachments.forEach { AttachmentRow(it) } }
        }
    }
}

@Composable
private fun TicketOpsCard(ticket: Ticket, replyDraft: String, onReplyDraftChange: (String) -> Unit, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    // Only clear the draft once the message count grows past the value captured when this
    // ticket's own reply was submitted; reacting to any message-count change would also wipe
    // an in-progress draft if the ticket picks up messages from another source (e.g. re-selecting
    // the same ticket in the list pane while composing).
    var pendingReplyBaseline by remember(ticket.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(ticket.messages.size) {
        val baseline = pendingReplyBaseline
        if (baseline != null && ticket.messages.size > baseline) {
            onReplyDraftChange("")
            pendingReplyBaseline = null
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(title = "Flujo", subtitle = "Actualiza estado y prioridad desde el detalle.") {
            Text("Estado", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FilterBar(label = "Estado", options = TicketStatus.entries.map { FilterOption(it, it.displayName()) }, selected = ticket.status, onSelected = { it?.let { onEvent(TicketsUiEvent.ChangeSelectedStatus(it)) } })
            Text("Prioridad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FilterBar(label = "Prioridad", options = TicketPriority.entries.map { FilterOption(it, it.displayName()) }, selected = ticket.priority, onSelected = { it?.let { onEvent(TicketsUiEvent.ChangeSelectedPriority(it)) } })
        }
        SectionCard(title = "Respuesta", subtitle = "El siguiente mensaje será visible para el cliente.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedTextField(value = replyDraft, onValueChange = onReplyDraftChange, modifier = Modifier.fillMaxWidth(), label = { Text("Respuesta al cliente") }, minLines = 4)
                PrimaryButton(
                    text = "Enviar respuesta",
                    onClick = {
                        pendingReplyBaseline = ticket.messages.size
                        onEvent(TicketsUiEvent.ReplyToSelected(replyDraft))
                    },
                    enabled = replyDraft.isNotBlank(),
                )
            }
        }
    }
}

@Composable
fun AdminCreateTicketScreen(
    clients: List<Client>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onCreateTicket: (CreateTicketInput) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        PageHeader(title = "Crear ticket", subtitle = "Registra la incidencia con el contexto necesario para poder resolverla.", eyebrow = "Alta de soporte", actions = { SecondaryButton(text = "Volver", onClick = onBack) })
        SectionCard(title = "Cliente y alcance", subtitle = "Selecciona primero la cuenta asociada al ticket.") {
            FilterBar(label = "Cliente", options = clients.map { FilterOption(it.id, it.companyName) }, selected = selectedClientId.takeIf { it.isNotBlank() }, onSelected = { selectedClientId = it.orEmpty() })
            InfoRow(label = "Aplicación afectada", value = client?.productName ?: "-")
        }
        SectionCard(title = "Resumen", subtitle = "Asunto, categoría y descripción para clasificar la solicitud.") {
            OutlinedTextField(value = subject, onValueChange = { subject = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Asunto") }, singleLine = true)
            FilterBar(label = "Categoría", options = TicketCategory.entries.map { FilterOption(it, it.displayName()) }, selected = selectedCategory, onSelected = { selectedCategory = it ?: TicketCategory.BUG })
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Descripción") }, minLines = 5)
        }
        SectionCard(title = "Contexto técnico", subtitle = "Incluye únicamente los datos útiles para investigar el problema.") {
            FilterBar(label = "Plataforma", options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) }, selected = selectedPlatform, onSelected = { selectedPlatform = it ?: SupportPlatform.DESKTOP })
            OutlinedTextField(value = appVersion, onValueChange = { appVersion = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Versión de la aplicación") }, singleLine = true)
            OutlinedTextField(value = clientReference, onValueChange = { clientReference = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Referencia del cliente") }, singleLine = true)
            OutlinedTextField(value = stepsToReproduce, onValueChange = { stepsToReproduce = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Pasos para reproducir") }, minLines = 4)
            errorMessage?.let { Text(text = it, color = SupportDeskThemeTokens.semanticColors.danger) }
            PrimaryButton(text = "Crear ticket", onClick = {
                onCreateTicket(CreateTicketInput(clientId = client?.id.orEmpty(), subject = subject, description = description, category = selectedCategory, affectedApp = client?.productName.orEmpty(), platform = selectedPlatform, appVersion = appVersion, stepsToReproduce = stepsToReproduce, clientReference = clientReference))
            }, enabled = !isSubmitting && client != null && subject.isNotBlank() && description.isNotBlank(), isLoading = isSubmitting)
        }
    }
}
