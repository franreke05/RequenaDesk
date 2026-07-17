package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.requena.supportdesk.app.client.ClientDestination
import com.requena.supportdesk.app.client.ClientNotice
import com.requena.supportdesk.app.client.components.ClientPortalMetric
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.SupportDeskBadge
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDuration
import com.requena.supportdesk.designsystem.theme.formatSupportDeskDateTime
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints

/**
 * Landing page for an existing client. Desktop intentionally fits in one
 * viewport: priority, service state, a small metric strip and today's work.
 * Detail remains in Trabajo rather than competing for attention here.
 */
@Composable
fun ClientHomeScreen(
    clientName: String,
    contactName: String = "",
    allTickets: List<Ticket>,
    openCount: Int,
    monthlyMinutes: Int,
    todayTasks: List<WorkTask>,
    recentTickets: List<Ticket>,
    isLoading: Boolean,
    errorMessage: String?,
    hasServiceSla: Boolean,
    activeProgramCount: Int,
    onNavigate: (ClientDestination) -> Unit,
) {
    val pendingTicket = remember(allTickets) { allTickets.firstOrNull { it.status == TicketStatus.PENDING_CLIENT } }
    val completedToday = remember(todayTasks) { todayTasks.count(WorkTask::completed) }
    val activeTicketCount = remember(allTickets) {
        allTickets.count { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val desktopCockpit = maxWidth >= SupportDeskBreakpoints.clientWide && maxHeight >= 620.dp && errorMessage == null
        val screenModifier = if (desktopCockpit) Modifier.fillMaxSize() else {
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        }
        val padding = if (desktopCockpit) SupportDeskThemeTokens.spacing.lg else SupportDeskThemeTokens.spacing.xl
        Column(
            modifier = screenModifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.md),
        ) {
            ClientPortalPageHeader(
                title = if (contactName.isBlank()) "Hola, $clientName" else "Hola, $contactName",
                subtitle = if (pendingTicket == null) {
                    "Todo el contexto de $clientName, ordenado para decidir el siguiente paso."
                } else {
                    "Hay una actualización que necesita tu respuesta."
                },
                action = {
                    PrimaryButton(
                        text = "Solicitar ayuda",
                        onClick = { onNavigate(ClientDestination.NEW_TICKET) },
                    )
                },
            )

            errorMessage?.let { ClientNotice(message = it, isError = true) }
            if (isLoading && allTickets.isEmpty()) {
                val loadingModifier = if (desktopCockpit) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()
                ClientPortalSurfaceCard(modifier = loadingModifier) {
                    Text("Cargando el estado de tu servicio…", style = MaterialTheme.typography.bodyMedium)
                }
                return@Column
            }

            if (desktopCockpit) {
                ClientHomeDesktop(
                    modifier = Modifier.weight(1f),
                    pendingTicket = pendingTicket,
                    activeTicketCount = activeTicketCount,
                    hasServiceSla = hasServiceSla,
                    activeProgramCount = activeProgramCount,
                    openCount = openCount,
                    completedToday = completedToday,
                    todayTaskCount = todayTasks.size,
                    monthlyMinutes = monthlyMinutes,
                    recentTickets = recentTickets,
                    todayTasks = todayTasks,
                    onNavigate = onNavigate,
                )
            } else {
                ClientHomeCompact(
                    pendingTicket = pendingTicket,
                    activeTicketCount = activeTicketCount,
                    hasServiceSla = hasServiceSla,
                    activeProgramCount = activeProgramCount,
                    openCount = openCount,
                    completedToday = completedToday,
                    todayTaskCount = todayTasks.size,
                    monthlyMinutes = monthlyMinutes,
                    recentTickets = recentTickets,
                    todayTasks = todayTasks,
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun ClientHomeDesktop(
    pendingTicket: Ticket?,
    activeTicketCount: Int,
    hasServiceSla: Boolean,
    activeProgramCount: Int,
    openCount: Int,
    completedToday: Int,
    todayTaskCount: Int,
    monthlyMinutes: Int,
    recentTickets: List<Ticket>,
    todayTasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            ClientPriorityCard(
                pendingTicket = pendingTicket,
                activeTicketCount = activeTicketCount,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1.45f).fillMaxHeight(),
            )
            ClientServiceSnapshotCard(
                hasServiceSla = hasServiceSla,
                activeProgramCount = activeProgramCount,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ClientPortalMetric("Solicitudes abiertas", openCount.toString(), "en seguimiento", Modifier.weight(1f))
            ClientPortalMetric("Tareas de hoy", "$completedToday/$todayTaskCount", "completadas", Modifier.weight(1f))
            ClientPortalMetric("Soporte este mes", formatSupportDeskDuration(monthlyMinutes), "tiempo registrado", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            ClientRecentActivityCard(
                tickets = recentTickets,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1.45f).fillMaxHeight(),
            )
            ClientTodayCard(
                tasks = todayTasks,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ClientHomeCompact(
    pendingTicket: Ticket?,
    activeTicketCount: Int,
    hasServiceSla: Boolean,
    activeProgramCount: Int,
    openCount: Int,
    completedToday: Int,
    todayTaskCount: Int,
    monthlyMinutes: Int,
    recentTickets: List<Ticket>,
    todayTasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ClientPriorityCard(pendingTicket, activeTicketCount, onNavigate, Modifier.fillMaxWidth())
        ClientServiceSnapshotCard(hasServiceSla, activeProgramCount, onNavigate, Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ClientPortalMetric("Abiertas", openCount.toString(), "solicitudes", Modifier.weight(1f))
            ClientPortalMetric("Hoy", "$completedToday/$todayTaskCount", "tareas", Modifier.weight(1f))
        }
        ClientPortalMetric("Soporte este mes", formatSupportDeskDuration(monthlyMinutes), "tiempo registrado", Modifier.fillMaxWidth())
        ClientTodayCard(todayTasks, onNavigate, Modifier.fillMaxWidth())
        ClientRecentActivityCard(recentTickets, onNavigate, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ClientPriorityCard(
    pendingTicket: Ticket?,
    activeTicketCount: Int,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle(
            title = if (pendingTicket == null) "Prioridad" else "Tu respuesta desbloquea el avance",
            supportingText = if (pendingTicket == null) "El equipo tiene el servicio bajo control." else "Revisa este ticket para que el equipo pueda continuar.",
        )
        Text(
            pendingTicket?.subject ?: "No tienes acciones bloqueadas ahora mismo.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (pendingTicket == null) "$activeTicketCount solicitud(es) en seguimiento." else "Estado: pendiente de tu confirmación.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(
            text = if (pendingTicket == null) "Ver trabajo" else "Ver y responder",
            onClick = { onNavigate(if (pendingTicket == null) ClientDestination.WORK else ClientDestination.TICKETS) },
        )
    }
}

@Composable
private fun ClientServiceSnapshotCard(
    hasServiceSla: Boolean,
    activeProgramCount: Int,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SupportDeskThemeTokens.semanticColors
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle("Servicio", "Estado de tu relación con OryKai")
        SupportDeskBadge(
            text = if (hasServiceSla) "Servicio y SLA activo" else "Portal esencial activo",
            containerColor = semantic.successContainer,
            contentColor = semantic.success,
        )
        Text(
            if (activeProgramCount == 0) "Aún no tienes programas extra activos." else "$activeProgramCount programa(s) activo(s) para tu equipo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(text = "Ver programas", onClick = { onNavigate(ClientDestination.PROGRAMS) })
    }
}

@Composable
private fun ClientRecentActivityCard(
    tickets: List<Ticket>,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle("Actividad reciente", "Últimos cambios relevantes")
        if (tickets.isEmpty()) {
            Text("Todavía no hay actualizaciones recientes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tickets.take(2).forEach { ticket ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SupportDeskThemeTokens.spacing.sm)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ticket.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatSupportDeskDateTime(ticket.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TicketStatusBadge(ticket.status)
                }
            }
        }
        SecondaryButton(text = "Ver actividad", onClick = { onNavigate(ClientDestination.ACTIVITY) })
    }
}

@Composable
private fun ClientTodayCard(
    tasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle("Hoy", "Tu lista de trabajo personal")
        if (tasks.isEmpty()) {
            Text("No tienes tareas para hoy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tasks.take(2).forEach { task ->
                Text(
                    text = if (task.completed) "✓ ${task.title}" else task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SecondaryButton(text = "Ir a tareas", onClick = { onNavigate(ClientDestination.TASKS) })
    }
}
