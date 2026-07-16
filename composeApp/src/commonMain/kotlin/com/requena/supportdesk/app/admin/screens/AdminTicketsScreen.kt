package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
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
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Funnel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.X

@Composable
fun AdminTicketsScreen(
    layoutMode: AdminLayoutMode,
    state: TicketsUiState,
    clients: List<Client>,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var showCreateDialog by remember { mutableStateOf(false) }
    val expanded = layoutMode == AdminLayoutMode.EXPANDED

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        PageHeader(
            title = "Tickets",
            subtitle = "Cola operativa con conversación, estado y prioridad sincronizados.",
            eyebrow = "Soporte",
            actions = { PrimaryButton(text = "Crear ticket", onClick = { showCreateDialog = true }) },
        )
        if (expanded) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TicketListPane(
                    state = state,
                    onEvent = onEvent,
                    // Detail is already shown inline in this layout - selecting a row only needs
                    // to update ViewModel state, not navigate away and lose the list column.
                    onOpenDetail = { onEvent(TicketsUiEvent.SelectTicket(it.id)) },
                    modifier = Modifier.weight(0.4f),
                )
                TicketDetailPane(
                    ticket = state.selectedTicket,
                    clients = clients,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.6f),
                )
            }
        } else {
            TicketListPane(
                state = state,
                onEvent = onEvent,
                onOpenDetail = { ticket ->
                    onEvent(TicketsUiEvent.SelectTicket(ticket.id))
                    onOpenDetail(ticket)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showCreateDialog) {
        CreateTicketDialog(
            clients = clients,
            isSubmitting = state.isLoading,
            errorMessage = state.errorMessage,
            onDismiss = { showCreateDialog = false },
            onCreateTicket = { input -> onEvent(TicketsUiEvent.CreateTicket(input)) },
        )
    }
    LaunchedEffect(state.lastCreatedTicketId) {
        if (state.lastCreatedTicketId != null) {
            showCreateDialog = false
        }
    }
}

@Composable
fun AdminTicketDetailScreen(
    ticket: Ticket?,
    clients: List<Client>,
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
        TicketDetailPane(ticket = ticket, clients = clients, onEvent = onEvent, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun TicketListPane(
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier, title = "Cola", subtitle = "${state.tickets.size} tickets visibles tras aplicar filtros.") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            SearchField(
                value = state.searchQuery,
                onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
                placeholder = "Buscar ticket, cliente, asunto o referencia",
            )
            TicketsFiltersRow(state = state, onEvent = onEvent)
            when {
                state.isLoading && state.tickets.isEmpty() -> LoadingState(itemCount = 5)
                state.tickets.isEmpty() -> EmptyState(
                    title = "No hay tickets visibles",
                    message = state.errorMessage ?: "Prueba a limpiar filtros o crea un nuevo ticket.",
                )
                else -> LazyColumn(modifier = Modifier.heightIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
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
        }
    }
}

// Estado/Prioridad/Plataforma stay visible at all times as chip-dropdowns (matching the reference
// design), since they're the filters used most often. Categoría and Pendiente de live behind the
// "Filtros" popover to avoid a sixth/seventh always-on chip crowding the queue header.
@Composable
private fun TicketsFiltersRow(
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    var moreFiltersExpanded by remember { mutableStateOf(false) }
    val hasActiveFilters = state.statusFilter != null ||
        state.priorityFilter != null ||
        state.categoryFilter != null ||
        state.platformFilter != null ||
        state.waitingOnFilter != null

    FlowRow(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TicketFilterChipDropdown(
            label = "Estado",
            options = TicketStatus.entries.map { FilterOption(it, it.displayName()) },
            selected = state.statusFilter,
            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
        )
        TicketFilterChipDropdown(
            label = "Prioridad",
            options = TicketPriority.entries.map { FilterOption(it, it.displayName()) },
            selected = state.priorityFilter,
            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
        )
        TicketFilterChipDropdown(
            label = "Plataforma",
            options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) },
            selected = state.platformFilter,
            onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) },
        )
        Box {
            OutlinedButton(onClick = { moreFiltersExpanded = true }, shape = MaterialTheme.shapes.small) {
                Icon(imageVector = Lucide.Funnel, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Filtros", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
            }
            DropdownMenu(
                expanded = moreFiltersExpanded,
                onDismissRequest = { moreFiltersExpanded = false },
                modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
            ) {
                FilterSection(
                    label = "Categoría",
                    options = TicketCategory.entries.map { FilterOption(it, it.displayName()) },
                    selected = state.categoryFilter,
                    onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) },
                )
                FilterSection(
                    label = "Pendiente de",
                    options = WaitingOn.entries.map { FilterOption(it, it.displayName()) },
                    selected = state.waitingOnFilter,
                    onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) },
                )
            }
        }
        if (hasActiveFilters) {
            Text(
                text = "Limpiar filtros",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = { onEvent(TicketsUiEvent.ClearFilters) }),
            )
        }
    }
}

