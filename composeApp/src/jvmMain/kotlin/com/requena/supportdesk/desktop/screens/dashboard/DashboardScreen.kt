package com.requena.supportdesk.desktop.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.requena.supportdesk.core.model.DashboardSummary
import com.requena.supportdesk.core.model.Ticket
import com.requena.supportdesk.designsystem.components.buttons.PrimaryButton
import com.requena.supportdesk.designsystem.components.buttons.SecondaryButton
import com.requena.supportdesk.designsystem.components.cards.MetricCard
import com.requena.supportdesk.designsystem.components.cards.SectionCard
import com.requena.supportdesk.designsystem.components.feedback.EmptyState
import com.requena.supportdesk.designsystem.components.feedback.LoadingState
import com.requena.supportdesk.designsystem.components.layout.PageHeader
import com.requena.supportdesk.designsystem.components.tickets.TicketListItem
import com.requena.supportdesk.designsystem.theme.SupportDeskThemeTokens

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
            title = "Dashboard",
            subtitle = "A compact operational view of the current workload, without filler metrics or decorative noise.",
            eyebrow = "Admin overview",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(text = "Open tickets", onClick = onOpenTickets)
                    SecondaryButton(text = "Open clients", onClick = onOpenClients)
                }
            },
        )

        when {
            isLoading && summary == null -> LoadingState(itemCount = 4)
            errorMessage != null && summary == null -> EmptyState(
                title = "Dashboard unavailable",
                message = errorMessage,
            )
            summary == null -> EmptyState(
                title = "No dashboard data",
                message = "The summary will appear here once the shared dashboard state is available.",
            )
            else -> {
                listOf(
                    DashboardMetric("Open tickets", summary.openTickets.toString(), "Active work that still needs attention."),
                    DashboardMetric("Pending client", summary.pendingClientTickets.toString(), "Tickets blocked waiting on client input."),
                    DashboardMetric("Resolved today", summary.resolvedToday.toString(), "Closed or fixed during the current day."),
                    DashboardMetric("Urgent focus", recentTickets.count { it.priority.name == "URGENT" }.toString(), "High pressure items surfaced from the queue."),
                    DashboardMetric("Active clients", summary.activeClients.toString(), "Accounts with current project activity."),
                ).chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        rowItems.forEach { metric ->
                            MetricCard(
                                label = metric.label,
                                value = metric.value,
                                supportingText = metric.supportingText,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                SectionCard(
                    title = "Critical queue",
                    subtitle = "The most recent or urgent tickets are one click away from the dashboard.",
                ) {
                    if (recentTickets.isEmpty()) {
                        EmptyState(
                            title = "Queue is clear",
                            message = "No recent tickets need immediate attention right now.",
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
