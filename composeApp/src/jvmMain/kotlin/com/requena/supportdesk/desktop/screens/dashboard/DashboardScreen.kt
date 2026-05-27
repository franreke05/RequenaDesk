package com.requena.supportdesk.desktop.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.core.model.TicketPriority
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary?,
    recentTickets: List<Ticket>,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenTickets: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenTicket: (Ticket) -> Unit,
) {
    val spacing = SupportDeskThemeTokens.spacing
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PageHeader(
            title = "Panel",
            subtitle = "Vista operativa compacta de la carga de trabajo actual, sin métricas de relleno ni ruido decorativo.",
            eyebrow = "Resumen de administración",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(text = "Ver tickets", onClick = onOpenTickets)
                    SecondaryButton(text = "Ver clientes", onClick = onOpenClients)
                }
            },
        )

        when {
            isLoading && summary == null -> LoadingState(itemCount = 4)
            errorMessage != null && summary == null -> EmptyState(
                title = "Panel no disponible",
                message = errorMessage,
            )
            summary == null -> EmptyState(
                title = "Sin datos del panel",
                message = "El resumen aparecerá aquí una vez que el estado del panel esté disponible.",
            )
            else -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    maxItemsInEachRow = Int.MAX_VALUE,
                ) {
                    listOf(
                        DashboardMetric("Tickets abiertos", summary.openTickets.toString(), "Trabajo activo que aún requiere atención."),
                        DashboardMetric("Pendiente de cliente", summary.pendingClientTickets.toString(), "Tickets bloqueados esperando respuesta del cliente."),
                        DashboardMetric("Resueltos hoy", summary.resolvedToday.toString(), "Cerrados o corregidos durante el día actual."),
                        DashboardMetric("Foco urgente", recentTickets.count { it.priority == TicketPriority.URGENT }.toString(), "Elementos de alta presión extraídos de la cola."),
                        DashboardMetric("Clientes activos", summary.activeClients.toString(), "Cuentas con actividad de proyecto en curso."),
                    ).forEach { metric ->
                        MetricCard(
                            label = metric.label,
                            value = metric.value,
                            supportingText = metric.supportingText,
                            modifier = Modifier.widthIn(min = 220.dp).weight(1f),
                        )
                    }
                }

                SectionCard(
                    title = "Cola crítica",
                    subtitle = "Los tickets más recientes o urgentes están a un clic desde el panel.",
                ) {
                    if (recentTickets.isEmpty()) {
                        EmptyState(
                            title = "Cola limpia",
                            message = "Ningún ticket reciente requiere atención inmediata ahora mismo.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            recentTickets.forEach { ticket ->
                                TicketListItem(
                                    ticket = ticket,
                                    selected = false,
                                    onClick = { onOpenTicket(ticket) },
                                    showClient = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardMetric(
    val label: String,
    val value: String,
    val supportingText: String,
)