// Chip that both displays the current value of one filter dimension and opens a small menu to
// change it - clicking the label opens the menu, clicking the trailing X (shown only when a
// non-default value is active) resets that one dimension without opening anything.
@Composable
private fun <T> TicketFilterChipDropdown(
    label: String,
    options: List<FilterOption<T>>,
    selected: T?,
    onSelected: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val valueLabel = options.firstOrNull { it.value == selected }?.label ?: "Todos"
    Box {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(start = 10.dp, end = if (selected != null) 6.dp else 10.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "$label: $valueLabel", style = MaterialTheme.typography.labelMedium)
            if (selected != null) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Quitar filtro $label",
                    modifier = Modifier.size(14.dp).clickable(onClick = { onSelected(null) }),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { expanded = false; onSelected(null) })
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { expanded = false; onSelected(option.value) },
                )
            }
        }
    }
}

@Composable
private fun <T> FilterSection(
    label: String,
    options: List<FilterOption<T>>,
    selected: T?,
    onSelected: (T?) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option.value,
                    onClick = { onSelected(if (selected == option.value) null else option.value) },
                    label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

private enum class TicketTab(val label: String) {
    CONVERSACION("Conversación"),
    FLUJO("Flujo"),
    INFORMACION("Información"),
    ARCHIVOS("Archivos"),
}

@Composable
private fun TicketDetailPane(
    ticket: Ticket?,
    clients: List<Client>,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            var selectedTab by rememberSaveable(current.id) { mutableStateOf(TicketTab.CONVERSACION) }
            var replyDraft by rememberSaveable(current.id) { mutableStateOf("") }
            // Estado/Prioridad are edited as a local draft and only sent to the backend when the
            // admin explicitly saves - the reference design's "Guardar cambios"/"Actualizar flujo"
            // buttons imply a deliberate save step rather than firing a PATCH per click.
            var pendingStatus by remember(current.id) { mutableStateOf(current.status) }
            var pendingPriority by remember(current.id) { mutableStateOf(current.priority) }
            val hasPendingFlujoChanges = pendingStatus != current.status || pendingPriority != current.priority
            val commitFlujoChanges: () -> Unit = {
                if (pendingStatus != current.status) onEvent(TicketsUiEvent.ChangeSelectedStatus(pendingStatus))
                if (pendingPriority != current.priority) onEvent(TicketsUiEvent.ChangeSelectedPriority(pendingPriority))
            }
            val client = clients.firstOrNull { it.id == current.clientId }

            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                TicketHeaderCard(
                    ticket = current,
                    hasPendingFlujoChanges = hasPendingFlujoChanges,
                    onSaveFlujoChanges = commitFlujoChanges,
                    onResolve = { onEvent(TicketsUiEvent.ChangeSelectedStatus(TicketStatus.RESOLVED)) },
                    onClose = { onEvent(TicketsUiEvent.ChangeSelectedStatus(TicketStatus.CLOSED)) },
                )
                TicketInfoRow(ticket = current, client = client)
                TicketTabs(selected = selectedTab, onSelect = { selectedTab = it })
                when (selectedTab) {
                    TicketTab.CONVERSACION -> TicketConversationTab(
                        ticket = current,
                        replyDraft = replyDraft,
                        onReplyDraftChange = { replyDraft = it },
                        onEvent = onEvent,
                    )
                    TicketTab.FLUJO -> TicketFlujoTab(
                        ticket = current,
                        pendingStatus = pendingStatus,
                        onPendingStatusChange = { pendingStatus = it },
                        pendingPriority = pendingPriority,
                        onPendingPriorityChange = { pendingPriority = it },
                        hasPendingChanges = hasPendingFlujoChanges,
                        onCommit = commitFlujoChanges,
                    )
                    TicketTab.INFORMACION -> TicketInformationTab(ticket = current)
                    TicketTab.ARCHIVOS -> TicketAttachmentsTab(ticket = current)
                }
            }
        }
    }
}

