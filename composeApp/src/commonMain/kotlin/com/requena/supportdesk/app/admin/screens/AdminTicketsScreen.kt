package com.requena.supportdesk.app.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.admin.AdminLayoutMode
import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.SupportPlatform
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketCategory
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WaitingOn
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.badges.WaitingOnBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.ConfirmDialog
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.inputs.FilterBar
import com.requena.supportdesk.designsystem.components.inputs.FilterOption
import com.requena.supportdesk.designsystem.components.inputs.SearchField
import com.requena.supportdesk.designsystem.components.layout.InfoRow
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.motion.SupportDeskEntrance
import com.requena.supportdesk.designsystem.components.navigation.AdminSectionDivider
import com.requena.supportdesk.designsystem.components.tickets.AttachmentRow
import com.requena.supportdesk.designsystem.components.tickets.CommentBubble
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
    val isSplitPane = layoutMode == AdminLayoutMode.EXPANDED

    if (isSplitPane) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SupportDeskEntrance(index = 0, horizontal = true, modifier = Modifier.weight(0.43f).fillMaxHeight()) {
                TicketListPane(
                    state = state,
                    onEvent = onEvent,
                    onOpenDetail = onOpenDetail,
                    onOpenCreateTicket = onOpenCreateTicket,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            SupportDeskEntrance(index = 1, horizontal = true, modifier = Modifier.weight(0.57f).fillMaxHeight()) {
                TicketDetailPane(
                    ticket = state.selectedTicket,
                    currentAdminId = currentAdminId,
                    currentAdminName = currentAdminName,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    } else {
        SupportDeskEntrance(index = 0, horizontal = true, modifier = modifier.fillMaxSize()) {
            TicketListPane(
                state = state,
                onEvent = onEvent,
                onOpenDetail = onOpenDetail,
                onOpenCreateTicket = onOpenCreateTicket,
                modifier = Modifier.fillMaxSize(),
            )
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
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SupportDeskEntrance(index = 0) {
            PageHeader(
                title = ticket?.subject ?: "Detalle del ticket",
                subtitle = ticket?.affectedApp ?: "Notas, tiempo y estado registrado.",
                eyebrow = ticket?.ticketNumber ?: "Soporte",
                actions = { SecondaryButton(text = "Volver", onClick = onBack) },
            )
        }
        SupportDeskEntrance(index = 1, horizontal = true, modifier = Modifier.fillMaxSize()) {
            TicketDetailPane(
                ticket = ticket,
                currentAdminId = currentAdminId,
                currentAdminName = currentAdminName,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TicketListPane(
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    onOpenDetail: (Ticket) -> Unit,
    onOpenCreateTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val derived = remember(state.allTickets) {
        TicketListDerived(
            stats = state.allTickets.toQueueStats(),
            availableStatuses = state.allTickets.map { it.status }.distinct().sortedBy { it.ordinal },
            availablePriorities = state.allTickets.map { it.priority }.distinct().sortedBy { it.ordinal },
            availableCategories = state.allTickets.map { it.category }.distinct().sortedBy { it.ordinal },
            availablePlatforms = state.allTickets.map { it.platform }.distinct().sortedBy { it.ordinal },
            availableWaitingOn = state.allTickets.map { it.waitingOn }.distinct().sortedBy { it.ordinal },
        )
    }
    var showFilterPopup by remember { mutableStateOf(false) }
    val hasActiveFilters = state.searchQuery.isNotBlank() ||
        state.statusFilter != null ||
        state.priorityFilter != null ||
        state.categoryFilter != null ||
        state.platformFilter != null ||
        state.waitingOnFilter != null

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "metrics") {
            QueueMetricsGrid(stats = derived.stats)
        }
        item(key = "filter-card") {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Cola de tickets",
                subtitle = "${state.tickets.size} visible de ${state.allTickets.size} tickets",
                neonAccentColor = MaterialTheme.colorScheme.primary,
                actions = {
                    SecondaryButton(
                        text = "Ver todos",
                        onClick = { clearTicketFilters(state, onEvent) },
                    )
                    PrimaryButton(text = "Nuevo ticket", onClick = onOpenCreateTicket)
                },
            ) {
                StatusDistributionBar(stats = derived.stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(TicketsUiEvent.SearchChanged(it)) },
                        placeholder = "Buscar ticket, cliente, app o descripciÃ³n",
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        FilterIconButton(
                            hasActiveFilters = hasActiveFilters,
                            onClick = { showFilterPopup = !showFilterPopup },
                        )
                        DropdownMenu(
                            expanded = showFilterPopup,
                            onDismissRequest = { showFilterPopup = false },
                        ) {
                            Box(modifier = Modifier.width(320.dp).padding(spacing.md)) {
                                QueueFilterPanel(
                                    availableStatuses = derived.availableStatuses,
                                    availablePriorities = derived.availablePriorities,
                                    availableCategories = derived.availableCategories,
                                    availablePlatforms = derived.availablePlatforms,
                                    availableWaitingOn = derived.availableWaitingOn,
                                    state = state,
                                    onEvent = onEvent,
                                    wrapFilters = true,
                                )
                            }
                        }
                    }
                }
                ActiveFilterStrip(state = state, hasActiveFilters = hasActiveFilters, onEvent = onEvent)
                state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    QueueErrorBanner(message = message, onRetry = { onEvent(TicketsUiEvent.Load) })
                }
            }
        }
        if (state.isLoading && state.tickets.isEmpty()) {
            item(key = "loading") {
                LoadingState(itemCount = 4, modifier = Modifier.fillMaxWidth())
            }
        } else if (state.tickets.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = if (state.allTickets.isEmpty()) "Sin tickets cargados" else "Sin coincidencias",
                    message = state.errorMessage ?: "No hay tickets con los filtros activos.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(state.tickets, key = { it.id }) { ticket ->
                TriageTicketRow(
                    ticket = ticket,
                    selected = state.selectedTicket?.id == ticket.id,
                    onClick = {
                        onEvent(TicketsUiEvent.SelectTicket(ticket.id))
                        onOpenDetail(ticket)
                    },
                )
            }
        }
    }
}

