package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientAccountStatus
import com.requena.supportdesk.core.model.ClientNote
import com.requena.supportdesk.core.model.ClientServiceTier
import com.requena.supportdesk.core.model.TaskCategory
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.core.utils.matchesQuery
import com.requena.supportdesk.designsystem.components.badges.ClientAccountStatusBadge
import com.requena.supportdesk.designsystem.components.badges.ClientServiceTierBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.tokens.SupportDeskMotion
import com.requena.supportdesk.features.clients.presentation.event.ClientsUiEvent
import com.requena.supportdesk.features.clients.presentation.state.ClientsUiState
import com.requena.supportdesk.features.tasks.presentation.state.TasksUiState
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

enum class ClientTab(val label: String) {
    RESUMEN("Resumen"),
    TAREAS("Tareas"),
    ETIQUETAS("Etiquetas"),
    TICKETS("Tickets"),
    FACTURAS("Facturas"),
    CREDENCIALES("Credenciales"),
    NOTAS("Notas"),
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "?" }

// Clients have no free-text tag field of their own; the "Etiquetas" the reference design
// shows are derived read-only from the task categories used in that client's own work.
private fun clientDerivedTags(client: Client, tasks: List<WorkTask>, categories: List<TaskCategory>): List<TaskCategory> {
    val categoryIds = tasks.filter { it.clientId == client.id }.map { it.categoryId }.toSet()
    return categories.filter { it.id in categoryIds }
}

// ─── Entry point ────────────────────────────────────────────────────────────────

