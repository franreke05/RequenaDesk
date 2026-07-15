package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.SupportPlatformBadge
import com.requena.supportdesk.designsystem.components.badges.TicketCategoryBadge
import com.requena.supportdesk.designsystem.components.badges.TicketPriorityBadge
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.ErrorState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState

// ── TABLERO (BOARD) ───────────────────────────────────────────────────────────

private val KanbanColumns = listOf(
    TicketStatus.OPEN to "Abiertos",
    TicketStatus.IN_PROGRESS to "En proceso",
    TicketStatus.PENDING_CLIENT to "Pendiente",
    TicketStatus.RESOLVED to "Resueltos",
    TicketStatus.CLOSED to "Cerrados",
)

@Composable
fun ClientBoardScreen(
    tickets: List<Ticket>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onTicketClick: (String) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val columnColors = listOf(
        semantic.info,
        MaterialTheme.colorScheme.primary,
        semantic.warning,
        semantic.success,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Tablero", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${tickets.size} tickets · Vista kanban",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            isLoading && tickets.isEmpty() -> LoadingState(modifier = Modifier.fillMaxSize())
            errorMessage != null && tickets.isEmpty() -> ErrorState(
                message = errorMessage,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            tickets.isEmpty() -> EmptyState(
                title = "Sin tickets",
                message = "Crea el primer ticket para empezar a usar el tablero.",
            )
            else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= SupportDeskBreakpoints.clientBoardWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    KanbanColumns.forEachIndexed { idx, (status, label) ->
                        val colTickets = remember(tickets, status) { tickets.filter { it.status == status } }
                        KanbanColumn(
                            label = label,
                            accentColor = columnColors[idx],
                            tickets = colTickets,
                            onTicketClick = onTicketClick,
                            scrollable = true,
                            modifier = Modifier.width(240.dp).fillMaxHeight(),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    KanbanColumns.forEachIndexed { idx, (status, label) ->
                        val colTickets = tickets.filter { it.status == status }
                        if (colTickets.isNotEmpty()) {
                            item(key = "col_${status.name}") {
                                KanbanColumn(
                                    label = label,
                                    accentColor = columnColors[idx],
                                    tickets = colTickets,
                                    onTicketClick = onTicketClick,
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

@Composable
private fun KanbanColumn(
    label: String,
    accentColor: Color,
    tickets: List<Ticket>,
    onTicketClick: (String) -> Unit,
    scrollable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val hasUrgent = tickets.any { it.priority == TicketPriority.URGENT }
    val animatedCount by animateIntAsState(targetValue = tickets.size, animationSpec = tween(300), label = "kanbanCount")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.06f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = if (scrollable) Modifier.fillMaxSize().padding(spacing.sm) else Modifier.padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    if (hasUrgent) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SupportDeskThemeTokens.semanticColors.danger, CircleShape),
                        )
                    }
                }
                SupportDeskBadge(
                    text = animatedCount.toString(),
                    containerColor = accentColor.copy(alpha = 0.18f),
                    contentColor = accentColor,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accentColor.copy(alpha = 0.42f), RoundedCornerShape(1.dp)),
            )
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin tickets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
                    )
                }
            } else if (scrollable) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    itemsIndexed(tickets, key = { _, t -> t.id }) { _, ticket ->
                        KanbanTicketCard(
                            ticket = ticket,
                            onClick = { onTicketClick(ticket.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    tickets.forEach { ticket ->
                        KanbanTicketCard(
                            ticket = ticket,
                            onClick = { onTicketClick(ticket.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanTicketCard(ticket: Ticket, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (hovered) 8.dp else 1.dp,
        animationSpec = tween(180),
        label = "kanban_elev",
    )
    val priorityAccent = when (ticket.priority) {
        TicketPriority.LOW -> MaterialTheme.colorScheme.secondary
        TicketPriority.MEDIUM -> semantic.info
        TicketPriority.HIGH -> semantic.warning
        TicketPriority.URGENT -> semantic.danger
    }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(priorityAccent),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = ticket.ticketNumber.ifBlank { "#${ticket.id.take(6)}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TicketPriorityBadge(ticket.priority)
            }
            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TicketCategoryBadge(ticket.category)
                SupportPlatformBadge(ticket.platform)
            }
            ticket.assignee?.let { assignee ->
                Text(
                    text = assignee.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            }
        }
    }
}