@Composable
private fun QueueMetricsGrid(stats: TicketQueueStats) {
    val semantic = SupportDeskThemeTokens.semanticColors
    val metrics = listOf(
        QueueMetricSpec("Total", stats.total, "tickets en cola", MaterialTheme.colorScheme.primary),
        QueueMetricSpec("Equipo", stats.waitingAdmin, "esperando respuesta", MaterialTheme.colorScheme.secondary),
        QueueMetricSpec("Urgentes", stats.urgent, "prioridad urgente", semantic.danger),
        QueueMetricSpec("Cerrados", stats.closed + stats.resolved, "resueltos o cerrados", semantic.success),
    )
    val spacing = SupportDeskThemeTokens.spacing

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        when {
            maxWidth < 620.dp -> LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                items(metrics, key = { it.label }) { metric ->
                    AnimatedMetricCard(metric = metric, modifier = Modifier.width(188.dp))
                }
            }
            maxWidth < 980.dp -> Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AnimatedMetricCard(metrics[0], Modifier.weight(1f))
                    AnimatedMetricCard(metrics[1], Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AnimatedMetricCard(metrics[2], Modifier.weight(1f))
                    AnimatedMetricCard(metrics[3], Modifier.weight(1f))
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                metrics.forEach { metric ->
                    AnimatedMetricCard(metric = metric, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AnimatedMetricCard(metric: QueueMetricSpec, modifier: Modifier = Modifier) {
    val animatedValue by animateFloatAsState(
        targetValue = metric.value.toFloat(),
        animationSpec = tween(durationMillis = 480),
        label = "ticketMetricValue",
    )
    MetricCard(
        label = metric.label,
        value = animatedValue.toInt().coerceAtLeast(0).toString(),
        supportingText = metric.supportingText,
        neonAccentColor = metric.color,
        modifier = modifier,
    )
}

@Composable
private fun StatusDistributionBar(stats: TicketQueueStats) {
    val total = stats.total.coerceAtLeast(1)
    val statuses = TicketStatus.entries
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        statuses.forEach { status ->
            val count = stats.countFor(status)
            val targetWeight = if (stats.total == 0) 1f else (count.toFloat() / total.toFloat()).coerceAtLeast(0.015f)
            val animatedWeight by animateFloatAsState(
                targetValue = targetWeight,
                animationSpec = tween(durationMillis = 520),
                label = "ticketStatusDistribution",
            )
            Box(
                modifier = Modifier
                    .weight(animatedWeight)
                    .fillMaxHeight()
                    .background(status.accentColor().copy(alpha = if (count > 0) 0.88f else 0.16f), shape),
            )
        }
    }
}

@Composable
private fun QueueSignalChip(text: String, color: Color, modifier: Modifier = Modifier) {
    SupportDeskBadge(
        text = text,
        containerColor = color.copy(alpha = 0.14f),
        contentColor = color,
        modifier = modifier,
    )
}

@Composable
private fun ActiveFilterStrip(state: TicketsUiState, hasActiveFilters: Boolean, onEvent: (TicketsUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    AnimatedVisibility(visible = hasActiveFilters) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            state.searchQuery.takeIf { it.isNotBlank() }?.let {
                QueueSignalChip("BÃºsqueda: $it", MaterialTheme.colorScheme.primary)
            }
            state.statusFilter?.let { QueueSignalChip("Estado: ${it.displayName()}", it.accentColor()) }
            state.priorityFilter?.let { QueueSignalChip("Prioridad: ${it.displayName()}", it.accentColor()) }
            state.categoryFilter?.let { QueueSignalChip("CategorÃ­a: ${it.displayName()}", MaterialTheme.colorScheme.secondary) }
            state.platformFilter?.let { QueueSignalChip("Plataforma: ${it.displayName()}", SupportDeskThemeTokens.semanticColors.info) }
            state.waitingOnFilter?.let { QueueSignalChip("Espera: ${it.displayName()}", it.accentColor()) }
        }
    }
}

@Composable
private fun QueueFilterPanel(
    availableStatuses: List<TicketStatus>,
    availablePriorities: List<TicketPriority>,
    availableCategories: List<TicketCategory>,
    availablePlatforms: List<SupportPlatform>,
    availableWaitingOn: List<WaitingOn>,
    state: TicketsUiState,
    onEvent: (TicketsUiEvent) -> Unit,
    wrapFilters: Boolean = false,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (availableStatuses.isNotEmpty()) {
            FilterBar(
                label = "Estado",
                options = availableStatuses.map { FilterOption(it, it.displayName()) },
                selected = state.statusFilter,
                onSelected = { onEvent(TicketsUiEvent.StatusFilterChanged(it)) },
                allLabel = "Todo",
                wrap = wrapFilters,
            )
        }
        if (availablePriorities.isNotEmpty()) {
            FilterBar(
                label = "Prioridad",
                options = availablePriorities.map { FilterOption(it, it.displayName()) },
                selected = state.priorityFilter,
                onSelected = { onEvent(TicketsUiEvent.PriorityFilterChanged(it)) },
                allLabel = "Todo",
                wrap = wrapFilters,
            )
        }
        if (availableCategories.isNotEmpty()) {
            FilterBar(
                label = "CategorÃ­a",
                options = availableCategories.map { FilterOption(it, it.displayName()) },
                selected = state.categoryFilter,
                onSelected = { onEvent(TicketsUiEvent.CategoryFilterChanged(it)) },
                allLabel = "Todo",
                wrap = wrapFilters,
            )
        }
        if (availablePlatforms.isNotEmpty()) {
            FilterBar(
                label = "Plataforma",
                options = availablePlatforms.map { FilterOption(it, it.displayName()) },
                selected = state.platformFilter,
                onSelected = { onEvent(TicketsUiEvent.PlatformFilterChanged(it)) },
                allLabel = "Todo",
                wrap = wrapFilters,
            )
        }
        if (availableWaitingOn.isNotEmpty()) {
            FilterBar(
                label = "Espera",
                options = availableWaitingOn.map { FilterOption(it, it.displayName()) },
                selected = state.waitingOnFilter,
                onSelected = { onEvent(TicketsUiEvent.WaitingOnFilterChanged(it)) },
                allLabel = "Todo",
                wrap = wrapFilters,
            )
        }
    }
}

@Composable
private fun FilterIconButton(hasActiveFilters: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val iconColor = if (hasActiveFilters) accent else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = if (hasActiveFilters) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = if (hasActiveFilters) BorderStroke(1.dp, accent.copy(alpha = 0.28f)) else null,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width
                val h = size.height
                val sw = 2.2.dp.toPx()
                drawLine(iconColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = sw)
                drawLine(iconColor, Offset(w * 0.18f, h * 0.44f), Offset(w * 0.82f, h * 0.44f), strokeWidth = sw)
                drawLine(iconColor, Offset(w * 0.36f, h * 0.88f), Offset(w * 0.64f, h * 0.88f), strokeWidth = sw)
            }
        }
    }
}