@Composable
private fun TicketHeaderCard(
    ticket: Ticket,
    hasPendingFlujoChanges: Boolean,
    onSaveFlujoChanges: () -> Unit,
    onResolve: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var menuExpanded by remember { mutableStateOf(false) }
    SectionCard(modifier = modifier, title = null, subtitle = null) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${ticket.ticketNumber}   ${ticket.platform.displayName()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = ticket.subject, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    SecondaryButton(text = "Guardar cambios", enabled = hasPendingFlujoChanges, onClick = onSaveFlujoChanges)
                    PrimaryButton(text = "Resolver ticket", enabled = ticket.status != TicketStatus.RESOLVED, onClick = onResolve)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Lucide.EllipsisVertical, contentDescription = "Más acciones")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Cerrar ticket") },
                                enabled = ticket.status != TicketStatus.CLOSED,
                                onClick = { menuExpanded = false; onClose() },
                            )
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                TicketStatusBadge(ticket.status)
                TicketPriorityBadge(ticket.priority)
            }
            Text(
                text = "Actualizado ${formatSupportDeskDateTime(ticket.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TicketInfoRow(ticket: Ticket, client: Client?, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier, title = null, subtitle = null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xl), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TicketInfoItem(icon = Lucide.UserRound, label = "Cliente", value = ticket.requester.name)
            TicketInfoItem(icon = Lucide.Mail, label = "Email", value = ticket.requester.email)
            TicketInfoItem(icon = Lucide.Building2, label = "Empresa", value = client?.companyName ?: "-")
            TicketInfoItem(icon = Lucide.Clock, label = "Actualizado", value = formatSupportDeskDateTime(ticket.updatedAt))
        }
    }
}

