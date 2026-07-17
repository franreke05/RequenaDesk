package com.requena.supportdesk.app.client.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientDailyTaskLimit
import com.requena.supportdesk.app.client.ClientDailyUrgentLimit
import com.requena.supportdesk.app.client.ClientDestination
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.app.client.initials
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration

// ── HOME ─────────────────────────────────────────────────────────────────────

@Composable
fun ClientHomeScreen(
    clientName: String,
    contactName: String = "",
    allTickets: List<Ticket>,
    openCount: Int,
    monthlyMinutes: Int,
    todayUrgentCount: Int,
    todayTaskCount: Int,
    todayTasksDone: Int,
    recentTickets: List<Ticket>,
    isLoading: Boolean,
    errorMessage: String?,
    hasServiceSla: Boolean,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val ticketCount = allTickets.size
    val animatedTickets by animateIntAsState(ticketCount, tween(700), label = "h_tickets")
    val animatedOpen by animateIntAsState(openCount, tween(850), label = "h_open")
    val urgentRemaining = ClientDailyUrgentLimit - todayUrgentCount
    val taskProgress = todayTasksDone.toFloat() / ClientDailyTaskLimit.toFloat()

    val companyInitials = remember(clientName) { clientName.initials() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = companyInitials,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "Centro de Cliente",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (contactName.isNotBlank()) {
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // ── Badges de identidad empresarial ──────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    SupportDeskBadge(
                        text = "$companyInitials · Empresa",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    SupportDeskBadge(
                        text = "Portal activo",
                        containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                        contentColor = SupportDeskThemeTokens.semanticColors.success,
                    )
                }
                errorMessage?.let {
                    ClientNotice(message = it, isError = true)
                }
            }
        }

        if (isLoading && allTickets.isEmpty()) {
            LoadingState(itemCount = 4)
            return@Column
        }

        ClientContinuityPanel(
            tickets = allTickets,
            hasServiceSla = hasServiceSla,
            onNavigate = onNavigate,
        )

        ClientStatusBoard(tickets = allTickets)

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MetricCard(
                    label = "Total tickets",
                    value = animatedTickets.toString(),
                    supportingText = "desde el inicio",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Este mes",
                    value = formatSupportDeskDuration(monthlyMinutes),
                    supportingText = "horas de soporte",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = "Abiertos",
                    value = animatedOpen.toString(),
                    supportingText = "en curso ahora",
                    modifier = Modifier.weight(1f),
                )
            }
            SupportDeskBadge(
                text = if (urgentRemaining <= 0) "Sin urgentes hoy" else "$urgentRemaining urgente${if (urgentRemaining == 1) "" else "s"} disponible${if (urgentRemaining == 1) "" else "s"}",
                containerColor = if (urgentRemaining <= 0) SupportDeskThemeTokens.semanticColors.dangerContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (urgentRemaining <= 0) SupportDeskThemeTokens.semanticColors.danger else MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(
                text = "Acciones rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                QuickActionCard(
                    title = "Nuevo ticket",
                    subtitle = "Describe tu problema al equipo",
                    badge = if (urgentRemaining > 0) "$urgentRemaining urgentes disponibles" else "Sin urgentes hoy",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(ClientDestination.NEW_TICKET) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    title = "Tablero",
                    subtitle = "Vista kanban de todos tus tickets",
                    badge = "$openCount activos",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { onNavigate(ClientDestination.BOARD) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                QuickActionCard(
                    title = "Mis tickets",
                    subtitle = "Historial y detalle de solicitudes",
                    badge = "$openCount abiertos",
                    accentColor = SupportDeskThemeTokens.semanticColors.info,
                    onClick = { onNavigate(ClientDestination.TICKETS) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionCard(
                    title = "Mis tareas",
                    subtitle = "Notas pendientes del día",
                    badge = "$todayTaskCount/$ClientDailyTaskLimit hoy",
                    accentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.80f),
                    onClick = { onNavigate(ClientDestination.TASKS) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionCard(title = "Actividad reciente", subtitle = "Últimas actualizaciones") {
            if (recentTickets.isEmpty()) {
                Text(
                    text = "Sin actividad reciente. Abre tu primer ticket para empezar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    recentTickets.forEach { ticket ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                            ) {
                                Text(
                                    text = ticket.subject,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatSupportDeskDateTime(ticket.updatedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TicketStatusBadge(ticket.status)
                        }
                    }
                }
            }
        }

        SectionCard(title = "Tareas de hoy", subtitle = "$todayTasksDone/$ClientDailyTaskLimit completadas") {
            LinearProgressIndicator(
                progress = { taskProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$todayTasksDone de $ClientDailyTaskLimit tareas completadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedVisibility(visible = todayTasksDone < ClientDailyTaskLimit) {
                    SecondaryButton(text = "Ver tareas", onClick = { onNavigate(ClientDestination.TASKS) })
                }
            }
        }

    }
}

@Composable
private fun ClientContinuityPanel(
    tickets: List<Ticket>,
    hasServiceSla: Boolean,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val pendingClientTicket = remember(tickets) {
        tickets.firstOrNull { it.status == TicketStatus.PENDING_CLIENT }
    }
    val activeTicketCount = remember(tickets) {
        tickets.count { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }

    SectionCard(
        title = if (pendingClientTicket == null) "Estado de tu servicio" else "Tu siguiente acción",
        subtitle = if (pendingClientTicket == null) {
            if (activeTicketCount == 0) "No hay solicitudes activas en este momento." else "El equipo está trabajando en $activeTicketCount solicitud${if (activeTicketCount == 1) "" else "es"}."
        } else {
            "Necesitamos tu confirmación para avanzar."
        },
    ) {
        if (pendingClientTicket != null) {
            Text(
                pendingClientTicket.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SecondaryButton(text = "Ver y responder", onClick = { onNavigate(ClientDestination.TICKETS) })
        } else {
            SupportDeskBadge(
                text = "Servicio en seguimiento",
                containerColor = SupportDeskThemeTokens.semanticColors.successContainer,
                contentColor = SupportDeskThemeTokens.semanticColors.success,
            )
        }
    }

    SectionCard(
        title = if (hasServiceSla) "Servicio y SLA activo" else "Más control sobre tu servicio",
        subtitle = if (hasServiceSla) {
            "Consulta consumo, actividad y el resumen de soporte."
        } else {
            "Activa el módulo Servicio y SLA para consultar el seguimiento de soporte."
        },
    ) {
        SecondaryButton(
            text = if (hasServiceSla) "Ver mi servicio" else "Conocer Servicio y SLA",
            onClick = { onNavigate(ClientDestination.SERVICE) },
        )
        if (!hasServiceSla) {
            Text(
                "No es una limitación de tus solicitudes actuales; es un componente opcional de tu portal.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (hovered) 10.dp else 2.dp,
        animationSpec = tween(200),
        label = "card_elev",
    )
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 130.dp)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = accentColor.copy(alpha = 0.08f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    badge?.let {
                        SupportDeskBadge(
                            text = it,
                            containerColor = accentColor.copy(alpha = 0.18f),
                            contentColor = accentColor,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .background(accentColor.copy(alpha = if (hovered) 0.80f else 0.32f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private val StatusBoardConfig = listOf(
    TicketStatus.OPEN to "Abiertos",
    TicketStatus.IN_PROGRESS to "En proceso",
    TicketStatus.PENDING_CLIENT to "Pendiente",
    TicketStatus.RESOLVED to "Resueltos",
    TicketStatus.CLOSED to "Cerrados",
)

@Composable
private fun ClientStatusBoard(tickets: List<Ticket>) {
    val spacing = SupportDeskThemeTokens.spacing
    val semantic = SupportDeskThemeTokens.semanticColors
    val statusColors = listOf(
        semantic.info,
        MaterialTheme.colorScheme.primary,
        semantic.warning,
        semantic.success,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val countByStatus = remember(tickets) { tickets.groupingBy { it.status }.eachCount() }
    val activeCount = (countByStatus[TicketStatus.OPEN] ?: 0) + (countByStatus[TicketStatus.IN_PROGRESS] ?: 0)
    SectionCard(
        title = "Estado del soporte",
        subtitle = "${tickets.size} tickets · $activeCount activos",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            StatusBoardConfig.forEachIndexed { index, (status, label) ->
                StatusPillCard(
                    label = label,
                    count = countByStatus[status] ?: 0,
                    accentColor = statusColors[index],
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatusPillCard(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val animatedCount by animateIntAsState(count, tween(700), label = "status_count")
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor.copy(alpha = 0.72f), RoundedCornerShape(2.dp)),
            )
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
