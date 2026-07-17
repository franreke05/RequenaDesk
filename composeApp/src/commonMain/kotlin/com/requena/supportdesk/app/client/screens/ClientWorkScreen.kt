package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.requena.supportdesk.app.client.ClientDestination
import com.requena.supportdesk.app.client.components.ClientPortalMetric
import com.requena.supportdesk.app.client.components.ClientPortalPageHeader
import com.requena.supportdesk.app.client.components.ClientPortalSectionTitle
import com.requena.supportdesk.app.client.components.ClientPortalSurfaceCard
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.badges.TicketStatusBadge
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens
import com.requena.supportdesk.designsystem.tokens.SupportDeskBreakpoints

/** A concise work hub: the customer chooses a queue, then sees the relevant detail. */
@Composable
fun ClientWorkScreen(
    tickets: List<Ticket>,
    tasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val openTickets = remember(tickets) {
        tickets.filter { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }
    val pendingClientTickets = remember(tickets) { tickets.count { it.status == TicketStatus.PENDING_CLIENT } }
    val openTasks = remember(tasks) { tasks.filterNot(WorkTask::completed) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ClientPortalPageHeader(
            title = "Trabajo",
            subtitle = "Todo lo que tu equipo necesita revisar o desbloquear.",
            action = {
                PrimaryButton(text = "Solicitar ayuda", onClick = { onNavigate(ClientDestination.NEW_TICKET) })
            },
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= SupportDeskBreakpoints.clientWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    RequestsOverview(
                        tickets = openTickets,
                        pendingClientTickets = pendingClientTickets,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1.15f),
                    )
                    TasksOverview(
                        tasks = openTasks,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    RequestsOverview(openTickets, pendingClientTickets, onNavigate, Modifier.fillMaxWidth())
                    TasksOverview(openTasks, onNavigate, Modifier.fillMaxWidth())
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ClientPortalMetric(
                label = "Solicitudes abiertas",
                value = openTickets.size.toString(),
                supportingText = if (pendingClientTickets == 0) "en seguimiento" else "$pendingClientTickets esperan tu respuesta",
                modifier = Modifier.weight(1f),
            )
            ClientPortalMetric(
                label = "Tareas pendientes",
                value = openTasks.size.toString(),
                supportingText = "para tu equipo",
                modifier = Modifier.weight(1f),
            )
        }

        ClientPortalSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ClientPortalSectionTitle("Consulta el contexto", "Las vistas detalladas se mantienen separadas para trabajar sin ruido.")
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SecondaryButton(text = "Tablero", onClick = { onNavigate(ClientDestination.BOARD) })
                SecondaryButton(text = "Actividad", onClick = { onNavigate(ClientDestination.ACTIVITY) })
            }
        }
    }
}

@Composable
private fun RequestsOverview(
    tickets: List<Ticket>,
    pendingClientTickets: Int,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SupportDeskThemeTokens.spacing
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle(
            title = if (pendingClientTickets > 0) "Hay una respuesta pendiente" else "Solicitudes",
            supportingText = if (pendingClientTickets > 0) {
                "Tu confirmación permite al equipo continuar."
            } else {
                "El soporte y sus actualizaciones, en un solo lugar."
            },
        )
        if (tickets.isEmpty()) {
            Text("No hay solicitudes abiertas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tickets.take(2).forEach { ticket ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ticket.subject,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TicketStatusBadge(ticket.status)
                }
            }
        }
        SecondaryButton(text = "Ver solicitudes", onClick = { onNavigate(ClientDestination.TICKETS) })
    }
}

@Composable
private fun TasksOverview(
    tasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ClientPortalSurfaceCard(modifier) {
        ClientPortalSectionTitle("Plan de trabajo", "Tareas activas compartidas con tu equipo.")
        if (tasks.isEmpty()) {
            Text("No hay tareas pendientes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tasks.take(2).forEach { task ->
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SecondaryButton(text = "Ver tareas", onClick = { onNavigate(ClientDestination.TASKS) })
    }
}