@Composable
fun AdminClientsScreen(
    state: ClientsUiState,
    tasksState: TasksUiState,
    ticketsState: TicketsUiState,
    currentAdminId: String,
    currentAdminName: String,
    onEvent: (ClientsUiEvent) -> Unit,
    onNavigateToInvoices: (clientId: String) -> Unit,
    onNavigateToLabels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val selectedClient = state.selectedClient

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }

    LaunchedEffect(state.lastCreatedClientId) {
        if (state.lastCreatedClientId != null) showAddDialog = false
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        when {
            state.isLoading && state.clients.isEmpty() -> LoadingState(itemCount = 4)
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val stacked = maxWidth < SupportDeskBreakpoints.adminListDetailStacked
                    val listPanel: @Composable (Modifier) -> Unit = { m ->
                        ClientsListPanel(
                            clients = state.clients,
                            tasks = tasksState.tasks,
                            tickets = ticketsState.allTickets,
                            query = state.query,
                            selectedClientId = selectedClient?.id,
                            onQueryChange = { onEvent(ClientsUiEvent.SearchChanged(it)) },
                            onSelect = { onEvent(ClientsUiEvent.SelectClient(it)) },
                            onAddClient = { showAddDialog = true },
                            modifier = m,
                        )
                    }
                    val detailPanel: @Composable (Modifier) -> Unit = { m ->
                        ClientDetailPanel(
                            client = selectedClient,
                            tasks = tasksState.tasks,
                            categories = tasksState.categories,
                            tickets = ticketsState.allTickets,
                            currentAdminId = currentAdminId,
                            currentAdminName = currentAdminName,
                            isLoading = state.isLoading,
                            onEvent = onEvent,
                            onEditClient = { editingClient = selectedClient },
                            onUpdateCredentials = { clientId, email, password ->
                                onEvent(ClientsUiEvent.UpdateClientCredentials(clientId, email, password))
                            },
                            onNavigateToInvoices = onNavigateToInvoices,
                            onNavigateToLabels = onNavigateToLabels,
                            modifier = m,
                        )
                    }
                    if (stacked) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            listPanel(Modifier.weight(1f).fillMaxWidth())
                            detailPanel(Modifier.weight(1f).fillMaxWidth())
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            listPanel(Modifier.weight(1f).fillMaxSize())
                            detailPanel(Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditClientDialog(
            client = null,
            isLoading = state.isLoading,
            onDismiss = { showAddDialog = false },
            onEvent = onEvent,
        )
    }
    editingClient?.let { client ->
        AddEditClientDialog(
            client = client,
            isLoading = state.isLoading,
            onDismiss = { editingClient = null },
            onEvent = onEvent,
        )
    }
}

// ─── Left column: list ──────────────────────────────────────────────────────────

@Composable
private fun ClientsListPanel(
    clients: List<Client>,
    tasks: List<WorkTask>,
    tickets: List<Ticket>,
    query: String,
    selectedClientId: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onAddClient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var statusFilter by remember { mutableStateOf<ClientAccountStatus?>(null) }
    var tierFilter by remember { mutableStateOf<ClientServiceTier?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }

    val filteredClients = remember(clients, query, statusFilter, tierFilter) {
        clients.filter { client ->
            (statusFilter == null || client.accountStatus == statusFilter) &&
                (tierFilter == null || client.serviceTier == tierFilter) &&
                (query.isBlank() ||
                    client.companyName.matchesQuery(query) ||
                    client.productName.matchesQuery(query) ||
                    client.contactName.matchesQuery(query) ||
                    client.email.matchesQuery(query))
        }
    }
    val openTaskCountByClient = remember(tasks) {
        tasks.filter { !it.completed }.groupingBy { it.clientId }.eachCount()
    }
    val ticketCountByClient = remember(tickets) {
        tickets.groupingBy { it.clientId }.eachCount()
    }

    Surface(
        modifier = modifier.neonHolderBorder(SupportDeskThemeTokens.spacing.sm),
        shape = RoundedCornerShape(SupportDeskThemeTokens.spacing.sm),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(SupportDeskThemeTokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Clientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Consulta y enlaza contexto sin mezclar trabajo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PrimaryButton(text = "Agregar cliente", onClick = onAddClient, icon = Lucide.Plus)
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar cliente, contacto, empresa o email") },
                leadingIcon = { Icon(imageVector = Lucide.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SecondaryButton(text = if (filtersExpanded) "Ocultar filtros" else "Filtros", onClick = { filtersExpanded = !filtersExpanded })

            AnimatedVisibility(visible = filtersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    FilterBar(
                        label = "Estado",
                        options = ClientAccountStatus.entries.map { FilterOption(it, it.displayName()) },
                        selected = statusFilter,
                        onSelected = { statusFilter = it },
                    )
                    FilterBar(
                        label = "Tipo",
                        options = ClientServiceTier.entries.map { FilterOption(it, it.displayName()) },
                        selected = tierFilter,
                        onSelected = { tierFilter = it },
                    )
                    Text(
                        "Más filtros próximamente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (filteredClients.isEmpty()) {
                EmptyState(
                    title = "Sin clientes visibles",
                    message = "No hay clientes que coincidan con la búsqueda o los filtros actuales.",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    items(filteredClients, key = { it.id }) { client ->
                        ClientListItem(
                            client = client,
                            openTaskCount = openTaskCountByClient[client.id] ?: 0,
                            ticketCount = ticketCountByClient[client.id] ?: 0,
                            selected = client.id == selectedClientId,
                            onClick = { onSelect(client.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientListItem(
    client: Client,
    openTaskCount: Int,
    ticketCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            hovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        },
        animationSpec = tween(SupportDeskMotion.quick),
        label = "clientListItemBackground",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = client.companyName.initials(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(client.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    client.email.ifBlank { client.contactName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$openTaskCount tareas · $ticketCount tickets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ClientServiceTierBadge(client.serviceTier)
                ClientAccountStatusBadge(client.accountStatus)
            }
        }
    }
}

// ─── Right column: detail ───────────────────────────────────────────────────────

@Composable
private fun ClientDetailPanel(
    client: Client?,
    tasks: List<WorkTask>,
    categories: List<TaskCategory>,
    tickets: List<Ticket>,
    currentAdminId: String,
    currentAdminName: String,
    isLoading: Boolean,
    onEvent: (ClientsUiEvent) -> Unit,
    onEditClient: () -> Unit,
    onUpdateCredentials: (clientId: String, email: String, password: String) -> Unit,
    onNavigateToInvoices: (clientId: String) -> Unit,
    onNavigateToLabels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.neonHolderBorder(spacing.sm),
        shape = RoundedCornerShape(spacing.sm),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (client == null) {
            Box(modifier = Modifier.fillMaxSize().padding(spacing.lg), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Ningún cliente seleccionado",
                    message = "Elige un cliente de la lista para ver su ficha completa.",
                )
            }
            return@Surface
        }

        var confirmDelete by rememberSaveable(client.id) { mutableStateOf(false) }
        var selectedTab by rememberSaveable(client.id) { mutableStateOf(ClientTab.RESUMEN) }
        val clientTasks = remember(tasks, client.id) { tasks.filter { it.clientId == client.id } }
        val clientTickets = remember(tickets, client.id) { tickets.filter { it.clientId == client.id } }
        val derivedTags = remember(client.id, tasks, categories) { clientDerivedTags(client, tasks, categories) }

        Column(
            modifier = Modifier.fillMaxSize().padding(spacing.lg).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            ClientHeader(
                client = client,
                tags = derivedTags,
                onEditClient = onEditClient,
                onDeleteClient = { confirmDelete = true },
            )
            ClientQuickInfoRow(client = client)
            ClientTabs(selected = selectedTab, onSelect = { selectedTab = it })

            when (selectedTab) {
                ClientTab.RESUMEN -> ClientSummaryTab(
                    client = client,
                    tasks = clientTasks,
                    tickets = clientTickets,
                    tags = derivedTags,
                    currentAdminId = currentAdminId,
                    currentAdminName = currentAdminName,
                    onEvent = onEvent,
                    onNavigateToLabels = onNavigateToLabels,
                )
                ClientTab.TAREAS -> ClientTasksTab(tasks = clientTasks)
                ClientTab.ETIQUETAS -> ClientTagsTab(tags = derivedTags, onNavigateToLabels = onNavigateToLabels)
                ClientTab.TICKETS -> ClientTicketsTab(tickets = clientTickets)
                ClientTab.FACTURAS -> ClientInvoicesTab(client = client, onGenerate = { onNavigateToInvoices(client.id) })
                ClientTab.CREDENCIALES -> ClientCredentialsTab(
                    client = client,
                    isLoading = isLoading,
                    onSave = { email, password -> onUpdateCredentials(client.id, email, password) },
                )
                ClientTab.NOTAS -> ClientNotesTab(
                    client = client,
                    currentAdminId = currentAdminId,
                    currentAdminName = currentAdminName,
                    onEvent = onEvent,
                )
            }
        }

        ConfirmDialog(
            visible = confirmDelete,
            title = "Borrar cliente",
            message = "Si el cliente tiene tickets relacionados, el servidor bloqueará el borrado.",
            confirmText = "Borrar",
            dismissText = "Cancelar",
            onConfirm = {
                confirmDelete = false
                onEvent(ClientsUiEvent.DeleteClient(client.id))
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ClientHeader(
    client: Client,
    tags: List<TaskCategory>,
    onEditClient: () -> Unit,
    onDeleteClient: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var menuExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        client.companyName.initials(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(client.companyName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        tags.take(3).forEach { tag ->
                            TagChip(tag)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SecondaryButton(text = "Editar cliente", onClick = onEditClient)
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = Lucide.EllipsisVertical, contentDescription = "Más acciones")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Borrar cliente", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDeleteClient() },
                    )
                }
            }
        }
    }
}

// Neon-glow holder border: brighter accent-colored stroke plus a soft colored shadow.
// MaterialTheme.colorScheme.primary already resolves per light/dark theme, so this needs
// no separate dark-mode variant.
@Composable
// Flat, no drop shadow - matches the shared SectionCard/MetricCard neon-border treatment.
private fun Modifier.neonHolderBorder(cornerRadius: Dp): Modifier {
    val glow = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(cornerRadius)
    return this.border(BorderStroke(1.5.dp, glow), shape)
}

private fun parseHexColorOrDefault(hex: String?): Color = runCatching {
    val value = hex.orEmpty().removePrefix("#").toLong(16)
    Color(
        red = ((value shr 16) and 0xFF).toInt() / 255f,
        green = ((value shr 8) and 0xFF).toInt() / 255f,
        blue = (value and 0xFF).toInt() / 255f,
        alpha = 1f,
    )
}.getOrElse { Color(0xFF6B7A5B) }

@Composable
private fun TagChip(tag: TaskCategory) {
    val color = remember(tag.colorHex) { parseHexColorOrDefault(tag.colorHex) }
    Surface(modifier = Modifier.wrapContentWidth(), shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.16f)) {
        Text(
            tag.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ClientQuickInfoRow(client: Client) {
    val spacing = SupportDeskThemeTokens.spacing
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.xl)) {
        QuickInfoItem(label = "Email", value = client.email, modifier = Modifier.weight(1f))
        QuickInfoItem(label = "Empresa", value = client.productName.ifBlank { "—" }, modifier = Modifier.weight(1f))
        QuickInfoItem(label = "Contacto", value = client.contactName, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuickInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ClientTabs(selected: ClientTab, onSelect: (ClientTab) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        ClientTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(SupportDeskMotion.quick),
                label = "clientTabColor",
            )
            Column(
                modifier = Modifier.clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                Surface(
                    modifier = Modifier.size(width = 24.dp, height = 2.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ) {}
            }
        }
    }
}

// ─── Tab content ─────────────────────────────────────────────────────────────────

@Composable
private fun ClientSummaryTab(
    client: Client,
    tasks: List<WorkTask>,
    tickets: List<Ticket>,
    tags: List<TaskCategory>,
    currentAdminId: String,
    currentAdminName: String,
    onEvent: (ClientsUiEvent) -> Unit,
    onNavigateToLabels: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val openTasks = tasks.count { !it.completed }
    val minutesThisMonth = tasks.sumOf { it.loggedMinutes }
    val openTickets = tickets.count { it.status != com.requena.supportdesk.core.model.TicketStatus.CLOSED && it.status != com.requena.supportdesk.core.model.TicketStatus.RESOLVED }
    val recentTasks = remember(tasks) { tasks.sortedByDescending { it.updatedAt }.take(5) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            SupportDeskFormCard(title = "Resumen general", modifier = Modifier.weight(1f)) {
                InfoLine("Tareas abiertas", openTasks.toString())
                InfoLine("Horas este mes", formatSupportDeskDuration(minutesThisMonth))
                InfoLine("Tickets abiertos", openTickets.toString())
            }
            SupportDeskFormCard(title = "Actividad reciente", modifier = Modifier.weight(1f)) {
                if (recentTasks.isEmpty()) {
                    Text("Sin actividad reciente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    recentTasks.forEach { task ->
                        Text(
                            "${task.title} · ${formatSupportDeskDuration(task.loggedMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            SupportDeskFormCard(title = "Etiquetas", modifier = Modifier.weight(1f)) {
                if (tags.isEmpty()) {
                    Text("Sin etiquetas de tareas todavía.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        tags.forEach { TagChip(it) }
                    }
                }
                SecondaryButton(text = "Gestionar", onClick = onNavigateToLabels)
            }
            SupportDeskFormCard(title = "Notas rápidas", modifier = Modifier.weight(1f)) {
                val lastNote = client.notes.maxByOrNull { it.createdAt }
                Text(
                    lastNote?.body ?: "Sin notas todavía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lastNote == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SupportDeskFormCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = modifier.neonHolderBorder(spacing.sm),
        shape = RoundedCornerShape(spacing.sm),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ClientTasksTab(tasks: List<WorkTask>) {
    val spacing = SupportDeskThemeTokens.spacing
    if (tasks.isEmpty()) {
        EmptyState(title = "Sin tareas", message = "Este cliente todavía no tiene tareas vinculadas.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        tasks.sortedByDescending { it.updatedAt }.forEach { task ->
            ExpandableRow(
                summary = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatSupportDeskDuration(task.loggedMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                detail = {
                    if (task.description.isNotBlank()) {
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (task.completed) "Completada" else "En curso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    }
}

// Click to reveal more detail inline, without navigating away from the Clientes screen.
@Composable
private fun ExpandableRow(
    summary: @Composable () -> Unit,
    detail: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(
        targetValue = if (hovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(SupportDeskMotion.quick),
        label = "expandableRowBackground",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { expanded = !expanded }
            .animateContentSize(),
        color = background,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = spacing.xs, horizontal = spacing.xs)) {
            summary()
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.padding(top = spacing.xs)) {
                    detail()
                }
            }
        }
    }
}

@Composable
private fun ClientTagsTab(tags: List<TaskCategory>, onNavigateToLabels: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (tags.isEmpty()) {
            EmptyState(title = "Sin etiquetas", message = "Cuando este cliente tenga tareas categorizadas, sus etiquetas aparecerán aquí.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                tags.forEach { TagChip(it) }
            }
        }
        SecondaryButton(text = "Gestionar etiquetas", onClick = onNavigateToLabels)
    }
}

@Composable
private fun ClientTicketsTab(tickets: List<Ticket>) {
    val spacing = SupportDeskThemeTokens.spacing
    if (tickets.isEmpty()) {
        EmptyState(title = "Sin tickets", message = "Este cliente todavía no tiene tickets.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        tickets.sortedByDescending { it.updatedAt }.forEach { ticket ->
            ExpandableRow(
                summary = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(ticket.subject, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            TicketPriorityBadge(ticket.priority)
                            TicketStatusBadge(ticket.status)
                        }
                    }
                },
                detail = {
                    if (ticket.description.isNotBlank()) {
                        Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Solicitado por ${ticket.requester.name} · ${ticket.messages.size} mensajes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun ClientInvoicesTab(client: Client, onGenerate: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            "Las facturas se generan como PDF descargable y no se guardan en el servidor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(text = "Generar factura para ${client.companyName}", onClick = onGenerate)
    }
}

@Composable
private fun ClientCredentialsTab(
    client: Client,
    isLoading: Boolean,
    onSave: (email: String, password: String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var email by rememberSaveable(client.id) { mutableStateOf(client.email) }
    var password by remember(client.id) { mutableStateOf("") }
    var passwordVisible by remember(client.id) { mutableStateOf(false) }
    val canSave = email.isNotBlank() && email.contains("@") && password.length >= 8 && !isLoading

    SupportDeskFormCard(title = "Credenciales de acceso") {
        Text(
            "Define o reemplaza el acceso del cliente al portal. La clave no se muestra ni se guarda en esta ficha.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo de acceso") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Clave de acceso") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                        contentDescription = if (passwordVisible) "Ocultar clave" else "Mostrar clave",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Usa una clave de al menos 8 caracteres.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = "Guardar credenciales",
            enabled = canSave,
            isLoading = isLoading,
            fullWidth = true,
            onClick = {
                onSave(email.trim(), password)
                password = ""
                passwordVisible = false
            },
        )
    }
}

@Composable
private fun ClientNotesTab(
    client: Client,
    currentAdminId: String,
    currentAdminName: String,
    onEvent: (ClientsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var draft by remember(client.id) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Añadir una nota…") },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "Añadir",
                enabled = draft.isNotBlank(),
                onClick = {
                    onEvent(ClientsUiEvent.AddClientNote(client.id, draft, currentAdminId, currentAdminName))
                    draft = ""
                },
            )
        }
        val notes = client.notes.sortedByDescending { it.createdAt }
        if (notes.isEmpty()) {
            EmptyState(title = "Sin notas", message = "Todavía no hay notas para este cliente.")
        } else {
            notes.forEach { note -> ClientNoteRow(note) }
        }
    }
}

@Composable
private fun ClientNoteRow(note: ClientNote) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(spacing.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(note.body, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${note.authorName} · ${note.createdAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ClientAccountStatus.displayName(): String = when (this) {
    ClientAccountStatus.ACTIVE -> "Activo"
    ClientAccountStatus.PAUSED -> "Pausado"
    ClientAccountStatus.INACTIVE -> "Inactivo"
}

private fun ClientServiceTier.displayName(): String = when (this) {
    ClientServiceTier.STANDARD -> "Estándar"
    ClientServiceTier.PRIORITY -> "Priority"
    ClientServiceTier.VIP -> "VIP"
}
