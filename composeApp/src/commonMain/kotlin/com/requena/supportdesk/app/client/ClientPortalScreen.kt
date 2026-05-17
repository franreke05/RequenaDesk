package com.requena.supportdesk.app.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.TimeEntry
import com.requena.supportdesk.core.time.currentIsoDate
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
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
import com.requena.supportdesk.designsystem.components.tickets.MessageBubble
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.displayName
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.features.tickets.domain.model.CreateTicketInput
import com.requena.supportdesk.features.tickets.presentation.event.TicketsUiEvent
import com.requena.supportdesk.features.tickets.presentation.state.TicketsUiState

private const val ClientDailyTicketLimit = 15

@Composable
fun ClientPortalScreen(
    clientName: String,
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val todayTicketCount = state.tickets.count { it.createdAt.take(10) == currentIsoDate() }
    val visibleEntries = state.tickets.flatMap { it.timeEntries }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.xl, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            ClientHero(
                clientName = clientName,
                ticketCount = state.tickets.size,
                openCount = state.tickets.count { it.status != TicketStatus.CLOSED },
                monthlyMinutes = visibleEntries.filter { it.workDate.take(7) == currentIsoDate().take(7) }.sumOf { it.minutes },
                dailyTicketCount = todayTicketCount,
                onRefresh = onRefresh,
                onSignOut = onSignOut,
            )

            state.errorMessage?.let { message ->
                ClientNotice(message = message, isError = true)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val stacked = maxWidth < 1080.dp
                if (stacked) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        item {
                            ClientTicketComposer(
                                ticketsToday = todayTicketCount,
                                isLoading = state.isLoading,
                                onEvent = onEvent,
                            )
                        }
                        item { ClientCommandBoard(state = state, timeEntries = visibleEntries, onEvent = onEvent) }
                        item { ClientTicketList(state = state, onEvent = onEvent) }
                        item { ClientTicketDetail(ticket = state.selectedTicket, onEvent = onEvent) }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        Column(
                            modifier = Modifier.weight(0.34f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(spacing.lg),
                        ) {
                            ClientTicketComposer(
                                ticketsToday = todayTicketCount,
                                isLoading = state.isLoading,
                                onEvent = onEvent,
                            )
                            ClientCommandBoard(state = state, timeEntries = visibleEntries, onEvent = onEvent)
                        }
                        ClientTicketList(state = state, onEvent = onEvent, modifier = Modifier.weight(0.30f))
                        ClientTicketDetail(ticket = state.selectedTicket, onEvent = onEvent, modifier = Modifier.weight(0.36f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientHero(
    clientName: String,
    ticketCount: Int,
    openCount: Int,
    monthlyMinutes: Int,
    dailyTicketCount: Int,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = SupportDeskThemeTokens.elevations.raised),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(spacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "Portal cliente",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        SupportDeskBadge(
                            text = "Chat con admin asignado",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        SupportDeskBadge(
                            text = "$dailyTicketCount/$ClientDailyTicketLimit tickets hoy",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    HeroMetric("Tickets", ticketCount.toString())
                    HeroMetric("Abiertos", openCount.toString())
                    HeroMetric("Mes", formatSupportDeskDuration(monthlyMinutes))
                    SecondaryButton(text = "Actualizar", onClick = onRefresh)
                    SecondaryButton(text = "Salir", onClick = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ClientNotice(message: String, isError: Boolean) {
    val semantic = SupportDeskThemeTokens.semanticColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) semantic.dangerContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isError) semantic.danger else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(SupportDeskThemeTokens.spacing.md),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ClientTicketComposer(
    ticketsToday: Int,
    isLoading: Boolean,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(TicketCategory.QUESTION) }
    var platform by rememberSaveable { mutableStateOf(SupportPlatform.DESKTOP) }
    val limitReached = ticketsToday >= ClientDailyTicketLimit
    val progress = (ticketsToday.toFloat() / ClientDailyTicketLimit.toFloat()).coerceIn(0f, 1f)

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = "Nuevo ticket",
        subtitle = "Puedes abrir hasta $ClientDailyTicketLimit tickets por dia. El servidor aplica el limite final.",
    ) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SupportDeskBadge(
                text = "$ticketsToday usados",
                containerColor = if (limitReached) SupportDeskThemeTokens.semanticColors.dangerContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (limitReached) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = if (limitReached) "Limite diario alcanzado" else "Quedan ${ClientDailyTicketLimit - ticketsToday}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilterBar(
            label = "Tipo",
            options = TicketCategory.entries.map { FilterOption(it, it.displayName()) },
            selected = category,
            onSelected = { category = it ?: TicketCategory.QUESTION },
            allLabel = "Consulta",
            wrap = true,
        )
        FilterBar(
            label = "Plataforma",
            options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) },
            selected = platform,
            onSelected = { platform = it ?: SupportPlatform.DESKTOP },
            allLabel = "Escritorio",
            wrap = true,
        )
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Asunto") },
            singleLine = true,
            enabled = !limitReached,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Detalle") },
            minLines = 4,
            enabled = !limitReached,
        )
        PrimaryButton(
            text = if (isLoading) "Enviando..." else "Enviar ticket",
            onClick = {
                onEvent(
                    TicketsUiEvent.CreateTicket(
                        CreateTicketInput(
                            subject = subject.trim(),
                            description = description.trim(),
                            category = category,
                            platform = platform,
                        ),
                    ),
                )
                subject = ""
                description = ""
            },
            enabled = !limitReached && !isLoading && subject.isNotBlank() && description.isNotBlank(),
            fullWidth = true,
        )
    }
}

@Composable
private fun ClientCommandBoard(
    state: TicketsUiState,
    timeEntries: List<TimeEntry>,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier.fillMaxWidth(), title = "Tablero", subtitle = "Prioridad, estado y horas visibles.") {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar ticket") },
            singleLine = true,
        )
        FilterBar(
            label = "Estado",
            options = TicketStatus.entries.map { FilterOption(it, it.displayName()) },
            selected = state.statusFilter,
            onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
            wrap = true,
            allLabel = "Todos",
        )
        FilterBar(
            label = "Prioridad",
            options = TicketPriority.entries.map { FilterOption(it, it.displayName()) },
            selected = state.priorityFilter,
            onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
            wrap = true,
            allLabel = "Todas",
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TicketStatus.entries.forEach { status ->
                BoardCounter(
                    label = status.displayName(),
                    value = state.tickets.count { it.status == status }.toString(),
                    modifier = Modifier.weight(1f),
                    selected = state.statusFilter == status,
                    onClick = { onEvent(TicketsUiEvent.StatusFilterChanged(if (state.statusFilter == status) null else status)) },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TicketPriority.entries.forEach { priority ->
                BoardCounter(
                    label = priority.displayName(),
                    value = state.tickets.count { it.priority == priority }.toString(),
                    modifier = Modifier.weight(1f),
                    selected = state.priorityFilter == priority,
                    onClick = { onEvent(TicketsUiEvent.PriorityFilterChanged(if (state.priorityFilter == priority) null else priority)) },
                )
            }
        }
        ClientTimeSummary(timeEntries = timeEntries)
    }
}

@Composable
private fun BoardCounter(label: String, value: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 74.dp)
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClientTimeSummary(timeEntries: List<TimeEntry>) {
    val spacing = SupportDeskThemeTokens.spacing
    val monthGroups = timeEntries.groupBy { it.workDate.take(7).ifBlank { "Sin fecha" } }
        .toList()
        .sortedByDescending { it.first }
        .take(4)

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text("Horas por mes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (monthGroups.isEmpty()) {
            Text("Aun no hay horas visibles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            monthGroups.forEach { (month, entries) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(month, style = MaterialTheme.typography.bodyMedium)
                    Text(formatSupportDeskDuration(entries.sumOf { it.minutes }), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ClientTicketList(state: TicketsUiState, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(modifier = modifier.fillMaxHeight(), title = "Mis tickets", subtitle = "${state.tickets.size} tickets visibles") {
        Crossfade(targetState = state.isLoading, label = "clientTicketsLoading") { loading ->
            if (loading) {
                LoadingState(itemCount = 5)
            } else if (state.tickets.isEmpty()) {
                EmptyState(
                    title = "Sin tickets",
                    message = state.errorMessage ?: "Cuando abras un ticket aparecera aqui con su estado, prioridad y horas.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().heightIn(min = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    items(state.tickets, key = { it.id }) { ticket ->
                        ClientTicketRow(
                            ticket = ticket,
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
private fun ClientTicketRow(ticket: Ticket, selected: Boolean, onClick: () -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(ticket.ticketNumber.ifBlank { formatSupportDeskDateTime(ticket.updatedAt) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                TicketStatusBadge(ticket.status)
                TicketPriorityBadge(ticket.priority)
            }
            Text(
                text = "Admin: ${ticket.assignee?.name ?: "pendiente de asignacion"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClientTicketDetail(ticket: Ticket?, onEvent: (TicketsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    if (ticket == null) {
        EmptyState(
            title = "Selecciona un ticket",
            message = "El detalle, chat con el admin asignado, cierre y horas apareceran aqui.",
            modifier = modifier,
        )
        return
    }

    SectionCard(modifier = modifier.fillMaxHeight(), title = ticket.subject, subtitle = ticket.assignee?.name ?: "Pendiente de admin asignado") {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            TicketDetailHeader(ticket)
            TicketTimeDetail(ticket)
            ClientChat(ticket = ticket, onEvent = onEvent)
            ClientClosure(ticket = ticket, onEvent = onEvent)
        }
    }
}

@Composable
private fun TicketDetailHeader(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            TicketStatusBadge(ticket.status)
            TicketPriorityBadge(ticket.priority)
            WaitingOnBadge(ticket.waitingOn)
        }
        Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MetricCard(
                label = "Horas ticket",
                value = formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }),
                supportingText = "${ticket.timeEntries.size} registros visibles",
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
    val entriesByMonth = ticket.timeEntries.groupBy { it.workDate.take(7).ifBlank { "Sin fecha" } }
        .toList()
        .sortedByDescending { it.first }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Horas por ticket, tarea y mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (ticket.timeEntries.isEmpty()) {
            EmptyState(title = "Sin horas visibles", message = "Cuando el admin registre tiempo en este ticket aparecera aqui.")
        } else {
            entriesByMonth.forEach { (month, entries) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                    shape = MaterialTheme.shapes.medium,
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
            .padding(top = 7.dp)
            .height(8.dp)
            .width(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun ClientChat(ticket: Ticket, onEvent: (TicketsUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    var reply by rememberSaveable(ticket.id) { mutableStateOf("") }
    val assignedAdminId = ticket.assignee?.id
    val visibleMessages = ticket.messages.filter { message ->
        message.authorId == ticket.requester.id || (assignedAdminId != null && message.authorId == assignedAdminId)
    }
    val canReply = assignedAdminId != null && ticket.status != TicketStatus.CLOSED && ticket.archivedAt == null

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text("Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SupportDeskBadge(
                text = ticket.assignee?.name ?: "Sin admin asignado",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (visibleMessages.isEmpty()) {
            EmptyState(
                title = "Sin mensajes",
                message = if (assignedAdminId == null) {
                    "El chat se abrira cuando haya un admin asignado."
                } else {
                    "Escribe al admin asignado para continuar el hilo."
                },
            )
        } else {
            visibleMessages.forEach { message ->
                MessageBubble(
                    authorName = message.authorName,
                    body = message.body,
                    timestamp = formatSupportDeskDateTime(message.createdAt),
                    isOwnMessage = message.authorId == ticket.requester.id,
                )
            }
        }
        OutlinedTextField(
            value = reply,
            onValueChange = { reply = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (canReply) "Responder a ${ticket.assignee?.name}" else "Chat bloqueado") },
            minLines = 3,
            enabled = canReply,
        )
        PrimaryButton(
            text = "Enviar respuesta",
            onClick = {
                onEvent(TicketsUiEvent.ReplyToSelected(reply.trim()))
                reply = ""
            },
            enabled = canReply && reply.isNotBlank(),
            fullWidth = true,
        )
    }
}

@Composable
private fun ClientClosure(ticket: Ticket, onEvent: (TicketsUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    val canAcceptClose = ticket.status == TicketStatus.RESOLVED && ticket.clientAcceptedCloseAt == null
    val canRate = ticket.status == TicketStatus.RESOLVED || ticket.status == TicketStatus.CLOSED

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text("Cierre y satisfaccion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            ClosurePill("Cliente", ticket.clientAcceptedCloseAt != null)
            ClosurePill("Admin", ticket.adminAcceptedCloseAt != null)
            ticket.archivedAt?.let {
                SupportDeskBadge(
                    text = "Archivado",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(
            visible = canAcceptClose,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) {
            SecondaryButton(
                text = "Aceptar cierre",
                onClick = { onEvent(TicketsUiEvent.AcceptSelectedClose()) },
                fullWidth = true,
            )
        }
        if (ticket.satisfactionRating != null) {
            ClientNotice(message = "Valoracion registrada: ${ticket.satisfactionRating}/5", isError = false)
        } else {
            Text(
                text = if (canRate) "Valora la resolucion del 1 al 5." else "La valoracion se activa cuando el ticket este resuelto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items((1..5).toList()) { rating ->
                    SecondaryButton(
                        text = rating.toString(),
                        onClick = { onEvent(TicketsUiEvent.RateSelected(rating)) },
                        enabled = canRate,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosurePill(label: String, complete: Boolean) {
    val alpha = if (complete) 1f else 0.56f
    SupportDeskBadge(
        text = if (complete) "$label aceptado" else "$label pendiente",
        containerColor = if (complete) SupportDeskThemeTokens.semanticColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (complete) SupportDeskThemeTokens.semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(alpha),
    )
}