@Composable
private fun QueueErrorBanner(message: String, onRetry: () -> Unit) {
    val semantic = SupportDeskThemeTokens.semanticColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = semantic.dangerContainer,
        border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Datos no disponibles",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.danger,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryButton(text = "Reintentar", onClick = onRetry)
        }
    }
}

@Composable
private fun TriageTicketRow(
    ticket: Ticket,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val accent = ticket.priority.accentColor()
    val targetBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 260),
        label = "triageTicketBackground",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) SupportDeskThemeTokens.elevations.raised else SupportDeskThemeTokens.elevations.subtle,
        animationSpec = tween(durationMillis = 260),
        label = "triageTicketElevation",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = elevation,
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.78f else 0.22f)),
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.18f)))),
            )
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = listOf(ticket.ticketNumber, ticket.affectedApp).filter { it.isNotBlank() }.joinToString("  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ticket.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        TicketPriorityBadge(ticket.priority)
                    }
                }
                Text(
                    text = ticket.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    TicketStatusBadge(ticket.status)
                    WaitingOnBadge(ticket.waitingOn)
                    TicketCategoryBadge(ticket.category)
                    SupportPlatformBadge(ticket.platform)
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    if (ticket.internalComments.isNotEmpty()) {
                        QueueSignalChip("${ticket.internalComments.size} notas", SupportDeskThemeTokens.semanticColors.warning)
                    }
                    QueueSignalChip(formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }), MaterialTheme.colorScheme.secondary)
                    if (ticket.attachments.isNotEmpty()) {
                        QueueSignalChip("${ticket.attachments.size} archivos", SupportDeskThemeTokens.semanticColors.info)
                    }
                }
                Text(
                    text = buildString {
                        append(ticket.requester.name)
                        append(" Â· ")
                        append(formatSupportDeskDateTime(ticket.updatedAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TicketDetailPane(
    ticket: Ticket?,
    currentAdminId: String,
    currentAdminName: String,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ticket == null) {
        SectionCard(
            modifier = modifier.fillMaxSize(),
            title = "NingÃºn ticket seleccionado",
            subtitle = "Selecciona un ticket de la cola para ver la conversaciÃ³n, notas y tiempo.",
            neonAccentColor = MaterialTheme.colorScheme.primary,
        ) {
            EmptyState(
                title = "Sin selecciÃ³n",
                message = "Elige un ticket de la cola para empezar.",
            )
        }
        return
    }

    val spacing = SupportDeskThemeTokens.spacing
    var noteDraft by rememberSaveable(ticket.id) { mutableStateOf("") }
    var timeMinutes by rememberSaveable(ticket.id) { mutableStateOf("") }
    var timeNote by rememberSaveable(ticket.id) { mutableStateOf("") }
    var billable by rememberSaveable(ticket.id) { mutableStateOf(true) }
    var showDeleteConfirm by rememberSaveable(ticket.id) { mutableStateOf(false) }

    ConfirmDialog(
        visible = showDeleteConfirm,
        title = "Eliminar ticket",
        message = "Â¿Eliminar el ticket ${ticket.ticketNumber}? Esta acciÃ³n no se puede deshacer.",
        confirmText = "Eliminar",
        dismissText = "Cancelar",
        onConfirm = {
            showDeleteConfirm = false
            onEvent(TicketsUiEvent.DeleteTicket(ticket.id))
        },
        onDismiss = { showDeleteConfirm = false },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        TicketIdentityCard(
            ticket = ticket,
            onDeleteClick = { showDeleteConfirm = true },
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 980.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    TicketContextColumn(ticket = ticket)
                    TicketOpsColumn(
                        ticket = ticket,
                        currentAdminId = currentAdminId,
                        currentAdminName = currentAdminName,
                        noteDraft = noteDraft,
                        onNoteDraftChange = { noteDraft = it },
                        timeMinutes = timeMinutes,
                        onTimeMinutesChange = { timeMinutes = it },
                        timeNote = timeNote,
                        onTimeNoteChange = { timeNote = it },
                        billable = billable,
                        onBillableChange = { billable = it },
                        onEvent = onEvent,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    TicketContextColumn(
                        ticket = ticket,
                        modifier = Modifier.weight(0.58f),
                    )
                    TicketOpsColumn(
                        ticket = ticket,
                        currentAdminId = currentAdminId,
                        currentAdminName = currentAdminName,
                        noteDraft = noteDraft,
                        onNoteDraftChange = { noteDraft = it },
                        timeMinutes = timeMinutes,
                        onTimeMinutesChange = { timeMinutes = it },
                        timeNote = timeNote,
                        onTimeNoteChange = { timeNote = it },
                        billable = billable,
                        onBillableChange = { billable = it },
                        onEvent = onEvent,
                        modifier = Modifier.weight(0.42f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketIdentityCard(
    ticket: Ticket,
    onDeleteClick: () -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = ticket.ticketNumber.ifBlank { "Ticket" },
        subtitle = ticket.subject,
        neonAccentColor = ticket.priority.accentColor(),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            TicketStatusBadge(ticket.status)
            TicketPriorityBadge(ticket.priority)
            WaitingOnBadge(ticket.waitingOn)
            TicketCategoryBadge(ticket.category)
            SupportPlatformBadge(ticket.platform)
        }
        Text(
            text = ticket.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 760.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TicketSummaryStat("Solicitante", ticket.requester.name, ticket.requester.email, MaterialTheme.colorScheme.primary)
                    TicketSummaryStat("Actualizado", formatSupportDeskDateTime(ticket.updatedAt), ticket.affectedApp, SupportDeskThemeTokens.semanticColors.info)
                    TicketSummaryStat("Registrado", formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }), "${ticket.timeEntries.size} entradas", MaterialTheme.colorScheme.secondary)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TicketSummaryStat("Solicitante", ticket.requester.name, ticket.requester.email, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    TicketSummaryStat("Actualizado", formatSupportDeskDateTime(ticket.updatedAt), ticket.affectedApp, SupportDeskThemeTokens.semanticColors.info, Modifier.weight(1f))
                    TicketSummaryStat("Registrado", formatSupportDeskDuration(ticket.timeEntries.sumOf { it.minutes }), "${ticket.timeEntries.size} entradas", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            SecondaryButton(text = "Eliminar ticket", onClick = onDeleteClick)
        }
    }
}

@Composable
private fun TicketSummaryStat(
    label: String,
    value: String,
    supportingText: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(9.dp).background(accent, CircleShape))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TicketContextColumn(ticket: Ticket, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        SectionCard(
            title = "Contexto",
            subtitle = "Solicitante, app y datos tÃ©cnicos.",
            neonAccentColor = SupportDeskThemeTokens.semanticColors.info,
        ) {
            InfoRow(label = "Solicitante", value = ticket.requester.name, supportingText = ticket.requester.email)
            InfoRow(label = "App afectada", value = ticket.affectedApp.ifBlank { "-" })
            InfoRow(label = "VersiÃ³n", value = ticket.appVersion?.takeIf { it.isNotBlank() } ?: "-")
            InfoRow(label = "Referencia", value = ticket.clientReference?.takeIf { it.isNotBlank() } ?: "-")
            InfoRow(label = "Pasos", value = ticket.stepsToReproduce?.takeIf { it.isNotBlank() } ?: "Sin pasos de reproducciÃ³n.")
            InfoRow(label = "SatisfacciÃ³n", value = ticket.satisfactionRating?.let { "$it/5" } ?: "Sin valorar")
            InfoRow(label = "Creado", value = formatSupportDeskDateTime(ticket.createdAt))
        }
        if (ticket.attachments.isNotEmpty()) {
            SectionCard(
                title = "Adjuntos",
                subtitle = "${ticket.attachments.size} archivos.",
                neonAccentColor = MaterialTheme.colorScheme.secondary,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ticket.attachments.forEach { AttachmentRow(it) }
                }
            }
        }
        ActivityLogSection(ticket = ticket)
    }
}

@Composable
private fun TicketOpsColumn(
    ticket: Ticket,
    currentAdminId: String,
    currentAdminName: String,
    noteDraft: String,
    onNoteDraftChange: (String) -> Unit,
    timeMinutes: String,
    onTimeMinutesChange: (String) -> Unit,
    timeNote: String,
    onTimeNoteChange: (String) -> Unit,
    billable: Boolean,
    onBillableChange: (Boolean) -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        WorkflowSection(ticket = ticket, onEvent = onEvent)
        InternalNotesSection(
            ticket = ticket,
            currentAdminId = currentAdminId,
            currentAdminName = currentAdminName,
            noteDraft = noteDraft,
            onNoteDraftChange = onNoteDraftChange,
            onEvent = onEvent,
        )
        LogTimeSection(
            ticket = ticket,
            currentAdminId = currentAdminId,
            currentAdminName = currentAdminName,
            timeMinutes = timeMinutes,
            onTimeMinutesChange = onTimeMinutesChange,
            timeNote = timeNote,
            onTimeNoteChange = onTimeNoteChange,
            billable = billable,
            onBillableChange = onBillableChange,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun WorkflowSection(ticket: Ticket, onEvent: (TicketsUiEvent) -> Unit) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Flujo de trabajo",
        subtitle = "Cambia el estado sin salir del detalle.",
        neonAccentColor = ticket.status.accentColor(),
    ) {
        WorkflowPulse(ticket = ticket)
        Text("Estado", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FilterBar(
            label = "Estado",
            options = TicketStatus.entries.map { FilterOption(it, it.displayName()) },
            selected = ticket.status,
            onSelected = { selected -> selected?.let { onEvent(TicketsUiEvent.ChangeSelectedStatus(it)) } },
            allLabel = "Actual",
            wrap = true,
        )
        Text("Prioridad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FilterBar(
            label = "Prioridad",
            options = TicketPriority.entries.map { FilterOption(it, it.displayName()) },
            selected = ticket.priority,
            onSelected = { selected -> selected?.let { onEvent(TicketsUiEvent.ChangeSelectedPriority(it)) } },
            allLabel = "Actual",
            wrap = true,
        )
        ticket.resolutionSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SupportDeskThemeTokens.semanticColors.successContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ResoluciÃ³n", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = SupportDeskThemeTokens.semanticColors.success)
                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun WorkflowPulse(ticket: Ticket) {
    val statusColor = ticket.status.accentColor()
    val priorityColor = ticket.priority.accentColor()
    val background by animateColorAsState(
        targetValue = statusColor.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 420),
        label = "workflowPulseBackground",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.18f)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(statusColor, priorityColor))),
            )
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.xs),
            ) {
                QueueSignalChip(ticket.status.displayName(), statusColor)
                QueueSignalChip(ticket.priority.displayName(), priorityColor)
                QueueSignalChip(ticket.waitingOn.displayName(), ticket.waitingOn.accentColor())
            }
        }
    }
}