@Composable
private fun TicketInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TicketTabs(selected: TicketTab, onSelect: (TicketTab) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        TicketTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier.clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 2.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun TicketConversationTab(
    ticket: Ticket,
    replyDraft: String,
    onReplyDraftChange: (String) -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    // Only clear the draft once the message count grows past the value captured when this
    // ticket's own reply was submitted; reacting to any message-count change would also wipe
    // an in-progress draft if the ticket picks up messages from another source.
    var pendingReplyBaseline by remember(ticket.id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(ticket.messages.size) {
        val baseline = pendingReplyBaseline
        if (baseline != null && ticket.messages.size > baseline) {
            onReplyDraftChange("")
            pendingReplyBaseline = null
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(title = "Conversación", subtitle = "${ticket.messages.size} mensajes visibles.") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                if (ticket.messages.isEmpty()) {
                    EmptyState(title = "Todavía no hay mensajes", message = "La conversación aparecerá cuando el ticket reciba respuestas.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        ticket.messages.forEach { message ->
                            val isFromClient = message.authorId == ticket.requester.id
                            MessageBubble(
                                authorName = message.authorName,
                                body = message.body,
                                timestamp = message.createdAt,
                                isOwnMessage = !isFromClient,
                                roleLabel = if (isFromClient) "Cliente" else "Admin",
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = replyDraft,
                        onValueChange = onReplyDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe una respuesta al cliente...") },
                        minLines = 1,
                        maxLines = 4,
                    )
                    PrimaryButton(
                        text = "Enviar respuesta",
                        icon = Lucide.Send,
                        enabled = replyDraft.isNotBlank(),
                        onClick = {
                            pendingReplyBaseline = ticket.messages.size
                            onEvent(TicketsUiEvent.ReplyToSelected(replyDraft))
                        },
                    )
                }
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
    }
}

@Composable
private fun TicketFlujoTab(
    ticket: Ticket,
    pendingStatus: TicketStatus,
    onPendingStatusChange: (TicketStatus) -> Unit,
    pendingPriority: TicketPriority,
    onPendingPriorityChange: (TicketPriority) -> Unit,
    hasPendingChanges: Boolean,
    onCommit: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Flujo del ticket", subtitle = "Actualiza el estado y la prioridad del ticket.") {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            TicketFieldDropdown(
                label = "Estado",
                options = TicketStatus.entries.map { FilterOption(it, it.displayName()) },
                selected = pendingStatus,
                onSelected = onPendingStatusChange,
            )
            TicketFieldDropdown(
                label = "Prioridad",
                options = TicketPriority.entries.map { FilterOption(it, it.displayName()) },
                selected = pendingPriority,
                onSelected = onPendingPriorityChange,
            )
            InfoRow(label = "Pendiente de", value = ticket.waitingOn.displayName())
            InfoRow(label = "Categoría", value = ticket.category.displayName())
            InfoRow(label = "Plataforma", value = ticket.platform.displayName())
            Text(
                text = "\"Pendiente de\" lo calcula el servidor al cambiar el estado. Categoría y plataforma se fijan al crear el ticket - el backend actual no expone un endpoint para editarlas después.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = "Actualizar flujo",
                enabled = hasPendingChanges,
                fullWidth = true,
                onClick = onCommit,
            )
        }
    }
}

@Composable
private fun TicketInformationTab(ticket: Ticket) {
    SectionCard(title = "Información del ticket", subtitle = "Contexto técnico y metadatos.") {
        InfoRow(label = "Aplicación afectada", value = ticket.affectedApp)
        InfoRow(label = "Versión", value = ticket.appVersion ?: "-")
        InfoRow(label = "Referencia del cliente", value = ticket.clientReference ?: "-")
        InfoRow(label = "Pasos para reproducir", value = ticket.stepsToReproduce ?: "Sin pasos de reproducción.")
        InfoRow(label = "Creado", value = formatSupportDeskDateTime(ticket.createdAt))
        InfoRow(label = "Actualizado", value = formatSupportDeskDateTime(ticket.updatedAt))
        InfoRow(label = "Id interno", value = ticket.id)
        ticket.resolutionSummary?.let { InfoRow(label = "Resumen de resolución", value = it) }
    }
}

@Composable
private fun TicketAttachmentsTab(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(title = "Adjuntos", subtitle = "${ticket.attachments.size} archivos vinculados.") {
        if (ticket.attachments.isEmpty()) {
            EmptyState(title = "Sin adjuntos", message = "Los archivos que se suban a este ticket aparecerán aquí.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                ticket.attachments.forEach { AttachmentRow(it) }
            }
        }
    }
}

@Composable
private fun <T> TicketFieldDropdown(
    label: String,
    options: List<FilterOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label ?: label
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(text = selectedLabel, modifier = Modifier.weight(1f))
                Icon(imageVector = Lucide.ChevronDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.widthIn(min = 200.dp)) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onSelected(option.value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTicketDialog(
    clients: List<Client>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreateTicket: (CreateTicketInput) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var selectedClientId by rememberSaveable { mutableStateOf(clients.firstOrNull()?.id.orEmpty()) }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var appVersion by rememberSaveable { mutableStateOf("") }
    var clientReference by rememberSaveable { mutableStateOf("") }
    var stepsToReproduce by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(TicketCategory.BUG) }
    var selectedPlatform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    val client = clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()
    val isValid = client != null && subject.isNotBlank() && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Crear ticket", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(text = "Registra la incidencia con el contexto necesario para poder resolverla.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TicketFieldDropdown(
                    label = "Cliente",
                    options = clients.map { FilterOption(it.id, it.companyName) },
                    selected = selectedClientId,
                    onSelected = { selectedClientId = it },
                )
                OutlinedTextField(value = subject, onValueChange = { subject = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Asunto") }, singleLine = true)
                TicketFieldDropdown(
                    label = "Categoría",
                    options = TicketCategory.entries.map { FilterOption(it, it.displayName()) },
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it },
                )
                OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Descripción") }, minLines = 4)
                TicketFieldDropdown(
                    label = "Plataforma",
                    options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) },
                    selected = selectedPlatform,
                    onSelected = { selectedPlatform = it },
                )
                OutlinedTextField(value = appVersion, onValueChange = { appVersion = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Versión de la aplicación") }, singleLine = true)
                OutlinedTextField(value = clientReference, onValueChange = { clientReference = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Referencia del cliente") }, singleLine = true)
                OutlinedTextField(value = stepsToReproduce, onValueChange = { stepsToReproduce = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Pasos para reproducir") }, minLines = 3)
                errorMessage?.let { Text(text = it, color = SupportDeskThemeTokens.semanticColors.danger) }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Crear ticket",
                enabled = isValid && !isSubmitting,
                isLoading = isSubmitting,
                onClick = {
                    onCreateTicket(
                        CreateTicketInput(
                            clientId = client?.id.orEmpty(),
                            subject = subject,
                            description = description,
                            category = selectedCategory,
                            affectedApp = client?.productName.orEmpty(),
                            platform = selectedPlatform,
                            appVersion = appVersion,
                            stepsToReproduce = stepsToReproduce,
                            clientReference = clientReference,
                        ),
                    )
                },
            )
        },
        dismissButton = {
            SecondaryButton(text = "Cancelar", onClick = onDismiss)
        },
    )
}
