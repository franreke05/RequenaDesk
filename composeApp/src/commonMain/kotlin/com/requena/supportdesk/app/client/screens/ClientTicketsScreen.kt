package com.requena.supportdesk.app.client.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.app.client.initials
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.app.client.components.ClientPortalMetric as MetricCard
import com.requena.supportdesk.app.client.components.ClientPortalSectionCard as SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

// ── TICKETS ───────────────────────────────────────────────────────────────────

@Composable
fun ClientTicketsScreen(
    state: TicketsUiState,
    companyName: String = "",
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val statusOptions = remember { TicketStatus.entries.map { FilterOption(it, it.displayName()) } }
    val priorityOptions = remember { TicketPriority.entries.map { FilterOption(it, it.displayName()) } }
    val allTickets = state.allTickets.ifEmpty { state.tickets }
    val statusCounts = remember(allTickets) { TicketStatus.entries.map { s -> s to allTickets.count { it.status == s } } }
    val priorityCounts = remember(allTickets) { TicketPriority.entries.map { p -> p to allTickets.count { it.priority == p } } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Deliberately decoupled: `stacked` (3-panel split) and `cols` (grid density) used
        // to share one threshold (the old clientStacked=820dp), which meant lowering either
        // one moved both together. Verified math at the 2-column threshold: the grid panel
        // gets a fixed-260dp Filters panel + a 0.48 weight share + the card's own internal
        // padding taken out first, landing around ~87dp/column at 820dp width - well under
        // that going lower. `stacked` has no such constraint (a single column always has
        // room), so it can drop to clientMedium without cramping anything.
        val stacked = maxWidth < SupportDeskBreakpoints.clientMedium
        val cols = when {
            maxWidth >= SupportDeskBreakpoints.clientUltraWide -> 3
            // TODO(wave-1): derive from the grid panel's own measured width (its own
            // BoxWithConstraints) rather than the outer maxWidth - today's threshold only
            // works because it happens to match the current fixed-260dp filters panel/weight
            // split; either of those changing would silently invalidate this number.
            maxWidth >= SupportDeskBreakpoints.clientWide -> 2
            else -> 1
        }
        if (stacked) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item {
                    ClientFiltersPanel(
                        state = state,
                        statusOptions = statusOptions,
                        priorityOptions = priorityOptions,
                        statusCounts = statusCounts,
                        priorityCounts = priorityCounts,
                        onEvent = onEvent,
                    )
                }
                item {
                    ClientTicketGridPanel(
                        state = state,
                        companyName = companyName,
                        cols = cols,
                        onEvent = onEvent,
                    )
                }
                item {
                    ClientTicketDetailPanel(ticket = state.selectedTicket, onEvent = onEvent)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                ClientFiltersPanel(
                    modifier = Modifier.width(260.dp).fillMaxHeight(),
                    state = state,
                    statusOptions = statusOptions,
                    priorityOptions = priorityOptions,
                    statusCounts = statusCounts,
                    priorityCounts = priorityCounts,
                    onEvent = onEvent,
                )
                ClientTicketGridPanel(
                    state = state,
                    companyName = companyName,
                    cols = cols,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.48f).fillMaxHeight(),
                )
                AnimatedContent(
                    targetState = state.selectedTicket,
                    transitionSpec = {
                        if (targetState != null) {
                            (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 8 }) togetherWith
                                fadeOut(tween(200))
                        } else {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                        }
                    },
                    label = "ticketDetailTransition",
                    modifier = Modifier.weight(0.52f).fillMaxHeight(),
                ) { ticket ->
                    ClientTicketDetailPanel(
                        ticket = ticket,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientFiltersPanel(
    state: TicketsUiState,
    statusOptions: List<FilterOption<TicketStatus>>,
    priorityOptions: List<FilterOption<TicketPriority>>,
    statusCounts: List<Pair<TicketStatus, Int>>,
    priorityCounts: List<Pair<TicketPriority, Int>>,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier.fillMaxWidth(), title = "Filtros", subtitle = "${state.tickets.size} tickets visibles") {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )
        FilterBar(
            label = "Estado",
            options = statusOptions,
            selected = state.statusFilter,
            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
            wrap = true,
            allLabel = "Todos",
        )
        FilterBar(
            label = "Prioridad",
            options = priorityOptions,
            selected = state.priorityFilter,
            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
            wrap = true,
            allLabel = "Todas",
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            statusCounts.forEach { (status, count) ->
                BoardCounter(
                    label = status.displayName(),
                    value = count.toString(),
                    selected = state.statusFilter == status,
                    onClick = { onEvent(TicketsUiEvent.StatusFilterChanged(if (state.statusFilter == status) null else status)) },
                    modifier = Modifier.widthIn(min = 60.dp),
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            priorityCounts.forEach { (priority, count) ->
                BoardCounter(
                    label = priority.displayName(),
                    value = count.toString(),
                    selected = state.priorityFilter == priority,
                    onClick = { onEvent(TicketsUiEvent.PriorityFilterChanged(if (state.priorityFilter == priority) null else priority)) },
                    modifier = Modifier.widthIn(min = 60.dp),
                )
            }
        }
    }
}

@Composable
private fun BoardCounter(label: String, value: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClientTicketGridPanel(
    state: TicketsUiState,
    companyName: String,
    cols: Int,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Mis tickets",
        subtitle = "${state.tickets.size} total",
    ) {
        Crossfade(targetState = state.isLoading, label = "ticketGridLoad") { loading ->
            if (loading) {
                LoadingState(itemCount = 6)
            } else if (state.tickets.isEmpty()) {
                EmptyState(
                    title = "Sin tickets",
                    message = state.errorMessage ?: "Cuando abras un ticket aparecerá aquí.",
                )
            } else {
                val rows = (state.tickets.size + cols - 1) / cols
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((rows * 220).coerceAtLeast(300).dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    userScrollEnabled = false,
                ) {
                    gridItems(state.tickets, key = { it.id }) { ticket ->
                        TicketGridCard(
                            ticket = ticket,
                            companyName = companyName,
                            selected = state.selectedTicket?.id == ticket.id,
                            onClick = { onEvent(TicketsUiEvent.SelectTicket(ticket.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketGridCard(
    ticket: Ticket,
    companyName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    // Flat card, no blurred Material elevation - selection/hover read through a
    // crisper ink/accent border instead, matching the app's hairline card language.
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "gridCardBorder",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            borderWidth,
            when {
                selected -> MaterialTheme.colorScheme.onSurface
                hovered -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                // Chip de empresa + número de ticket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (companyName.isNotBlank()) {
                        SupportDeskBadge(
                            text = companyName.initials(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Text(
                        text = ticket.ticketNumber.ifBlank { "#—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Asunto
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Estado + prioridad
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TicketStatusBadge(ticket.status)
                    TicketPriorityBadge(ticket.priority)
                }
                // Categoría + plataforma
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TicketCategoryBadge(ticket.category)
                    SupportPlatformBadge(ticket.platform)
                }
                // Footer: asignado + horas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ticket.assignee?.name?.let { "Admin: $it" } ?: "Sin asignar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ticket.timeEntries.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientTicketDetailPanel(
    ticket: Ticket?,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "Selecciona un ticket",
            message = "El detalle, horas y cierre apareceran aqui.",
            modifier = modifier,
        )
        return
    }
    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = ticket.subject,
        subtitle = ticket.assignee?.name ?: "Pendiente de admin asignado",
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TicketDetailHeader(ticket = ticket)
            TicketTimeDetail(ticket)
            ClientClosure(ticket = ticket, onEvent = onEvent)
        }
    }
}

@Composable
private fun TicketDetailHeader(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                TicketStatusBadge(ticket.status)
                TicketPriorityBadge(ticket.priority)
                WaitingOnBadge(ticket.waitingOn)
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
            }
        }
        Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MetricCard(
                label = "Horas ticket",
                value = formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }),
                supportingText = "${ticket.timeEntries.size} registros",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Actualizado",
                value = formatSupportDeskDateTime(ticket.updatedAt).takeLast(10).trim(),
                supportingText = ticket.ticketNumber.ifBlank { "Ticket cliente" },
                modifier = Modifier.weight(1f),
            )
        }
        ticket.resolutionSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            ClientNotice(message = "Resolucion: $summary", isError = false)
        }
    }
}

@Composable
private fun TicketTimeDetail(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    val entriesByMonth = remember(ticket.timeEntries) {
        ticket.timeEntries.groupBy { it.workDate.take(7).ifBlank { "Sin fecha" } }
            .toList()
            .sortedByDescending { it.first }
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Horas registradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (ticket.timeEntries.isEmpty()) {
            EmptyState(title = "Sin horas", message = "El admin registrara tiempo cuando trabaje en este ticket.")
        } else {
            entriesByMonth.forEach { (month, entries) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(month, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatSupportDeskDuration(entries.sumOf { it.minutes }), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        entries.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                StatusDot()
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.note.ifBlank { "Trabajo registrado" }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${entry.authorName} · ${entry.workDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatSupportDeskDuration(entry.minutes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot() {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .height(8.dp)
            .width(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun ClientClosure(ticket: Ticket, onEvent: (TicketsUiEvent) -> Unit) {
    val (title, message) = when (ticket.status) {
        TicketStatus.OPEN -> "Recibido" to "El equipo aún no ha empezado a trabajar en este ticket. Te avisaremos en cuanto haya movimiento."
        TicketStatus.IN_PROGRESS -> "En curso" to "El equipo está trabajando activamente en tu solicitud."
        TicketStatus.PENDING_CLIENT -> "Esperando tu respuesta" to "El equipo necesita una confirmación tuya para poder continuar. Revisa la descripción o los comentarios más arriba."
        TicketStatus.RESOLVED -> "Resuelto" to "El equipo considera este ticket resuelto. Si el problema persiste, puedes reabrir la conversación desde un nuevo ticket."
        TicketStatus.CLOSED -> "Cerrado" to "Este ticket está cerrado y ya no admite cambios. Consulta el historial en Actividad."
    }
    SectionCard(title = title, emphasized = ticket.status == TicketStatus.PENDING_CLIENT) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