@Composable
private fun InternalNotesSection(
    ticket: Ticket,
    currentAdminId: String,
    currentAdminName: String,
    noteDraft: String,
    onNoteDraftChange: (String) -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Notas internas",
        subtitle = "Solo visible para el equipo.",
        neonAccentColor = SupportDeskThemeTokens.semanticColors.warning,
    ) {
        OutlinedTextField(
            value = noteDraft,
            onValueChange = onNoteDraftChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("AÃ±adir nota interna") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                text = "Guardar nota",
                onClick = {
                    onEvent(TicketsUiEvent.AddInternalNote(noteDraft, currentAdminId, currentAdminName))
                    onNoteDraftChange("")
                },
                enabled = noteDraft.isNotBlank(),
            )
            Text(
                text = "Guardado como $currentAdminName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (ticket.internalComments.isEmpty()) {
            EmptyState(
                title = "Sin notas internas",
                message = "AÃ±ade notas de triaje y decisiones aquÃ­.",
            )
        } else {
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

@Composable
private fun LogTimeSection(
    ticket: Ticket,
    currentAdminId: String,
    currentAdminName: String,
    timeMinutes: String,
    onTimeMinutesChange: (String) -> Unit,
    timeNote: String,
    onTimeNoteChange: (String) -> Unit,
    billable: Boolean,
    onBillableChange: (Boolean) -> Unit,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Registrar tiempo",
        subtitle = "Registro manual asociado a este ticket.",
        neonAccentColor = MaterialTheme.colorScheme.secondary,
    ) {
        OutlinedTextField(
            value = timeMinutes,
            onValueChange = onTimeMinutesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Minutos") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            value = timeNote,
            onValueChange = onTimeNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nota de trabajo") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = billable, onCheckedChange = onBillableChange)
            Text(
                text = if (billable) "Tiempo facturable" else "Tiempo no facturable",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PrimaryButton(
            text = "AÃ±adir entrada",
            onClick = {
                onEvent(
                    TicketsUiEvent.AddTimeEntry(
                        timeMinutes.toIntOrNull() ?: 0,
                        timeNote,
                        billable,
                        currentAdminId,
                        currentAdminName,
                    ),
                )
                onTimeMinutesChange("")
                onTimeNoteChange("")
                onBillableChange(true)
            },
            enabled = timeMinutes.toIntOrNull()?.let { it > 0 } == true && timeNote.isNotBlank(),
        )
        if (ticket.timeEntries.isEmpty()) {
            EmptyState(
                title = "Sin tiempo registrado",
                message = "La primera entrada aparecerÃ¡ aquÃ­.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ticket.timeEntries.forEach { entry ->
                    TimeEntryRow(
                        authorName = entry.authorName,
                        minutes = entry.minutes,
                        workDate = entry.workDate,
                        note = entry.note,
                        billable = entry.billable,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeEntryRow(
    authorName: String,
    minutes: Int,
    workDate: String,
    note: String,
    billable: Boolean,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$authorName - ${formatSupportDeskDuration(minutes)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = workDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QueueSignalChip(
                text = if (billable) "Facturable" else "Interno",
                color = if (billable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
        }
        Text(note, style = MaterialTheme.typography.bodyMedium)
        AdminSectionDivider()
    }
}

@Composable
private fun ActivityLogSection(ticket: Ticket) {
    val spacing = SupportDeskThemeTokens.spacing
    SectionCard(
        title = "Historial",
        subtitle = "${ticket.events.size} eventos registrados.",
        neonAccentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        if (ticket.events.isEmpty()) {
            EmptyState(
                title = "Sin actividad",
                message = "La actividad del sistema aparecerÃ¡ aquÃ­ cuando estÃ© disponible.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ticket.events.forEach { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${event.actorName} - ${formatSupportDeskDateTime(event.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AdminSectionDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCreateTicketScreen(
    clients: List<Client>,
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
    var selectedPriority by rememberSaveable { mutableStateOf(TicketPriority.MEDIUM) }
    val client = clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        SupportDeskEntrance(index = 0) {
            PageHeader(
                title = "Crear ticket",
                subtitle = "Abre un ticket con el contexto necesario para dar seguimiento.",
                eyebrow = "Nuevo ticket",
                actions = { SecondaryButton(text = "Volver", onClick = onBack) },
            )
        }
        SupportDeskEntrance(index = 1) {
            SectionCard(
                title = "Cliente",
                subtitle = "Selecciona el cliente para asignar el contexto correcto.",
                neonAccentColor = MaterialTheme.colorScheme.primary,
            ) {
                if (clients.isEmpty()) {
                    EmptyState(
                        title = "Sin clientes disponibles",
                        message = "Crea o carga un cliente antes de abrir un ticket.",
                    )
                } else {
                    FilterBar(
                        label = "Cliente",
                        options = clients.map { FilterOption(it.id, it.companyName) },
                        selected = selectedClientId.takeIf { it.isNotBlank() },
                        onSelected = { selectedClientId = it.orEmpty() },
                        allLabel = "Ninguno",
                        wrap = true,
                    )
                    InfoRow(label = "App afectada", value = client?.productName ?: "-")
                }
            }
        }
        SupportDeskEntrance(index = 2) {
            SectionCard(
                title = "DescripciÃ³n del problema",
                subtitle = "Asunto, prioridad y descripciÃ³n para la cola.",
                neonAccentColor = selectedPriority.accentColor(),
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Asunto") },
                    singleLine = true,
                )
                FilterBar(
                    label = "Prioridad",
                    options = TicketPriority.entries.map { FilterOption(it, it.displayName()) },
                    selected = selectedPriority,
                    onSelected = { selectedPriority = it ?: TicketPriority.MEDIUM },
                    allLabel = "Predeterminado",
                    wrap = true,
                )
                FilterBar(
                    label = "CategorÃ­a",
                    options = TicketCategory.entries.map { FilterOption(it, it.displayName()) },
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it ?: TicketCategory.BUG },
                    allLabel = "Predeterminado",
                    wrap = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DescripciÃ³n") },
                    minLines = 5,
                )
            }
        }
        SupportDeskEntrance(index = 3) {
            SectionCard(
                title = "Contexto tÃ©cnico",
                subtitle = "Campos que ayudan a depurar y reconstruir el problema.",
                neonAccentColor = SupportDeskThemeTokens.semanticColors.info,
            ) {
                FilterBar(
                    label = "Plataforma",
                    options = SupportPlatform.entries.map { FilterOption(it, it.displayName()) },
                    selected = selectedPlatform,
                    onSelected = { selectedPlatform = it ?: SupportPlatform.DESKTOP },
                    allLabel = "Predeterminado",
                    wrap = true,
                )
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stacked = maxWidth < 720.dp
                    if (stacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            TicketTextField(value = appVersion, onValueChange = { appVersion = it }, label = "VersiÃ³n de la app")
                            TicketTextField(value = clientReference, onValueChange = { clientReference = it }, label = "Referencia del cliente")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            TicketTextField(value = appVersion, onValueChange = { appVersion = it }, label = "VersiÃ³n de la app", modifier = Modifier.weight(1f))
                            TicketTextField(value = clientReference, onValueChange = { clientReference = it }, label = "Referencia del cliente", modifier = Modifier.weight(1f))
                        }
                    }
                }
                OutlinedTextField(
                    value = stepsToReproduce,
                    onValueChange = { stepsToReproduce = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pasos para reproducir") },
                    minLines = 4,
                )
                PrimaryButton(
                    text = "Crear ticket",
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
                                priority = selectedPriority,
                            ),
                        )
                    },
                    enabled = client != null && subject.isNotBlank() && description.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun TicketTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

private fun clearTicketFilters(state: TicketsUiState, onEvent: (TicketsUiEvent) -> Unit) {
    if (state.searchQuery.isNotBlank()) onEvent(TicketsUiEvent.SearchChanged(""))
    if (state.statusFilter != null) onEvent(TicketsUiEvent.StatusFilterChanged(null))
    if (state.priorityFilter != null) onEvent(TicketsUiEvent.PriorityFilterChanged(null))
    if (state.categoryFilter != null) onEvent(TicketsUiEvent.CategoryFilterChanged(null))
    if (state.platformFilter != null) onEvent(TicketsUiEvent.PlatformFilterChanged(null))
    if (state.waitingOnFilter != null) onEvent(TicketsUiEvent.WaitingOnFilterChanged(null))
}

private data class TicketListDerived(
    val stats: TicketQueueStats,
    val availableStatuses: List<TicketStatus>,
    val availablePriorities: List<TicketPriority>,
    val availableCategories: List<TicketCategory>,
    val availablePlatforms: List<SupportPlatform>,
    val availableWaitingOn: List<WaitingOn>,
)

private data class QueueMetricSpec(
    val label: String,
    val value: Int,
    val supportingText: String,
    val color: Color,
)

private data class TicketQueueStats(
    val total: Int,
    val open: Int,
    val inProgress: Int,
    val pendingClient: Int,
    val resolved: Int,
    val closed: Int,
    val active: Int,
    val waitingAdmin: Int,
    val waitingClient: Int,
    val urgent: Int,
    val high: Int,
    val loggedMinutes: Int,
) {
    fun countFor(status: TicketStatus): Int = when (status) {
        TicketStatus.OPEN -> open
        TicketStatus.IN_PROGRESS -> inProgress
        TicketStatus.PENDING_CLIENT -> pendingClient
        TicketStatus.RESOLVED -> resolved
        TicketStatus.CLOSED -> closed
    }
}

private fun List<Ticket>.toQueueStats(): TicketQueueStats {
    val activeTickets = filter { it.status.isActiveQueueStatus() }
    return TicketQueueStats(
        total = size,
        open = count { it.status == TicketStatus.OPEN },
        inProgress = count { it.status == TicketStatus.IN_PROGRESS },
        pendingClient = count { it.status == TicketStatus.PENDING_CLIENT },
        resolved = count { it.status == TicketStatus.RESOLVED },
        closed = count { it.status == TicketStatus.CLOSED },
        active = activeTickets.size,
        waitingAdmin = activeTickets.count { it.waitingOn == WaitingOn.ADMIN },
        waitingClient = activeTickets.count { it.waitingOn == WaitingOn.CLIENT },
        urgent = activeTickets.count { it.priority == TicketPriority.URGENT },
        high = activeTickets.count { it.priority == TicketPriority.HIGH },
        loggedMinutes = sumOf { ticket -> ticket.timeEntries.sumOf { it.minutes } },
    )
}

private fun TicketStatus.isActiveQueueStatus(): Boolean = this != TicketStatus.RESOLVED && this != TicketStatus.CLOSED

@Composable
private fun TicketStatus.accentColor(): Color {
    val semantic = SupportDeskThemeTokens.semanticColors
    return when (this) {
        TicketStatus.OPEN -> semantic.info
        TicketStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TicketStatus.PENDING_CLIENT -> semantic.warning
        TicketStatus.RESOLVED -> semantic.success
        TicketStatus.CLOSED -> MaterialTheme.colorScheme.outline
    }
}

@Composable
private fun TicketPriority.accentColor(): Color {
    val semantic = SupportDeskThemeTokens.semanticColors
    return when (this) {
        TicketPriority.LOW -> MaterialTheme.colorScheme.secondary
        TicketPriority.MEDIUM -> semantic.info
        TicketPriority.HIGH -> semantic.warning
        TicketPriority.URGENT -> semantic.danger
    }
}

@Composable
private fun WaitingOn.accentColor(): Color = when (this) {
    WaitingOn.CLIENT -> SupportDeskThemeTokens.semanticColors.warning
    WaitingOn.ADMIN -> MaterialTheme.colorScheme.primary
}
