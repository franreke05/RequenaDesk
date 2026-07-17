package com.requena.supportdesk.app.client.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.requena.supportdesk.app.client.ClientDestination
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketStatus
import com.requena.supportdesk.core.model.WorkTask
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@Composable
fun ClientWorkScreen(
    tickets: List<Ticket>,
    tasks: List<WorkTask>,
    onNavigate: (ClientDestination) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    val openTickets = remember(tickets) {
        tickets.count { it.status != TicketStatus.CLOSED && it.status != TicketStatus.RESOLVED }
    }
    val pendingClientTickets = remember(tickets) { tickets.count { it.status == TicketStatus.PENDING_CLIENT } }
    val openTasks = remember(tasks) { tasks.count { !it.completed } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text("Trabajo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Solicitudes, tareas y avances de tu equipo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            MetricCard(
                label = "Solicitudes abiertas",
                value = openTickets.toString(),
                supportingText = "en curso",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Tu respuesta",
                value = pendingClientTickets.toString(),
                supportingText = "pendientes",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Tareas",
                value = openTasks.toString(),
                supportingText = "por completar",
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(
            title = "Solicitudes",
            subtitle = if (pendingClientTickets > 0) "Hay una respuesta que necesita tu atención." else "Todo el historial de soporte en un solo lugar.",
        ) {
            SecondaryButton(text = "Ver solicitudes", onClick = { onNavigate(ClientDestination.TICKETS) })
            SecondaryButton(text = "Abrir una solicitud", onClick = { onNavigate(ClientDestination.NEW_TICKET) })
        }

        SectionCard(
            title = "Plan de trabajo",
            subtitle = "Consulta qué tareas están activas y qué ha cambiado.",
        ) {
            SecondaryButton(text = "Ver tareas", onClick = { onNavigate(ClientDestination.TASKS) })
            SecondaryButton(text = "Ver tablero", onClick = { onNavigate(ClientDestination.BOARD) })
        }

        SectionCard(
            title = "Actividad",
            subtitle = "Revisa el historial de actualizaciones del servicio.",
        ) {
            SecondaryButton(text = "Ver actividad", onClick = { onNavigate(ClientDestination.ACTIVITY) })
        }
    }
}
